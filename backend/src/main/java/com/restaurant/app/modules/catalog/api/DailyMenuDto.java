package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DailyMenuDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    String name,
    String description,
    String dayOfWeek,
    PriceMode priceMode,
    BigDecimal packagePrice,
    boolean isTemplate,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    Long version,
    List<DailyMenuItemDto> items
) {}
