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
    String notes
) {
}
