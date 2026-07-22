package com.medha.configmanagementservice.productservice.exception;

/** Thrown when a requested Product id does not exist. Mapped to HTTP 404 by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
