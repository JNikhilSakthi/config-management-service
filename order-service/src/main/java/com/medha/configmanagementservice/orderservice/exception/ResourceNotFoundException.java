package com.medha.configmanagementservice.orderservice.exception;

/** Thrown when a requested Order id does not exist. Mapped to HTTP 404 by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
