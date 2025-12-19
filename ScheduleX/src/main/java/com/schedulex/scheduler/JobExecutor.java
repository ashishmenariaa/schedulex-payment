package com.schedulex.scheduler;

import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import com.schedulex.model.Order;
import com.schedulex.service.JobService;
import com.schedulex.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class JobExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    
    private final JobService jobService;
    private final OrderService orderService;
    private final ExecutorService executorService;
    
    public JobExecutor(JobService jobService, OrderService orderService) {
        this.jobService = jobService;
        this.orderService = orderService;
        this.executorService = Executors.newFixedThreadPool(10);
    }
    
    @PostConstruct
    public void init() {
        log.info("💼 JobExecutor started with 10 worker threads");
    }
    
    public void executeJob(Job job) {
        executorService.submit(() -> {
            try {
                log.info("⚡ Executing job: ID={}, Name={}", job.getId(), job.getJobName());
                
                // Update status to RUNNING
                jobService.updateJobStatus(job.getId(), JobStatus.RUNNING);
                
                // Check if this is a payment retry job
                if (job.getJobName().startsWith("Payment Retry")) {
                    executePaymentRetry(job);
                } else {
                    // Execute other job types (generic jobs)
                    executeGenericJob(job);
                }
                
                // Mark job as completed
                jobService.updateJobStatus(job.getId(), JobStatus.COMPLETED);
                log.info("✅ Job completed: ID={}", job.getId());
                
            } catch (Exception e) {
                log.error("❌ Job execution failed: ID={}, Error: {}", 
                         job.getId(), e.getMessage(), e);
                
                // Handle job failure
                if (job.canRetry()) {
                    jobService.incrementRetry(job.getId());
                    log.info("🔄 Job retry scheduled: ID={}", job.getId());
                } else {
                    jobService.updateJobStatus(job.getId(), JobStatus.FAILED);
                    log.error("💀 Job failed permanently: ID={}", job.getId());
                }
            }
        });
    }
    
    /**
     * Execute payment retry logic
     */
    private void executePaymentRetry(Job job) {
        String orderId = job.getJobData(); // Order ID stored in jobData
        
        log.info("💳 Processing payment retry for order: {}", orderId);
        
        try {
            // Get the order
            Order order = orderService.getOrder(orderId);
            
            log.info("📋 Order details: ID={}, Amount=₹{}, Retry={}/{}", 
                     order.getOrderId(), 
                     order.getAmount(),
                     order.getRetryCount(),
                     order.getMaxRetries());
            
            // Attempt payment
            orderService.attemptPayment(order);
            
            log.info("✅ Payment retry processing completed for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("❌ Payment retry failed for order: {} - Error: {}", 
                     orderId, e.getMessage());
            throw new RuntimeException("Payment retry failed", e);
        }
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