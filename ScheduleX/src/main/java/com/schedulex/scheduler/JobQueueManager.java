package com.schedulex.scheduler;

import com.schedulex.model.Job;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class JobQueueManager {
    
    private static final Logger log = LoggerFactory.getLogger(JobQueueManager.class);
    
    private final BlockingQueue<Job> queue = new PriorityBlockingQueue<>(
        100, Comparator.comparing(Job::getPriority).reversed()
    );
    
    public void addJob(Job job) {
        queue.offer(job);
        log.info("Job queued: {}", job.getId());
    }
    
    public Job takeJob() throws InterruptedException {
        return queue.take();
    }
    
    public int getQueueSize() {
        return queue.size();
    }
}