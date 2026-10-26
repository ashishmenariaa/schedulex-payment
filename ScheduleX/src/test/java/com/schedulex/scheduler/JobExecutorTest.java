package com.schedulex.scheduler;

import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import com.schedulex.service.JobService;
import com.schedulex.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutorTest {

    @Mock private JobService jobService;
    @Mock private OrderService orderService;
    @Mock private ExecutorService pool;

    private JobExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new JobExecutor(jobService, orderService, pool);
    }

    @Test
    void paymentRetryJobDelegatesToOrderServiceAndCompletes() {
        Job job = job(1L, OrderService.PAYMENT_RETRY_JOB_PREFIX + "ORD_9", "ORD_9");
        when(orderService.retryIfDue("ORD_9")).thenReturn(true);

        executor.run(job);

        InOrder order = inOrder(jobService, orderService);
        order.verify(jobService).updateJobStatus(1L, JobStatus.RUNNING);
        order.verify(orderService).retryIfDue("ORD_9");
        order.verify(jobService).updateJobStatus(1L, JobStatus.COMPLETED);
        verify(jobService, never()).incrementRetry(anyLong());
    }

    @Test
    void failingJobIsHandedBackForRetryAccounting() {
        Job job = job(2L, OrderService.PAYMENT_RETRY_JOB_PREFIX + "ORD_X", "ORD_X");
        when(orderService.retryIfDue("ORD_X")).thenThrow(new IllegalStateException("db down"));

        executor.run(job);

        verify(jobService).incrementRetry(2L);
        verify(jobService, never()).updateJobStatus(2L, JobStatus.COMPLETED);
    }

    @Test
    void executeJobSubmitsToWorkerPool() {
        executor.executeJob(job(3L, "Report", null));

        verify(pool).submit(any(Runnable.class));
        verifyNoInteractions(jobService);
    }

    private static Job job(Long id, String name, String data) {
        Job job = new Job();
        job.setId(id);
        job.setJobName(name);
        job.setJobData(data);
        return job;
    }
}
