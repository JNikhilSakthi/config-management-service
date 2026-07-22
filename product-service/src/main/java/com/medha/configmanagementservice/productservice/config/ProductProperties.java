package com.medha.configmanagementservice.productservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Type-safe binding for {@code app.product.*}, sourced from
 * config-repo/product-service.yml (and its -docker overlay) on the Config Server.
 * See order-service's OrderProperties for a full explanation of @RefreshScope.
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.product")
public class ProductProperties {

    private String welcomeMessage;
    private int lowStockThreshold = 5;
    private String priceCurrency = "INR";

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }
}
