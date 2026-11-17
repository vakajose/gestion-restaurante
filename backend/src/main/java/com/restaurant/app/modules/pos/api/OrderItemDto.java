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
    List<OrderItemModifierDto> modifiers,
    UUID dailyMenuId,
    List<OrderItemSelectionDto> selections
) {
    public OrderItemDto {
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
        selections = selections != null ? List.copyOf(selections) : List.of();
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, BigDecimal.ZERO, null, List.of(), null, List.of());
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, null, List.of(), null, List.of());
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount, UUID comboId) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, comboId, List.of(), null, List.of());
    }

    public OrderItemDto(UUID id, UUID dishId, String dishName, int quantity,
                        BigDecimal unitPrice, BigDecimal subtotal, String notes, BigDecimal discountAmount, UUID comboId, List<OrderItemModifierDto> modifiers) {
        this(id, dishId, dishName, quantity, unitPrice, subtotal, notes, discountAmount, comboId, modifiers, null, List.of());
    }
}
