package com.schedulex.service;

import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import com.schedulex.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {
    private final JobRepository jobRepository;
    
    @Transactional
    public Job createJob(Job job) {
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);
        log.info("Job created: {}", savedJob.getId());
        return savedJob;
    }
    
    public Job getJob(Long id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Job not found: " + id));
    }
    
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    
    public List<Job> getDueJobs() {
        return jobRepository.findByStatusAndScheduledTimeBefore(JobStatus.PENDING, LocalDateTime.now());
    }
    
    @Transactional
    public void updateJobStatus(Long jobId, JobStatus status) {
        Job job = getJob(jobId);
        job.setStatus(status);
        if (status == JobStatus.RUNNING) {
            job.setLastExecutionTime(LocalDateTime.now());
        }
        jobRepository.save(job);
    }
    
    @Transactional
    public void incrementRetry(Long jobId) {
        Job job = getJob(jobId);
        job.setRetryCount(job.getRetryCount() + 1);
        if (job.canRetry()) {
            job.setStatus(JobStatus.PENDING);
        } else {
            job.setStatus(JobStatus.FAILED);
        }
        jobRepository.save(job);
    }
    
    public JobStats getStats() {
        return new JobStats(
            jobRepository.count(),
            jobRepository.countByStatus(JobStatus.PENDING),
            jobRepository.countByStatus(JobStatus.RUNNING),
            jobRepository.countByStatus(JobStatus.COMPLETED),
            jobRepository.countByStatus(JobStatus.FAILED)
        );
    }
    
    public record JobStats(long total, long pending, long running, long completed, long failed) {}
}