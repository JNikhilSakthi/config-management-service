package com.medha.configmanagementservice.orderservice.dto;

import com.medha.configmanagementservice.orderservice.entity.Order;
import com.medha.configmanagementservice.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Outbound representation of an {@link Order}. */
public record OrderResponse(
        Long id,
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getProductName(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
