package com.medha.configmanagementservice.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for order-service.
 *
 * Nothing Config-Server-specific needs to happen in code here: the moment
 * spring-cloud-starter-config is on the classpath and
 * spring.config.import=configserver:... is set (see application.yml), Spring
 * Boot's config-data import mechanism reaches out to config-server during
 * context startup, BEFORE any {@code @Configuration} classes are processed,
 * and merges the returned properties into the Environment as just another
 * (high-priority) PropertySource.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
