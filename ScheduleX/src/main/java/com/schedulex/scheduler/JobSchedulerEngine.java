package com.schedulex.scheduler;

import com.schedulex.model.Job;
import com.schedulex.service.JobService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Producer/consumer pipeline: the poller claims due jobs from the database and puts them on a
 * priority queue; a single consumer thread hands them to the worker pool.
 * Payment retries are ordinary jobs created by OrderService, so there is exactly one retry path.
 */
@Component
public class JobSchedulerEngine {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulerEngine.class);

    private final JobService jobService;
    private final JobQueueManager queueManager;
    private final JobExecutor jobExecutor;

    public JobSchedulerEngine(JobService jobService,
                             JobQueueManager queueManager,
                             JobExecutor jobExecutor) {
        this.jobService = jobService;
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
        }, "job-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    /**
     * Poll for due jobs (every 5 seconds by default)
     */
    @Scheduled(fixedDelayString = "${schedulex.scheduler.poll-interval:5000}")
    public void pollDueJobs() {
        List<Job> dueJobs = jobService.claimDueJobs();
        if (!dueJobs.isEmpty()) {
            log.info("📋 Found {} due jobs", dueJobs.size());
            dueJobs.forEach(queueManager::addJob);
        }
    }
}
