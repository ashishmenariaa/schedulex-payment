package com.schedulex.controller;

import com.schedulex.dto.JobRequest;
import com.schedulex.model.Job;
import com.schedulex.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@Valid @RequestBody JobRequest request) {
        return new ResponseEntity<>(jobService.createJob(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<JobService.JobStats> getStats() {
        return ResponseEntity.ok(jobService.getStats());
    }
}
