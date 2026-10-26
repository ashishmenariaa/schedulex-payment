package com.schedulex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    /** Worker pool for job execution. Spring calls shutdown() on this bean when the app stops. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService jobWorkerPool(@Value("${schedulex.worker.threads:10}") int threads) {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory namedThreads = runnable -> {
            Thread thread = new Thread(runnable, "job-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(threads, namedThreads);
    }
}
