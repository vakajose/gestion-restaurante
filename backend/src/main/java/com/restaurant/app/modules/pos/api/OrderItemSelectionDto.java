package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSelectionDto(
    UUID id,
    String slotLabel,
    UUID selectedDishId,
    String selectedDishName,
    int quantity,
    BigDecimal priceAdjustment
) {}
