package com.schedulex.service;

import com.schedulex.gateway.PaymentGateway;
import com.schedulex.gateway.PaymentGateway.GatewayResult;
import com.schedulex.model.Order;
import com.schedulex.model.PaymentStatus;
import com.schedulex.model.PaymentTransaction;
import com.schedulex.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    private PaymentService paymentService;

    private final Order order = new Order("ORD_1", "CUST_1", "Asha", "asha@example.com",
                                          "9876543210", new BigDecimal("499.00"), "[]");

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(transactionRepository, paymentGateway);
        lenient().when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulChargeIsRecordedWithPaymentId() {
        when(paymentGateway.charge(eq("ORD_1"), any(), eq(2)))
            .thenReturn(GatewayResult.success("PAY_123", "{\"status\":\"captured\"}"));

        PaymentTransaction txn = paymentService.processPayment(order, 2);

        assertThat(txn.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(txn.getPaymentId()).isEqualTo("PAY_123");
        assertThat(txn.getAttemptNumber()).isEqualTo(2);
        assertThat(txn.getAmount()).isEqualByComparingTo("499.00");
    }

    @Test
    void declinedChargeIsRecordedWithReason() {
        when(paymentGateway.charge(any(), any(), anyInt()))
            .thenReturn(GatewayResult.failure("Card expired", "{\"status\":\"failed\"}"));

        PaymentTransaction txn = paymentService.processPayment(order, 1);

        assertThat(txn.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(txn.getErrorMessage()).isEqualTo("Card expired");
        assertThat(txn.getPaymentId()).isNull();
    }

    @Test
    void gatewayExceptionIsRecordedAsFailureInsteadOfPropagating() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenThrow(new IllegalStateException("timeout"));

        PaymentTransaction txn = paymentService.processPayment(order, 1);

        assertThat(txn.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(txn.getErrorMessage()).contains("timeout");
    }

    @ParameterizedTest(name = "after {0} failed attempt(s) wait {1}h")
    @CsvSource({"1, 6", "2, 24", "3, 72", "7, 72", "0, 6"})
    void retryBackoffFollowsSchedule(int failedAttempts, long expectedHours) {
        LocalDateTime expected = LocalDateTime.now().plus(Duration.ofHours(expectedHours));

        assertThat(paymentService.calculateNextRetryTime(failedAttempts))
            .isCloseTo(expected, within(Duration.ofSeconds(5).toMillis(), java.time.temporal.ChronoUnit.MILLIS));
    }
}
