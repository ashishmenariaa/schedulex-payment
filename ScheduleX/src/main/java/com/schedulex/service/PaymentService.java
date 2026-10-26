package com.schedulex.service;

import com.schedulex.gateway.PaymentGateway;
import com.schedulex.gateway.PaymentGateway.GatewayResult;
import com.schedulex.model.Order;
import com.schedulex.model.PaymentStatus;
import com.schedulex.model.PaymentTransaction;
import com.schedulex.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** Wait before retry N (index 0 = after the 1st failure): 6h, 24h, 72h. */
    static final List<Duration> RETRY_BACKOFF = List.of(
        Duration.ofHours(6), Duration.ofHours(24), Duration.ofHours(72)
    );

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentTransactionRepository transactionRepository, PaymentGateway paymentGateway) {
        this.transactionRepository = transactionRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Charge the order once and record the attempt.
     * The (orderId, attemptNumber) unique constraint means the same attempt can never be recorded twice.
     */
    public PaymentTransaction processPayment(Order order, int attemptNumber) {
        log.info("💳 Processing payment for order: {}, attempt: {}",
                 order.getOrderId(), attemptNumber);

        PaymentTransaction transaction = new PaymentTransaction(
            order.getOrderId(),
            order.getAmount(),
            attemptNumber
        );
        transaction.setStatus(PaymentStatus.PROCESSING);
        transactionRepository.saveAndFlush(transaction);

        try {
            GatewayResult result = paymentGateway.charge(order.getOrderId(), order.getAmount(), attemptNumber);
            transaction.setGatewayResponse(result.rawResponse());

            if (result.success()) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setPaymentId(result.paymentId());
                transaction.setPaymentMethod("card");
                log.info("✅ Payment successful: {} - ₹{}", order.getOrderId(), order.getAmount());
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setErrorMessage(result.errorMessage());
                log.error("❌ Payment failed: {} - Reason: {}", order.getOrderId(), result.errorMessage());
            }
        } catch (RuntimeException e) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setErrorMessage("Gateway error: " + e.getMessage());
            log.error("❌ Payment processing error: {}", e.getMessage());
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Next retry time after the given number of failed attempts (exponential-style backoff).
     */
    public LocalDateTime calculateNextRetryTime(int failedAttempts) {
        int index = Math.min(Math.max(failedAttempts, 1), RETRY_BACKOFF.size()) - 1;
        return LocalDateTime.now().plus(RETRY_BACKOFF.get(index));
    }
}
