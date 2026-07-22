package com.medha.configmanagementservice.productservice.service;

import com.medha.configmanagementservice.productservice.config.ProductProperties;
import com.medha.configmanagementservice.productservice.dto.ProductRequest;
import com.medha.configmanagementservice.productservice.dto.ProductResponse;
import com.medha.configmanagementservice.productservice.entity.Product;
import com.medha.configmanagementservice.productservice.exception.ResourceNotFoundException;
import com.medha.configmanagementservice.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for the Product domain. The "lowStock" flag returned to
 * clients is computed against {@code app.product.low-stock-threshold}, a
 * value sourced from the Config Server via the {@code @RefreshScope}
 * {@link ProductProperties} bean — so lowering/raising the threshold in
 * config-repo and hitting POST /actuator/refresh changes which products are
 * flagged as low-stock without any code change or restart.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductProperties productProperties;

    public ProductServiceImpl(ProductRepository productRepository, ProductProperties productProperties) {
        this.productRepository = productRepository;
        this.productProperties = productProperties;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Override
    public ProductResponse getProduct(Long id) {
        return toResponse(findProductOrThrow(id));
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existing = findProductOrThrow(id);
        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setStockQuantity(request.stockQuantity());

        Product saved = productRepository.save(existing);
        return toResponse(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        Product existing = findProductOrThrow(id);
        productRepository.delete(existing);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.from(product, productProperties.getLowStockThreshold());
    }
}
