package com.medha.configmanagementservice.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:"
})
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a successful context refresh is the assertion.
    }
}
