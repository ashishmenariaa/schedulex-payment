package com.schedulex.service;

import com.schedulex.dto.CreateOrderRequest;
import com.schedulex.exception.PaymentConflictException;
import com.schedulex.exception.ResourceNotFoundException;
import com.schedulex.model.*;
import com.schedulex.repository.OrderRepository;
import com.schedulex.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public static final String PAYMENT_RETRY_JOB_PREFIX = "Payment Retry - ";

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final JobService jobService;

    public OrderService(OrderRepository orderRepository,
                       PaymentTransactionRepository transactionRepository,
                       PaymentService paymentService,
                       JobService jobService) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.paymentService = paymentService;
        this.jobService = jobService;
    }

    /**
     * Create new order and attempt first payment
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderId = (request.orderId() == null || request.orderId().isBlank())
            ? "ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
            : request.orderId();

        if (orderRepository.findByOrderId(orderId).isPresent()) {
            throw new PaymentConflictException("Order already exists: " + orderId);
        }

        log.info("🛒 Creating order: {}", orderId);

        Order order = new Order(orderId, request.customerId(), request.customerName(),
                                request.customerEmail(), request.customerPhone(),
                                request.amount(), request.orderItems());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Attempt first payment
        return charge(savedOrder);
    }

    /**
     * Manually retry payment for an order. Rejected if the order is paid, in progress or cancelled.
     */
    @Transactional
    public Order retryPayment(String orderId) {
        return charge(getOrder(orderId));
    }

    /**
     * Called by the scheduler when a retry job fires.
     * Skips (returns false) if the order was already paid or a newer retry has been scheduled since this job was created.
     */
    @Transactional
    public boolean retryIfDue(String orderId) {
        Order order = getOrder(orderId);

        if (!order.isPayable()) {
            log.info("⏭️ Skipping retry for order {}: status is {}", orderId, order.getPaymentStatus());
            return false;
        }
        if (order.getNextRetryTime() != null && order.getNextRetryTime().isAfter(LocalDateTime.now())) {
            log.info("⏭️ Skipping stale retry job for order {}: next retry is at {}", orderId, order.getNextRetryTime());
            return false;
        }

        charge(order);
        return true;
    }

    /**
     * Charge the order once. The PROCESSING status is flushed with an optimistic-lock version check
     * before the gateway is called, so a concurrent attempt on the same order fails instead of charging twice.
     */
    private Order charge(Order order) {
        if (!order.isPayable()) {
            throw new PaymentConflictException(
                "Order " + order.getOrderId() + " cannot be charged in status " + order.getPaymentStatus());
        }

        int attemptNumber = order.getRetryCount() + 1;

        log.info("💰 Attempting payment: Order={}, Attempt={}/{}",
                 order.getOrderId(), attemptNumber, order.getMaxRetries());

        order.setPaymentStatus(PaymentStatus.PROCESSING);
        order.setLastRetryTime(LocalDateTime.now());
        order = orderRepository.saveAndFlush(order);

        PaymentTransaction transaction = paymentService.processPayment(order, attemptNumber);

        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            handlePaymentSuccess(order, transaction);
        } else {
            handlePaymentFailure(order, transaction);
        }
        return order;
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(Order order, PaymentTransaction transaction) {
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentId(transaction.getPaymentId());
        order.setPaidAt(LocalDateTime.now());
        order.setNextRetryTime(null);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);

        log.info("🎉 Order paid successfully: {} - ₹{}",
                 order.getOrderId(), order.getAmount());
    }

    /**
     * Handle payment failure and schedule retry
     */
    private void handlePaymentFailure(Order order, PaymentTransaction transaction) {
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setFailureReason(transaction.getErrorMessage());
        order.setRetryCount(order.getRetryCount() + 1);
        order.setUpdatedAt(LocalDateTime.now());

        if (order.canRetry()) {
            LocalDateTime nextRetryTime = paymentService.calculateNextRetryTime(order.getRetryCount());
            order.setNextRetryTime(nextRetryTime);
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);

            orderRepository.save(order);

            schedulePaymentRetryJob(order, nextRetryTime);

            log.info("🔄 Payment retry scheduled: Order={}, NextRetry={}, Attempt={}/{}",
                     order.getOrderId(), nextRetryTime,
                     order.getRetryCount(), order.getMaxRetries());

        } else {
            // Max retries exceeded - cancel order
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            order.setNextRetryTime(null);
            orderRepository.save(order);

            log.error("💀 Order cancelled after max retries: {}", order.getOrderId());
        }
    }

    /**
     * Schedule payment retry job in ScheduleX engine
     */
    private void schedulePaymentRetryJob(Order order, LocalDateTime retryTime) {
        Job retryJob = new Job();
        retryJob.setJobName(PAYMENT_RETRY_JOB_PREFIX + order.getOrderId());
        retryJob.setJobType(JobType.ONE_TIME);
        retryJob.setScheduledTime(retryTime);
        retryJob.setJobData(order.getOrderId()); // Store order ID
        retryJob.setPriority(8); // High priority for payments
        retryJob.setMaxRetries(1); // Don't retry the retry job itself

        jobService.createJob(retryJob);
    }

    /**
     * Get order by ID
     */
    public Order getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Get payment transactions for order
     */
    public List<PaymentTransaction> getOrderTransactions(String orderId) {
        getOrder(orderId); // 404 for unknown orders instead of an empty list
        return transactionRepository.findByOrderIdOrderByAttemptedAtDesc(orderId);
    }

    /**
     * Get statistics
     */
    public OrderStats getStats() {
        return new OrderStats(
            orderRepository.count(),
            orderRepository.countByPaymentStatus(PaymentStatus.PENDING),
            orderRepository.countByPaymentStatus(PaymentStatus.FAILED),
            orderRepository.countByPaymentStatus(PaymentStatus.SUCCESS),
            orderRepository.countByPaymentStatus(PaymentStatus.CANCELLED)
        );
    }

    public record OrderStats(
        long totalOrders,
        long pendingPayments,
        long failedPayments,
        long successfulPayments,
        long cancelledOrders
    ) {}
}
