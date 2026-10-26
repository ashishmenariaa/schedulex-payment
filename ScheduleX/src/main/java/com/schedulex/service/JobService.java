package com.schedulex.service;

import com.schedulex.dto.JobRequest;
import com.schedulex.exception.ResourceNotFoundException;
import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import com.schedulex.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
        if (job.getScheduledTime() == null) {
            job.setScheduledTime(LocalDateTime.now());
        }
        Job savedJob = jobRepository.save(job);
        log.info("Job created: {}", savedJob.getId());
        return savedJob;
    }

    @Transactional
    public Job createJob(JobRequest request) {
        Job job = new Job();
        job.setJobName(request.jobName());
        job.setJobData(request.jobData());
        job.setScheduledTime(request.scheduledTime());
        if (request.priority() != null) {
            job.setPriority(request.priority());
        }
        if (request.maxRetries() != null) {
            job.setMaxRetries(request.maxRetries());
        }
        return createJob(job);
    }

    public Job getJob(Long id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * Find due jobs and mark them QUEUED in the same transaction,
     * so the next poll doesn't pick them up again while they wait for a worker.
     */
    @Transactional
    public List<Job> claimDueJobs() {
        List<Job> dueJobs = jobRepository.findByStatusAndScheduledTimeBefore(JobStatus.PENDING, LocalDateTime.now());
        dueJobs.forEach(job -> job.setStatus(JobStatus.QUEUED));
        return jobRepository.saveAll(dueJobs);
    }

    /**
     * The in-memory queue is lost on restart, so jobs left QUEUED by a previous run go back to PENDING.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void requeueOrphanedJobs() {
        List<Job> orphaned = jobRepository.findByStatus(JobStatus.QUEUED);
        if (!orphaned.isEmpty()) {
            orphaned.forEach(job -> job.setStatus(JobStatus.PENDING));
            jobRepository.saveAll(orphaned);
            log.info("Re-queued {} jobs left over from the previous run", orphaned.size());
        }
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
