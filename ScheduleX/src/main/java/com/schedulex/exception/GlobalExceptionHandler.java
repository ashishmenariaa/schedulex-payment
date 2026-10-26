package com.schedulex.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(LocalDateTime timestamp, int status, String error, String message,
                           Map<String, String> fieldErrors) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(PaymentConflictException.class)
    public ResponseEntity<ApiError> handlePaymentConflict(PaymentConflictException e) {
        return build(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> handleConcurrentUpdate(RuntimeException e) {
        log.warn("Concurrent or duplicate write rejected: {}", e.getMessage());
        return build(HttpStatus.CONFLICT, "The order was modified by another request or already exists", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(err -> fieldErrors.putIfAbsent(err.getField(), err.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
