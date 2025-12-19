package com.schedulex.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderId;
    
    private String paymentId;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String paymentMethod; // card, upi, netbanking
    
    @Column(columnDefinition = "TEXT")
    private String gatewayResponse; // Raw response from payment gateway
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private Integer attemptNumber;
    
    private LocalDateTime attemptedAt = LocalDateTime.now();
    
    // Constructors
    public PaymentTransaction() {}
    
    public PaymentTransaction(String orderId, BigDecimal amount, Integer attemptNumber) {
        this.orderId = orderId;
        this.amount = amount;
        this.attemptNumber = attemptNumber;
        this.status = PaymentStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}