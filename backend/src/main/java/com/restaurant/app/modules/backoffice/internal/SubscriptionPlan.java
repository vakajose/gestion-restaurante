package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.QuotaResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "BOB";

    @Column(name = "max_branches")
    private Integer maxBranches;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_dishes")
    private Integer maxDishes;

    @Column(name = "max_inventory_items")
    private Integer maxInventoryItems;

    @Column(name = "max_monthly_orders")
    private Integer maxMonthlyOrders;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    protected SubscriptionPlan() {
    }

    SubscriptionPlan(
        String code,
        String name,
        String description,
        BillingCycle billingCycle,
        int durationDays,
        BigDecimal price,
        String currency,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
        this.code = code.trim().toUpperCase();
        updateDetails(name, description, billingCycle, durationDays, price, currency,
            maxBranches, maxUsers, maxDishes, maxInventoryItems, maxMonthlyOrders);
        this.isActive = true;
    }

    void updateDetails(
        String name,
        String description,
        BillingCycle billingCycle,
        int durationDays,
        BigDecimal price,
        String currency,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
        this.name = name.trim();
        this.description = description;
        this.billingCycle = billingCycle != null ? billingCycle : BillingCycle.MONTHLY;
        this.durationDays = durationDays;
        this.price = price != null ? price : BigDecimal.ZERO;
        this.currency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "BOB";
        this.maxBranches = maxBranches;
        this.maxUsers = maxUsers;
        this.maxDishes = maxDishes;
        this.maxInventoryItems = maxInventoryItems;
        this.maxMonthlyOrders = maxMonthlyOrders;
    }

    Integer limitFor(QuotaResource resource) {
        return switch (resource) {
            case BRANCHES -> maxBranches;
            case USERS -> maxUsers;
            case DISHES -> maxDishes;
            case INVENTORY_ITEMS -> maxInventoryItems;
            case MONTHLY_ORDERS -> maxMonthlyOrders;
        };
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    BillingCycle getBillingCycle() {
        return billingCycle;
    }

    int getDurationDays() {
        return durationDays;
    }

    BigDecimal getPrice() {
        return price;
    }

    String getCurrency() {
        return currency;
    }

    Integer getMaxBranches() {
        return maxBranches;
    }

    Integer getMaxUsers() {
        return maxUsers;
    }

    Integer getMaxDishes() {
        return maxDishes;
    }

    Integer getMaxInventoryItems() {
        return maxInventoryItems;
    }

    Integer getMaxMonthlyOrders() {
        return maxMonthlyOrders;
    }

    boolean isActive() {
        return isActive;
    }

    void setActive(boolean active) {
        isActive = active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
