package com.schedulex.gateway;

import java.math.BigDecimal;

/**
 * Boundary to the external payment provider.
 * The app ships with {@link SimulatedPaymentGateway}; a Razorpay/Stripe client would implement this interface.
 */
public interface PaymentGateway {

    GatewayResult charge(String orderId, BigDecimal amount, int attemptNumber);

    record GatewayResult(boolean success, String paymentId, String errorMessage, String rawResponse) {

        public static GatewayResult success(String paymentId, String rawResponse) {
            return new GatewayResult(true, paymentId, null, rawResponse);
        }

        public static GatewayResult failure(String errorMessage, String rawResponse) {
            return new GatewayResult(false, null, errorMessage, rawResponse);
        }
    }
}
