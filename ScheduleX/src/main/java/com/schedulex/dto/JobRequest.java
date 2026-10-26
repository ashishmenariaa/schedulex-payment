package com.schedulex.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Client input for a generic scheduled job. A missing scheduledTime means "run now". */
public record JobRequest(
    @NotBlank @Size(max = 200) String jobName,
    String jobData,
    LocalDateTime scheduledTime,
    @Min(0) @Max(10) Integer priority,
    @Min(0) @Max(10) Integer maxRetries
) {}
