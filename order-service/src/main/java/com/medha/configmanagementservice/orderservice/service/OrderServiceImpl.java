package com.medha.configmanagementservice.orderservice.service;

import com.medha.configmanagementservice.orderservice.config.OrderProperties;
import com.medha.configmanagementservice.orderservice.dto.OrderRequest;
import com.medha.configmanagementservice.orderservice.dto.OrderResponse;
import com.medha.configmanagementservice.orderservice.entity.Order;
import com.medha.configmanagementservice.orderservice.exception.OrderLimitExceededException;
import com.medha.configmanagementservice.orderservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Business logic for the Order domain. Two of the rules here are driven directly
 * by values pulled from the Config Server through {@link OrderProperties}:
 * the max-quantity-per-order guardrail, and whether/how much discount to apply
 * to the computed total. Because OrderProperties is {@code @RefreshScope}, an
 * operator can change either rule at runtime via POST /actuator/refresh.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderProperties orderProperties;

    public OrderServiceImpl(OrderRepository orderRepository, OrderProperties orderProperties) {
        this.orderRepository = orderRepository;
        this.orderProperties = orderProperties;
    }

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        validateQuantity(request.quantity());

        Order order = new Order();
        order.setCustomerName(request.customerName());
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setTotalPrice(computeTotalPrice(request));

        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved);
    }

    @Override
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(findOrderOrThrow(id));
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        validateQuantity(request.quantity());

        Order existing = findOrderOrThrow(id);
        existing.setCustomerName(request.customerName());
        existing.setProductName(request.productName());
        existing.setQuantity(request.quantity());
        existing.setTotalPrice(computeTotalPrice(request));

        Order saved = orderRepository.save(existing);
        return OrderResponse.from(saved);
    }

    @Override
    public void deleteOrder(Long id) {
        Order existing = findOrderOrThrow(id);
        orderRepository.delete(existing);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private void validateQuantity(int quantity) {
        int max = orderProperties.getMaxQuantityPerOrder();
        if (quantity > max) {
            throw new OrderLimitExceededException(
                    "Requested quantity " + quantity + " exceeds the configured limit of " + max
                            + " (app.order.max-quantity-per-order, sourced from Config Server)");
        }
    }

    private BigDecimal computeTotalPrice(OrderRequest request) {
        BigDecimal subtotal = request.unitPrice().multiply(BigDecimal.valueOf(request.quantity()));
        if (!orderProperties.isDiscountEnabled() || orderProperties.getDiscountPercentage() <= 0) {
            return subtotal.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discountFraction = BigDecimal.valueOf(orderProperties.getDiscountPercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal discountAmount = subtotal.multiply(discountFraction);
        return subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
