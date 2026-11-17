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
@Table(name = "order_item_selections")
class OrderItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "slot_label", nullable = false, length = 100)
    private String slotLabel;

    @Column(name = "selected_dish_id", nullable = false)
    private UUID selectedDishId;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "price_adjustment", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    OrderItemSelection() {
    }

    OrderItemSelection(String slotLabel, UUID selectedDishId, int quantity, BigDecimal priceAdjustment) {
        this.slotLabel = slotLabel;
        this.selectedDishId = selectedDishId;
        this.quantity = quantity > 0 ? quantity : 1;
        this.priceAdjustment = priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public String getSlotLabel() {
        return slotLabel;
    }

    public UUID getSelectedDishId() {
        return selectedDishId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }
}
