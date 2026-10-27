package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.List;
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
    boolean isAvailable,
    List<ModifierGroupDto> modifierGroups
) {
    public DishPriceDto(UUID dishId, String code, String name, String description,
                        UUID categoryId, String categoryName, BigDecimal basePrice,
                        BigDecimal effectivePrice, boolean hasOverride, boolean isAvailable) {
        this(dishId, code, name, description, categoryId, categoryName, basePrice, effectivePrice, hasOverride, isAvailable, List.of());
    }

    public DishPriceDto {
        modifierGroups = modifierGroups != null ? List.copyOf(modifierGroups) : List.of();
    }
}
