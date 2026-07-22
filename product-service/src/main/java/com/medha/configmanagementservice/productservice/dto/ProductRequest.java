package com.medha.configmanagementservice.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "name must not be blank")
        String name,

        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity cannot be negative")
        Integer stockQuantity
) {
}
