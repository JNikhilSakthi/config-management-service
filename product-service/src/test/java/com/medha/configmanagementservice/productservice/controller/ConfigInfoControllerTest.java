package com.medha.configmanagementservice.productservice.controller;

import com.medha.configmanagementservice.productservice.config.ProductProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfigInfoController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "info.config-source=test-source"
})
class ConfigInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductProperties productProperties;

    @Test
    void currentConfig_reflectsBoundProperties() throws Exception {
        when(productProperties.getWelcomeMessage()).thenReturn("hello from test");
        when(productProperties.getLowStockThreshold()).thenReturn(8);
        when(productProperties.getPriceCurrency()).thenReturn("USD");

        mockMvc.perform(get("/api/products/config-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.welcomeMessage").value("hello from test"))
                .andExpect(jsonPath("$.lowStockThreshold").value(8))
                .andExpect(jsonPath("$.priceCurrency").value("USD"))
                .andExpect(jsonPath("$.configSource").value("test-source"));
    }
}
