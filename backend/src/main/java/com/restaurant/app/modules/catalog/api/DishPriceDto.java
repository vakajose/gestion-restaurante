package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record DishPriceDto(
    UUID dishId,
    String code,
    String name,
    String description,
    UUID categoryId,
    String categoryName,
    BigDecimal basePrice,
    BigDecimal effectivePrice,
    boolean hasOverride,
    boolean isAvailable
) {
}
