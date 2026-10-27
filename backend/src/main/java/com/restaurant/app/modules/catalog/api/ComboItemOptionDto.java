package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ComboItemOptionDto(
    UUID id,
    UUID dishId,
    String dishName,
    BigDecimal priceAdjustment,
    boolean isDefault
) {
}
