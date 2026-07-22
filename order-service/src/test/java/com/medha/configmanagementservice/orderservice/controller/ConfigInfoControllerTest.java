package com.medha.configmanagementservice.orderservice.controller;

import com.medha.configmanagementservice.orderservice.config.OrderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies /api/orders/config-info reflects whatever values are currently bound
 * onto the @RefreshScope OrderProperties bean — the same mechanism that lets
 * this endpoint show DIFFERENT output after a real POST /actuator/refresh
 * against a running config-server.
 */
@WebMvcTest(controllers = ConfigInfoController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "product-service.base-url=http://localhost:9999",
        "info.config-source=test-source"
})
class ConfigInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProperties orderProperties;

    @Test
    void currentConfig_reflectsBoundProperties() throws Exception {
        when(orderProperties.getWelcomeMessage()).thenReturn("hello from test");
        when(orderProperties.getMaxQuantityPerOrder()).thenReturn(7);
        when(orderProperties.isDiscountEnabled()).thenReturn(true);
        when(orderProperties.getDiscountPercentage()).thenReturn(15);

        mockMvc.perform(get("/api/orders/config-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcomeMessage").value("hello from test"))
                .andExpect(jsonPath("$.maxQuantityPerOrder").value(7))
                .andExpect(jsonPath("$.discountEnabled").value(true))
                .andExpect(jsonPath("$.discountPercentage").value(15))
                .andExpect(jsonPath("$.productServiceBaseUrl").value("http://localhost:9999"))
                .andExpect(jsonPath("$.configSource").value("test-source"));
    }
}
