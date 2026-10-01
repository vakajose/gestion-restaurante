package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DishDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID clonedFromId,
    UUID categoryId,
    String categoryName,
    String code,
    String name,
    String description,
    BigDecimal salePrice,
    boolean isActive,
    Instant createdAt,
    Long version
) {
}
