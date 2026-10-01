package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branch_dishes")
class BranchDish {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "dish_id", nullable = false)
    private UUID dishId;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    BranchDish() {
    }

    BranchDish(UUID tenantId, UUID branchId, UUID dishId, boolean isAvailable, BigDecimal priceOverride) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.dishId = dishId;
        this.isAvailable = isAvailable;
        this.priceOverride = priceOverride;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getDishId() {
        return dishId;
    }

    public void setDishId(UUID dishId) {
        this.dishId = dishId;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
