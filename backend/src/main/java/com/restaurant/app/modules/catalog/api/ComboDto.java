package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ComboDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID categoryId,
    String code,
    String name,
    String description,
    BigDecimal basePrice,
    boolean isActive,
    List<ComboItemDto> items
) {
    public ComboDto {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
