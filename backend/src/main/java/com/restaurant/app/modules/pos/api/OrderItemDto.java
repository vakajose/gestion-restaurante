package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
    UUID id,
    UUID dishId,
    String dishName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    String notes,
    BigDecimal discountAmount,
    UUID comboId
) {
    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, BigDecimal.ZERO, null);
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, null);
    }
}
