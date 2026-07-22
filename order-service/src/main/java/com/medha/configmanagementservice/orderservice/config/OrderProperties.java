package com.medha.configmanagementservice.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Type-safe binding for the {@code app.order.*} properties that live in
 * config-repo/order-service.yml (and its -docker overlay) on the Config Server —
 * NOT in this module's own application.yml.
 *
 * <p>{@code @RefreshScope} is the key annotation for this whole project: it makes
 * Spring throw away this bean and re-create it (re-binding from a fresh call to
 * config-server) the next time {@code POST /actuator/refresh} is invoked, instead
 * of only ever binding once at startup like a normal singleton bean would.
 * Without {@code @RefreshScope}, editing config-repo and refreshing would have
 * no visible effect on already-injected values.</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.order")
public class OrderProperties {

    /** Human-readable banner proving which profile/label's config is active. */
    private String welcomeMessage;

    /** Business rule: max quantity allowed per single order, centrally tunable. */
    private int maxQuantityPerOrder = 10;

    /** Feature flag: whether the discount is applied when computing totalPrice. */
    private boolean discountEnabled;

    /** Discount percentage (0-100) applied when discountEnabled is true. */
    private int discountPercentage;

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public int getMaxQuantityPerOrder() {
        return maxQuantityPerOrder;
    }

    public void setMaxQuantityPerOrder(int maxQuantityPerOrder) {
        this.maxQuantityPerOrder = maxQuantityPerOrder;
    }

    public boolean isDiscountEnabled() {
        return discountEnabled;
    }

    public void setDiscountEnabled(boolean discountEnabled) {
        this.discountEnabled = discountEnabled;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
}
