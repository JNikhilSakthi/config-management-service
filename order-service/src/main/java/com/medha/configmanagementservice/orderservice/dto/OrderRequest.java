package com.medha.configmanagementservice.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Inbound payload for creating/updating an order. Bean-Validation annotated. */
public record OrderRequest(

        @NotBlank(message = "customerName must not be blank")
        String customerName,

        @NotBlank(message = "productName must not be blank")
        String productName,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than 0")
        BigDecimal unitPrice
) {
}
