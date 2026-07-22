package com.medha.configmanagementservice.orderservice.controller;

import com.medha.configmanagementservice.orderservice.config.OrderProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only window into the configuration order-service is CURRENTLY running with.
 *
 * <p>This is the endpoint you hit before and after {@code POST /actuator/refresh}
 * to observe values sourced from the Config Server change live:</p>
 * <pre>
 * curl http://localhost:8081/api/orders/config-info
 * # ...edit config-server/config-repo/order-service.yml, restart config-server...
 * curl -X POST http://localhost:8081/actuator/refresh
 * curl http://localhost:8081/api/orders/config-info   # now reflects new values
 * </pre>
 */
@RestController
@RequestMapping("/api/orders/config-info")
public class ConfigInfoController {

    private final OrderProperties orderProperties;

    // Default here only guards against config-server being unreachable (see the
    // "optional:" prefix on spring.config.import); in normal operation this value
    // always comes from config-repo/order-service*.yml on the Config Server.
    @Value("${product-service.base-url:http://localhost:8082}")
    private String productServiceBaseUrl;

    @Value("${info.config-source:unknown}")
    private String configSource;

    public ConfigInfoController(OrderProperties orderProperties) {
        this.orderProperties = orderProperties;
    }

    @GetMapping
    public Map<String, Object> currentConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("welcomeMessage", orderProperties.getWelcomeMessage());
        body.put("maxQuantityPerOrder", orderProperties.getMaxQuantityPerOrder());
        body.put("discountEnabled", orderProperties.isDiscountEnabled());
        body.put("discountPercentage", orderProperties.getDiscountPercentage());
        body.put("productServiceBaseUrl", productServiceBaseUrl);
        body.put("configSource", configSource);
        return body;
    }
}
