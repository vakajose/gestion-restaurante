package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.util.List;
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
    UUID comboId,
    List<OrderItemModifierDto> modifiers
) {
    public OrderItemDto {
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, BigDecimal.ZERO, null, List.of());
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, null, List.of());
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount, UUID comboId) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, comboId, List.of());
    }
}
