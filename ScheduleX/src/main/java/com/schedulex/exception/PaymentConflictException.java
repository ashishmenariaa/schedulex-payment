package com.schedulex.exception;

/** Thrown when a payment is requested for an order that is already paid, in progress or cancelled. */
public class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(String message) {
        super(message);
    }
}
