package com.medha.configmanagementservice.orderservice.exception;

/**
 * Thrown when the requested quantity exceeds {@code app.order.max-quantity-per-order},
 * a value sourced from the Config Server — proving that centrally-managed config can
 * drive live business rules, not just cosmetic banners.
 */
public class OrderLimitExceededException extends RuntimeException {
    public OrderLimitExceededException(String message) {
        super(message);
    }
}
