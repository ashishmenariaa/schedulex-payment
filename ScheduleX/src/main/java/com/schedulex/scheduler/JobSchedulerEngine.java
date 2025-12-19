package com.schedulex.scheduler;

import com.schedulex.model.Job;
import com.schedulex.model.Order;
import com.schedulex.service.JobService;
import com.schedulex.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
public class JobSchedulerEngine {
    
    private static final Logger log = LoggerFactory.getLogger(JobSchedulerEngine.class);
    
    private final JobService jobService;
    private final OrderService orderService;
    private final JobQueueManager queueManager;
    private final JobExecutor jobExecutor;
    
    public JobSchedulerEngine(JobService jobService,
                             OrderService orderService,
                             JobQueueManager queueManager, 
                             JobExecutor jobExecutor) {
        this.jobService = jobService;
        this.orderService = orderService;
        this.queueManager = queueManager;
        this.jobExecutor = jobExecutor;
    }
    
    @PostConstruct
    public void startConsumer() {
        Thread consumer = new Thread(() -> {
            log.info("🚀 Job Consumer started");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Job job = queueManager.takeJob();
                    jobExecutor.executeJob(job);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumer.setDaemon(true);
        consumer.start();
    }
    
    /**
     * Poll for due jobs every 5 seconds
     */
    @Scheduled(fixedDelayString = "${schedulex.scheduler.poll-interval:5000}")
    public void pollDueJobs() {
        List<Job> dueJobs = jobService.getDueJobs();
        if (!dueJobs.isEmpty()) {
            log.info("📋 Found {} due jobs", dueJobs.size());
            dueJobs.forEach(queueManager::addJob);
        }
    }
    
    /**
     * Check for payment retries every 30 seconds
     * (More frequent than regular job polling for critical payments)
     */
    @Scheduled(fixedDelay = 30000)
    public void checkPaymentRetries() {
        List<Order> ordersNeedingRetry = orderService.getOrdersNeedingRetry();
        
        if (!ordersNeedingRetry.isEmpty()) {
            log.info("💰 Found {} orders needing payment retry", ordersNeedingRetry.size());
            
            for (Order order : ordersNeedingRetry) {
                log.info("🔄 Triggering payment retry for order: {}", order.getOrderId());
                orderService.attemptPayment(order);
            }
        }
    }
}