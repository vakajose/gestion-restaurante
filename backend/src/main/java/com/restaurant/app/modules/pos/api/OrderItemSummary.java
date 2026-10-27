package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemSummary(
    UUID dishId,
    String dishName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    UUID comboId,
    List<UUID> componentDishIds
) {
    public OrderItemSummary {
        componentDishIds = componentDishIds != null ? List.copyOf(componentDishIds) : List.of();
    }

    public OrderItemSummary(
        UUID dishId,
        String dishName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {
        this(dishId, dishName, quantity, unitPrice, subtotal, null, List.of());
    }
}
