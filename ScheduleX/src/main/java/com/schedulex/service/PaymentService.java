package com.schedulex.service;

import com.schedulex.model.Order;
import com.schedulex.model.PaymentStatus;
import com.schedulex.model.PaymentTransaction;
import com.schedulex.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentTransactionRepository transactionRepository;
    private final Random random = new Random();
    
    public PaymentService(PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Simulate payment processing
     * In production: integrate with Razorpay/Stripe API
     */
    public PaymentTransaction processPayment(Order order, int attemptNumber) {
        log.info("💳 Processing payment for order: {}, attempt: {}", 
                 order.getOrderId(), attemptNumber);
        
        // Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
            order.getOrderId(),
            order.getAmount(),
            attemptNumber
        );
        transaction.setStatus(PaymentStatus.PROCESSING);
        transactionRepository.save(transaction);
        
        try {
            // Simulate API call delay
            Thread.sleep(2000);
            
            // SIMULATE PAYMENT OUTCOME
            // 70% success rate (in real world, depends on actual payment)
            boolean paymentSuccess = simulatePaymentGateway(attemptNumber);
            
            if (paymentSuccess) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setPaymentId("PAY_" + System.currentTimeMillis());
                transaction.setPaymentMethod("card");
                transaction.setGatewayResponse("{\"status\":\"captured\",\"method\":\"card\"}");
                
                log.info("✅ Payment successful: {} - ₹{}", 
                         order.getOrderId(), order.getAmount());
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                transaction.setErrorMessage(getRandomFailureReason());
                transaction.setGatewayResponse("{\"status\":\"failed\",\"error\":\"" + 
                                              transaction.getErrorMessage() + "\"}");
                
                log.error("❌ Payment failed: {} - Reason: {}", 
                          order.getOrderId(), transaction.getErrorMessage());
            }
            
        } catch (Exception e) {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setErrorMessage("System error: " + e.getMessage());
            log.error("❌ Payment processing error: {}", e.getMessage());
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Simulate payment gateway response
     * Retry attempts have higher success rate
     */
    private boolean simulatePaymentGateway(int attemptNumber) {
        // TESTING MODE: First attempt always fails
        if (attemptNumber == 1) {
            return false; // Force failure on first attempt
        }
        
        // Retry attempts have higher success
        // Second attempt: 70% success
        // Third attempt: 90% success  
        int successRate = attemptNumber == 2 ? 70 : 90;
        return random.nextInt(100) < successRate;
    }
    /**
     * Get realistic payment failure reasons
     */
    private String getRandomFailureReason() {
        String[] reasons = {
            "Insufficient funds in account",
            "Card expired",
            "Transaction declined by bank",
            "Invalid CVV",
            "Card limit exceeded",
            "Payment gateway timeout",
            "3D Secure authentication failed",
            "Card blocked by bank"
        };
        return reasons[random.nextInt(reasons.length)];
    }
    
    /**
     * Calculate next retry time based on attempt number
     */
    public LocalDateTime calculateNextRetryTime(int attemptNumber) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (attemptNumber) {
            case 1:
                return now.plusHours(6);   // Retry after 6 hours
            case 2:
                return now.plusHours(24);  // Retry after 24 hours (next day)
            case 3:
                return now.plusHours(72);  // Retry after 72 hours (3 days)
            default:
                return now.plusHours(6);
        }
    }
}