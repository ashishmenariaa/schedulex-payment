package com.schedulex.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String jobName;
    
    @Enumerated(EnumType.STRING)
    private JobType jobType = JobType.ONE_TIME;
    
    @Column(columnDefinition = "TEXT")
    private String jobData;
    
    private String cronExpression;
    private LocalDateTime scheduledTime;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;
    
    private Integer priority = 0;
    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime lastExecutionTime;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
}