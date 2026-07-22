package com.medha.configmanagementservice.orderservice.service;

import com.medha.configmanagementservice.orderservice.dto.OrderRequest;
import com.medha.configmanagementservice.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrder(Long id);

    List<OrderResponse> getAllOrders();

    OrderResponse updateOrder(Long id, OrderRequest request);

    void deleteOrder(Long id);
}
