package com.medha.configmanagementservice.productservice.controller;

import com.medha.configmanagementservice.productservice.config.ProductProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only window into the configuration product-service is CURRENTLY running
 * with. See order-service's ConfigInfoController for the full refresh workflow
 * this endpoint is designed to demonstrate.
 */
@RestController
@RequestMapping("/api/products/config-info")
public class ConfigInfoController {

    private final ProductProperties productProperties;

    @Value("${info.config-source:unknown}")
    private String configSource;

    public ConfigInfoController(ProductProperties productProperties) {
        this.productProperties = productProperties;
    }

    @GetMapping
    public Map<String, Object> currentConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("welcomeMessage", productProperties.getWelcomeMessage());
        body.put("lowStockThreshold", productProperties.getLowStockThreshold());
        body.put("priceCurrency", productProperties.getPriceCurrency());
        body.put("configSource", configSource);
        return body;
    }
}
