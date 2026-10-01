package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IngredientDto(
    UUID id,
    UUID tenantId,
    String name,
    String unitOfMeasure,
    BigDecimal minStockAlert,
    Instant createdAt
) {
}
