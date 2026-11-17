package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftMenuItemDto(
    UUID id,
    UUID shiftMenuAssignmentId,
    UUID dishId,
    String dishName,
    String dishCode,
    CourseType courseType,
    int initialQuota,
    int currentQuota,
    int soldCount,
    BigDecimal effectivePrice,
    boolean isSoldOut,
    Long version,
    boolean allowIndividualSale,
    boolean isUnlimited
) {
    public ShiftMenuItemDto(
        UUID id,
        UUID shiftMenuAssignmentId,
        UUID dishId,
        String dishName,
        String dishCode,
        CourseType courseType,
        int initialQuota,
        int currentQuota,
        int soldCount,
        BigDecimal effectivePrice,
        boolean isSoldOut,
        Long version
    ) {
        this(id, shiftMenuAssignmentId, dishId, dishName, dishCode, courseType, initialQuota, currentQuota, soldCount, effectivePrice, isSoldOut, version, true, false);
    }
}
