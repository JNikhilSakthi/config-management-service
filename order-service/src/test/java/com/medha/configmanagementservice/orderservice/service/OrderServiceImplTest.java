package com.medha.configmanagementservice.orderservice.service;

import com.medha.configmanagementservice.orderservice.config.OrderProperties;
import com.medha.configmanagementservice.orderservice.dto.OrderRequest;
import com.medha.configmanagementservice.orderservice.dto.OrderResponse;
import com.medha.configmanagementservice.orderservice.entity.Order;
import com.medha.configmanagementservice.orderservice.entity.OrderStatus;
import com.medha.configmanagementservice.orderservice.exception.OrderLimitExceededException;
import com.medha.configmanagementservice.orderservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderProperties orderProperties;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderProperties = new OrderProperties();
        orderProperties.setWelcomeMessage("test-message");
        orderProperties.setMaxQuantityPerOrder(10);
        orderProperties.setDiscountEnabled(false);
        orderProperties.setDiscountPercentage(0);
        orderService = new OrderServiceImpl(orderRepository, orderProperties);
    }

    private Order savedOrder(Long id, Integer quantity, BigDecimal totalPrice) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerName("Nikhil");
        order.setProductName("Keyboard");
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.CREATED);
        return order;
    }

    @Test
    void createOrder_computesTotalPrice_withoutDiscount() {
        OrderRequest request = new OrderRequest("Nikhil", "Keyboard", 2, new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.totalPrice()).isEqualByComparingTo("100.00");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_appliesConfiguredDiscount_whenEnabled() {
        orderProperties.setDiscountEnabled(true);
        orderProperties.setDiscountPercentage(10);

        OrderRequest request = new OrderRequest("Nikhil", "Keyboard", 2, new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        // 100.00 subtotal - 10% = 90.00
        assertThat(response.totalPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void createOrder_rejectsQuantityAboveConfiguredMax() {
        orderProperties.setMaxQuantityPerOrder(3);
        OrderRequest request = new OrderRequest("Nikhil", "Keyboard", 5, new BigDecimal("50.00"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderLimitExceededException.class)
                .hasMessageContaining("exceeds the configured limit of 3");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrder_returnsMappedResponse_whenFound() {
        Order order = savedOrder(1L, 1, new BigDecimal("25.00"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.customerName()).isEqualTo("Nikhil");
    }

    @Test
    void getOrder_throwsResourceNotFound_whenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllOrders_mapsEveryEntity() {
        when(orderRepository.findAll()).thenReturn(List.of(
                savedOrder(1L, 1, BigDecimal.TEN),
                savedOrder(2L, 2, BigDecimal.valueOf(20))
        ));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertThat(responses).hasSize(2);
    }

    @Test
    void deleteOrder_removesExisting() {
        Order order = savedOrder(1L, 1, BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(order);
    }

    @Test
    void updateOrder_throwsResourceNotFound_whenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        OrderRequest request = new OrderRequest("Nikhil", "Mouse", 1, BigDecimal.TEN);

        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
