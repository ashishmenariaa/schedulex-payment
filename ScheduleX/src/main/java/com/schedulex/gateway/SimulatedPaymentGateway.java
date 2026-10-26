package com.schedulex.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulates a payment provider so the retry flow can be demonstrated without real money.
 * The first attempt always fails; retries succeed 70% (2nd attempt) and 90% (later attempts) of the time.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final String[] FAILURE_REASONS = {
        "Insufficient funds in account",
        "Card expired",
        "Transaction declined by bank",
        "Invalid CVV",
        "Card limit exceeded",
        "Payment gateway timeout",
        "3D Secure authentication failed",
        "Card blocked by bank"
    };

    private final Random random = new Random();
    private final long latencyMs;

    public SimulatedPaymentGateway(@Value("${schedulex.gateway.simulated-latency-ms:2000}") long latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Override
    public GatewayResult charge(String orderId, BigDecimal amount, int attemptNumber) {
        simulateNetworkLatency();

        int successRate = attemptNumber == 1 ? 0 : attemptNumber == 2 ? 70 : 90;
        if (random.nextInt(100) < successRate) {
            String paymentId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            return GatewayResult.success(paymentId, "{\"status\":\"captured\",\"method\":\"card\"}");
        }

        String reason = FAILURE_REASONS[random.nextInt(FAILURE_REASONS.length)];
        return GatewayResult.failure(reason, "{\"status\":\"failed\",\"error\":\"" + reason + "\"}");
    }

    private void simulateNetworkLatency() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
