package com.schedulex.repository;

import com.schedulex.model.Job;
import com.schedulex.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
    List<Job> findByStatusAndScheduledTimeBefore(JobStatus status, LocalDateTime time);
    long countByStatus(JobStatus status);
}