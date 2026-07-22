package com.medha.configmanagementservice.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medha.configmanagementservice.orderservice.dto.OrderRequest;
import com.medha.configmanagementservice.orderservice.dto.OrderResponse;
import com.medha.configmanagementservice.orderservice.entity.OrderStatus;
import com.medha.configmanagementservice.orderservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_returns201_withLocationBody() throws Exception {
        OrderRequest request = new OrderRequest("Nikhil", "Keyboard", 2, new BigDecimal("50.00"));
        OrderResponse response = new OrderResponse(1L, "Nikhil", "Keyboard", 2,
                new BigDecimal("100.00"), OrderStatus.CREATED, LocalDateTime.now());
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Nikhil"));
    }

    @Test
    void createOrder_rejectsInvalidPayload_with400() throws Exception {
        String invalidJson = """
                {"customerName":"","productName":"Keyboard","quantity":0,"unitPrice":0}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_returns404_whenServiceThrowsNotFound() throws Exception {
        when(orderService.getOrder(eq(99L))).thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }

    @Test
    void getAllOrders_returnsList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(
                new OrderResponse(1L, "Nikhil", "Keyboard", 1, BigDecimal.TEN, OrderStatus.CREATED, LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deleteOrder_returns204() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }
}
