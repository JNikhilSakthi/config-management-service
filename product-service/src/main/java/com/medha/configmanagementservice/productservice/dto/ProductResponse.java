package com.medha.configmanagementservice.productservice.dto;

import com.medha.configmanagementservice.productservice.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        boolean lowStock
) {
    public static ProductResponse from(Product product, int lowStockThreshold) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStockQuantity() != null && product.getStockQuantity() <= lowStockThreshold
        );
    }
}
