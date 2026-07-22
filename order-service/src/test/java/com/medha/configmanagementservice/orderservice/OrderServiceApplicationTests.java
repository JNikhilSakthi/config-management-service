package com.medha.configmanagementservice.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the full application context must start WITHOUT a live Config Server
 * reachable (spring.cloud.config.enabled=false short-circuits the remote call, and
 * the "optional:" prefix on spring.config.import means startup does not fail even
 * with no config-server present). Spring Boot auto-configures an embedded H2
 * datasource since no spring.datasource.* is defined in this offline test mode.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a successful context refresh is the assertion.
    }
}
