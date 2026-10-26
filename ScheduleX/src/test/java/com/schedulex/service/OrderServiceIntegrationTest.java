package com.schedulex.service;

import com.schedulex.dto.CreateOrderRequest;
import com.schedulex.exception.PaymentConflictException;
import com.schedulex.gateway.PaymentGateway;
import com.schedulex.gateway.PaymentGateway.GatewayResult;
import com.schedulex.model.*;
import com.schedulex.repository.JobRepository;
import com.schedulex.repository.OrderRepository;
import com.schedulex.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderServiceIntegrationTest {

    private static final GatewayResult DECLINED = GatewayResult.failure("Insufficient funds in account", "{}");
    private static final GatewayResult CAPTURED = GatewayResult.success("PAY_OK", "{}");

    @Autowired private OrderService orderService;
    @Autowired private JobService jobService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;
    @Autowired private JobRepository jobRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        jobRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void failedFirstPaymentSchedulesOneRetryJobSixHoursLater() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED);

        Order order = orderService.createOrder(request("ORD_FAIL"));

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getRetryCount()).isEqualTo(1);
        assertThat(order.getNextRetryTime())
            .isCloseTo(LocalDateTime.now().plusHours(6), within(1, ChronoUnit.MINUTES));

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getJobData()).isEqualTo("ORD_FAIL");
        assertThat(jobs.get(0).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(jobs.get(0).getPriority()).isEqualTo(8);
    }

    @Test
    void successfulRetryMarksOrderPaid() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED, CAPTURED);
        orderService.createOrder(request("ORD_RETRY"));

        Order paid = orderService.retryPayment("ORD_RETRY");

        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paid.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaymentId()).isEqualTo("PAY_OK");
        assertThat(paid.getNextRetryTime()).isNull();
        assertThat(transactionRepository.countByOrderId("ORD_RETRY")).isEqualTo(2);
    }

    @Test
    void paidOrderIsNeverChargedAgain() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(CAPTURED);
        orderService.createOrder(request("ORD_PAID"));

        assertThatThrownBy(() -> orderService.retryPayment("ORD_PAID"))
            .isInstanceOf(PaymentConflictException.class);

        verify(paymentGateway, times(1)).charge(any(), any(), anyInt());
        assertThat(transactionRepository.countByOrderId("ORD_PAID")).isEqualTo(1);
    }

    @Test
    void orderIsCancelledAfterMaxRetries() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED);
        orderService.createOrder(request("ORD_CANCEL"));
        orderService.retryPayment("ORD_CANCEL");
        Order cancelled = orderService.retryPayment("ORD_CANCEL");

        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(cancelled.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getRetryCount()).isEqualTo(3);
        assertThatThrownBy(() -> orderService.retryPayment("ORD_CANCEL"))
            .isInstanceOf(PaymentConflictException.class);
        verify(paymentGateway, times(3)).charge(any(), any(), anyInt());
    }

    @Test
    void retryJobFiringBeforeNextRetryTimeIsSkipped() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED);
        orderService.createOrder(request("ORD_STALE")); // next retry is 6h away

        assertThat(orderService.retryIfDue("ORD_STALE")).isFalse();
        verify(paymentGateway, times(1)).charge(any(), any(), anyInt());
    }

    @Test
    void retryJobFiresWhenDue() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED, CAPTURED);
        orderService.createOrder(request("ORD_DUE"));
        Order order = orderRepository.findByOrderId("ORD_DUE").orElseThrow();
        order.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        orderRepository.save(order);

        assertThat(orderService.retryIfDue("ORD_DUE")).isTrue();
        assertThat(orderService.getOrder("ORD_DUE").getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void duplicateOrderIdIsRejected() {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(CAPTURED);
        orderService.createOrder(request("ORD_DUP"));

        assertThatThrownBy(() -> orderService.createOrder(request("ORD_DUP")))
            .isInstanceOf(PaymentConflictException.class);
    }

    @Test
    void concurrentRetriesOfTheSameOrderChargeOnlyOnce() throws Exception {
        when(paymentGateway.charge(any(), any(), anyInt())).thenReturn(DECLINED);
        orderService.createOrder(request("ORD_RACE"));
        reset(paymentGateway);

        AtomicInteger charges = new AtomicInteger();
        when(paymentGateway.charge(any(), any(), anyInt())).thenAnswer(inv -> {
            charges.incrementAndGet();
            Thread.sleep(300); // hold the row while the other thread tries
            return CAPTURED;
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                orderService.retryPayment("ORD_RACE");
                return true;
            } catch (RuntimeException rejected) {
                return false;
            }
        };
        Future<Boolean> first = pool.submit(attempt);
        Future<Boolean> second = pool.submit(attempt);
        start.countDown();

        int succeeded = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
        pool.shutdown();

        assertThat(charges.get()).isEqualTo(1);
        assertThat(succeeded).isEqualTo(1);
        assertThat(orderService.getOrder("ORD_RACE").getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void claimedJobsAreNotClaimedAgain() {
        Job job = new Job();
        job.setJobName("Report");
        job.setScheduledTime(LocalDateTime.now().minus(Duration.ofMinutes(1)));
        jobService.createJob(job);

        assertThat(jobService.claimDueJobs()).hasSize(1);
        assertThat(jobService.claimDueJobs()).isEmpty();
        assertThat(jobRepository.findAll().get(0).getStatus()).isEqualTo(JobStatus.QUEUED);
    }

    private static CreateOrderRequest request(String orderId) {
        return new CreateOrderRequest(orderId, "CUST_1", "Asha Verma", "asha@example.com",
                                      "9876543210", new BigDecimal("999.00"), "[]");
    }
}
