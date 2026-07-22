package com.medha.configmanagementservice.productservice.service;

import com.medha.configmanagementservice.productservice.config.ProductProperties;
import com.medha.configmanagementservice.productservice.dto.ProductRequest;
import com.medha.configmanagementservice.productservice.dto.ProductResponse;
import com.medha.configmanagementservice.productservice.entity.Product;
import com.medha.configmanagementservice.productservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductProperties productProperties;
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productProperties = new ProductProperties();
        productProperties.setWelcomeMessage("test-message");
        productProperties.setLowStockThreshold(5);
        productProperties.setPriceCurrency("INR");
        productService = new ProductServiceImpl(productRepository, productProperties);
    }

    private Product product(Long id, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Widget");
        product.setDescription("A widget");
        product.setPrice(new BigDecimal("19.99"));
        product.setStockQuantity(stock);
        return product;
    }

    @Test
    void createProduct_flagsLowStock_belowConfiguredThreshold() {
        ProductRequest request = new ProductRequest("Widget", "A widget", new BigDecimal("19.99"), 3);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.lowStock()).isTrue();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void createProduct_doesNotFlagLowStock_aboveConfiguredThreshold() {
        ProductRequest request = new ProductRequest("Widget", "A widget", new BigDecimal("19.99"), 50);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request);

        assertThat(response.lowStock()).isFalse();
    }

    @Test
    void getProduct_throwsResourceNotFound_whenMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void getAllProducts_mapsEveryEntity_withConfiguredThreshold() {
        when(productRepository.findAll()).thenReturn(List.of(product(1L, 2), product(2L, 100)));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).lowStock()).isTrue();
        assertThat(responses.get(1).lowStock()).isFalse();
    }

    @Test
    void deleteProduct_removesExisting() {
        Product existing = product(1L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void updateProduct_throwsResourceNotFound_whenMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        ProductRequest request = new ProductRequest("Widget", "desc", BigDecimal.TEN, 1);

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
