package com.schedulex.model;

public enum PaymentStatus {
    PENDING,      // Order created, payment not attempted
    PROCESSING,   // Payment in progress
    FAILED,       // Payment failed
    SUCCESS,      // Payment successful
    REFUNDED,     // Payment refunded
    CANCELLED     // Order cancelled
}