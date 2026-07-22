package com.medha.configmanagementservice.productservice.service;

import com.medha.configmanagementservice.productservice.dto.ProductRequest;
import com.medha.configmanagementservice.productservice.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProduct(Long id);

    List<ProductResponse> getAllProducts();

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
