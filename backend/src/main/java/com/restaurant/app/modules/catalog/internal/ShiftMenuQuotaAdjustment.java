package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shift_menu_quota_adjustments")
class ShiftMenuQuotaAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_menu_item_id", nullable = false)
    private ShiftMenuItem shiftMenuItem;

    @Column(name = "adjusted_by", nullable = false)
    private UUID adjustedBy;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "reason", nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    ShiftMenuQuotaAdjustment() {
    }

    ShiftMenuQuotaAdjustment(ShiftMenuItem shiftMenuItem, UUID adjustedBy, int quantityDelta, String reason) {
        this.shiftMenuItem = shiftMenuItem;
        this.adjustedBy = adjustedBy;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ShiftMenuItem getShiftMenuItem() {
        return shiftMenuItem;
    }

    public void setShiftMenuItem(ShiftMenuItem shiftMenuItem) {
        this.shiftMenuItem = shiftMenuItem;
    }

    public UUID getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(UUID adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(int quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
