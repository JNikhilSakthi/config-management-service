package com.medha.configmanagementservice.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring application context (with @EnableConfigServer
 * wired in and the "native" profile active against config-repo/) starts cleanly.
 * If the native search-locations were misconfigured, or a YAML file in
 * config-repo/ were malformed, this test would fail at context startup.
 */
@SpringBootTest
@ActiveProfiles("native")
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a successful context refresh is the assertion.
    }
}
