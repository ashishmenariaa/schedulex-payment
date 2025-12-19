package com.schedulex.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderId; // ORD_123456
    
    private String customerId;
    
    private String customerName;
    
    private String customerEmail;
    
    private String customerPhone;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(columnDefinition = "TEXT")
    private String orderItems; // JSON string with product details
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    
    private String paymentId; // From Razorpay
    
    private String paymentGatewayOrderId;
    
    private Integer retryCount = 0;
    
    private Integer maxRetries = 3;
    
    private LocalDateTime nextRetryTime;
    
    private LocalDateTime lastRetryTime;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    private LocalDateTime paidAt;
    
    // Constructors
    public Order() {}
    
    public Order(String orderId, String customerId, String customerName, 
                 String customerEmail, String customerPhone, BigDecimal amount, 
                 String orderItems) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.amount = amount;
        this.orderItems = orderItems;
        this.paymentStatus = PaymentStatus.PENDING;
        this.orderStatus = OrderStatus.CREATED;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getOrderItems() { return orderItems; }
    public void setOrderItems(String orderItems) { this.orderItems = orderItems; }
    
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getPaymentGatewayOrderId() { return paymentGatewayOrderId; }
    public void setPaymentGatewayOrderId(String paymentGatewayOrderId) { 
        this.paymentGatewayOrderId = paymentGatewayOrderId; 
    }
    
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    
    public LocalDateTime getLastRetryTime() { return lastRetryTime; }
    public void setLastRetryTime(LocalDateTime lastRetryTime) { this.lastRetryTime = lastRetryTime; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    
    // Helper method
    public boolean canRetry() {
        return retryCount < maxRetries && 
               paymentStatus == PaymentStatus.FAILED;
    }
}