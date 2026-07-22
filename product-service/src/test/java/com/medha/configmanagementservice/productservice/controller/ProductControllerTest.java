package com.medha.configmanagementservice.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medha.configmanagementservice.productservice.dto.ProductRequest;
import com.medha.configmanagementservice.productservice.dto.ProductResponse;
import com.medha.configmanagementservice.productservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void createProduct_returns201() throws Exception {
        ProductRequest request = new ProductRequest("Widget", "A widget", new BigDecimal("19.99"), 3);
        ProductResponse response = new ProductResponse(1L, "Widget", "A widget", new BigDecimal("19.99"), 3, true);
        when(productService.createProduct(any())).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lowStock").value(true));
    }

    @Test
    void createProduct_rejectsInvalidPayload_with400() throws Exception {
        String invalidJson = """
                {"name":"","description":"x","price":0,"stockQuantity":-1}
                """;

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_returns404_whenServiceThrowsNotFound() throws Exception {
        when(productService.getProduct(eq(99L))).thenThrow(new ResourceNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void getAllProducts_returnsList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(
                new ProductResponse(1L, "Widget", "A widget", BigDecimal.TEN, 10, false)
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
