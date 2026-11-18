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
    List<DailyMenuItemDto> items,
    List<UUID> comboIds
) {
    public DailyMenuDto(
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
    ) {
        this(id, tenantId, branchId, name, description, dayOfWeek, priceMode, packagePrice, isTemplate, isActive, createdAt, updatedAt, version, items, List.of());
    }

    public DailyMenuDto {
        items = items != null ? List.copyOf(items) : List.of();
        comboIds = comboIds != null ? List.copyOf(comboIds) : List.of();
    }
}
