package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ModifierDto(
    UUID id,
    String name,
    BigDecimal additionalPrice,
    UUID ingredientId,
    BigDecimal quantityConsumed,
    boolean isActive,
    int sortOrder
) {
}
