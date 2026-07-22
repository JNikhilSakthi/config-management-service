package com.medha.configmanagementservice.productservice.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Uniform error payload returned by GlobalExceptionHandler. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public ApiError(int status, String error, String message, List<String> details) {
        this(LocalDateTime.now(), status, error, message, details);
    }
}
