package com.restaurant.app.modules.pos.internal;

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
@Table(name = "order_item_modifiers")
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "modifier_id")
    private UUID modifierId;

    @Column(name = "modifier_name", nullable = false, length = 100)
    private String modifierName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "ingredient_id")
    private UUID ingredientId;

    @Column(name = "quantity_consumed", precision = 12, scale = 4)
    private BigDecimal quantityConsumed = BigDecimal.ZERO;

    public OrderItemModifier() {
    }

    public OrderItemModifier(UUID tenantId, UUID branchId, UUID modifierId, String modifierName,
                             BigDecimal unitPrice, UUID ingredientId, BigDecimal quantityConsumed) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.modifierId = modifierId;
        this.modifierName = modifierName;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.ingredientId = ingredientId;
        this.quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
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

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public UUID getModifierId() {
        return modifierId;
    }

    public void setModifierId(UUID modifierId) {
        this.modifierId = modifierId;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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
}
