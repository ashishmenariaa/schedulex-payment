package com.schedulex.model;

public enum JobStatus {
    PENDING,
    QUEUED,     // Claimed by the poller and waiting in the in-memory queue
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}