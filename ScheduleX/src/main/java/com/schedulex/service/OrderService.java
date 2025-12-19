package com.schedulex.service;

import com.schedulex.model.*;
import com.schedulex.repository.OrderRepository;
import com.schedulex.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
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
    public Order createOrder(Order order) {
        log.info("🛒 Creating order: {}", order.getOrderId());
        
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        
        // Attempt first payment
        attemptPayment(savedOrder);
        
        return savedOrder;
    }
    
    /**
     * Attempt payment for an order
     */
    @Transactional
    public void attemptPayment(Order order) {
        int attemptNumber = order.getRetryCount() + 1;
        
        log.info("💰 Attempting payment: Order={}, Attempt={}/{}", 
                 order.getOrderId(), attemptNumber, order.getMaxRetries());
        
        order.setPaymentStatus(PaymentStatus.PROCESSING);
        order.setLastRetryTime(LocalDateTime.now());
        orderRepository.save(order);
        
        // Process payment
        PaymentTransaction transaction = paymentService.processPayment(order, attemptNumber);
        
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            handlePaymentSuccess(order, transaction);
        } else {
            handlePaymentFailure(order, transaction);
        }
    }
    
    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(Order order, PaymentTransaction transaction) {
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentId(transaction.getPaymentId());
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        orderRepository.save(order);
        
        log.info("🎉 Order paid successfully: {} - ₹{}", 
                 order.getOrderId(), order.getAmount());
        
        // TODO: Send success email/SMS
        // TODO: Notify warehouse to ship
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
            // Schedule retry job
            LocalDateTime nextRetryTime = paymentService.calculateNextRetryTime(
                order.getRetryCount()
            );
            order.setNextRetryTime(nextRetryTime);
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            
            orderRepository.save(order);
            
            // Create retry job in job scheduler
            schedulePaymentRetryJob(order, nextRetryTime);
            
            log.info("🔄 Payment retry scheduled: Order={}, NextRetry={}, Attempt={}/{}", 
                     order.getOrderId(), nextRetryTime, 
                     order.getRetryCount(), order.getMaxRetries());
            
        } else {
            // Max retries exceeded - cancel order
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            orderRepository.save(order);
            
            log.error("💀 Order cancelled after max retries: {}", order.getOrderId());
            
            // TODO: Send cancellation email
        }
    }
    
    /**
     * Schedule payment retry job in ScheduleX engine
     */
    private void schedulePaymentRetryJob(Order order, LocalDateTime retryTime) {
        Job retryJob = new Job();
        retryJob.setJobName("Payment Retry - " + order.getOrderId());
        retryJob.setJobType(JobType.ONE_TIME);
        retryJob.setScheduledTime(retryTime);
        retryJob.setJobData(order.getOrderId()); // Store order ID
        retryJob.setPriority(8); // High priority for payments
        retryJob.setMaxRetries(1); // Don't retry the retry job itself
        
        jobService.createJob(retryJob);
    }
    
    /**
     * Get orders that need retry (called by scheduler)
     */
    public List<Order> getOrdersNeedingRetry() {
        return orderRepository.findByPaymentStatusAndNextRetryTimeBefore(
            PaymentStatus.FAILED, 
            LocalDateTime.now()
        );
    }
    
    /**
     * Get order by ID
     */
    public Order getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
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
        return transactionRepository.findByOrderIdOrderByAttemptedAtDesc(orderId);
    }
    
    /**
     * Get statistics
     */
    public OrderStats getStats() {
        long totalOrders = orderRepository.count();
        long pendingPayments = orderRepository.countByPaymentStatus(PaymentStatus.PENDING);
        long failedPayments = orderRepository.countByPaymentStatus(PaymentStatus.FAILED);
        long successfulPayments = orderRepository.countByPaymentStatus(PaymentStatus.SUCCESS);
        long cancelledOrders = orderRepository.countByPaymentStatus(PaymentStatus.CANCELLED);
        
        return new OrderStats(
            totalOrders,
            pendingPayments,
            failedPayments,
            successfulPayments,
            cancelledOrders
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