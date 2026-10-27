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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "modifiers")
public class Modifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ModifierGroup modifierGroup;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "additional_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Column(name = "ingredient_id")
    private UUID ingredientId;

    @Column(name = "quantity_consumed", precision = 12, scale = 4)
    private BigDecimal quantityConsumed = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public Modifier() {
    }

    public Modifier(UUID tenantId, String name, BigDecimal additionalPrice,
                    UUID ingredientId, BigDecimal quantityConsumed, int sortOrder) {
        this.tenantId = tenantId;
        this.name = name;
        this.additionalPrice = additionalPrice != null ? additionalPrice : BigDecimal.ZERO;
        this.ingredientId = ingredientId;
        this.quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public ModifierGroup getModifierGroup() {
        return modifierGroup;
    }

    public void setModifierGroup(ModifierGroup modifierGroup) {
        this.modifierGroup = modifierGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public void setAdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice;
    }

    public UUID getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(UUID ingredientId) {
        this.ingredientId = ingredientId;
    }

    public BigDecimal getQuantityConsumed() {
        return quantityConsumed;
    }

    public void setQuantityConsumed(BigDecimal quantityConsumed) {
        this.quantityConsumed = quantityConsumed;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
