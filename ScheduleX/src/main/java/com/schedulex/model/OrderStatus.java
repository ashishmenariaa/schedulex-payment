package com.schedulex.model;

public enum OrderStatus {
    CREATED,           // Order created
    PAYMENT_PENDING,   // Waiting for payment
    PAYMENT_FAILED,    // Payment failed, retry scheduled
    PAID,              // Payment successful
    PROCESSING,        // Order being prepared
    SHIPPED,           // Order shipped
    DELIVERED,         // Order delivered
    CANCELLED,         // Order cancelled
    REFUNDED           // Order refunded
}