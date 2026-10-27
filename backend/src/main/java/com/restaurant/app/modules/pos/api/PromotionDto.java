package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record PromotionDto(
    UUID id,
    UUID tenantId,
    String name,
    String description,
    String discountType,
    BigDecimal discountValue,
    String scope,
    UUID targetCategoryId,
    UUID targetDishId,
    String applicationMode,
    String daysOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal minOrderAmount,
    boolean isActive,
    Set<UUID> branchIds
) {
    public PromotionDto {
        branchIds = branchIds != null ? Set.copyOf(branchIds) : Set.of();
    }
}
