package com.schedulex.scheduler;

import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import com.schedulex.service.JobService;
import com.schedulex.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;

@Component
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final JobService jobService;
    private final OrderService orderService;
    private final ExecutorService executorService;

    public JobExecutor(JobService jobService, OrderService orderService, ExecutorService jobWorkerPool) {
        this.jobService = jobService;
        this.orderService = orderService;
        this.executorService = jobWorkerPool;
    }

    public void executeJob(Job job) {
        executorService.submit(() -> run(job));
    }

    /**
     * Runs one job on the calling thread. Package-private so tests can run it synchronously.
     */
    void run(Job job) {
        try {
            log.info("⚡ Executing job: ID={}, Name={}", job.getId(), job.getJobName());

            jobService.updateJobStatus(job.getId(), JobStatus.RUNNING);

            if (job.getJobName().startsWith(OrderService.PAYMENT_RETRY_JOB_PREFIX)) {
                executePaymentRetry(job);
            } else {
                executeGenericJob(job);
            }

            jobService.updateJobStatus(job.getId(), JobStatus.COMPLETED);
            log.info("✅ Job completed: ID={}", job.getId());

        } catch (Exception e) {
            log.error("❌ Job execution failed: ID={}, Error: {}",
                     job.getId(), e.getMessage(), e);

            // incrementRetry re-checks the persisted retry count and marks the job FAILED when exhausted
            jobService.incrementRetry(job.getId());
        }
    }

    /**
     * Execute payment retry logic
     */
    private void executePaymentRetry(Job job) {
        String orderId = job.getJobData(); // Order ID stored in jobData

        log.info("💳 Processing payment retry for order: {}", orderId);

        boolean attempted = orderService.retryIfDue(orderId);

        log.info(attempted ? "✅ Payment retry processed for order: {}" : "⏭️ Payment retry not needed for order: {}",
                 orderId);
    }

    /**
     * Execute generic jobs (non-payment)
     */
    private void executeGenericJob(Job job) throws InterruptedException {
        log.info("⚙️ Executing generic job: {}", job.getJobName());
        log.info("📦 Job data: {}", job.getJobData());

        // Simulate job processing
        Thread.sleep(2000);

        log.info("✅ Generic job processing complete");
    }
}
