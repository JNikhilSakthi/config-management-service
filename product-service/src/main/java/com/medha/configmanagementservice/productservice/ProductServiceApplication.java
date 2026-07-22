package com.medha.configmanagementservice.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for product-service. See order-service's OrderServiceApplication
 * for a detailed explanation of how spring.config.import=configserver:...
 * resolves remote configuration during startup — the mechanism is identical here.
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
