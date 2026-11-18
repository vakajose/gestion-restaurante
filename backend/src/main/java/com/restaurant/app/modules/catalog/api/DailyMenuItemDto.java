package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record DailyMenuItemDto(
    UUID id,
    UUID dishId,
    String dishName,
    String dishCode,
    CourseType courseType,
    int suggestedQuota,
    BigDecimal overridePrice,
    boolean isMandatoryInPackage,
    int sortOrder,
    boolean allowIndividualSale,
    boolean isUnlimited
) {
    public DailyMenuItemDto(
        UUID id,
        UUID dishId,
        String dishName,
        String dishCode,
        CourseType courseType,
        int suggestedQuota,
        BigDecimal overridePrice,
        boolean isMandatoryInPackage,
        int sortOrder
    ) {
        this(id, dishId, dishName, dishCode, courseType, suggestedQuota, overridePrice, isMandatoryInPackage, sortOrder, true, false);
    }
}
