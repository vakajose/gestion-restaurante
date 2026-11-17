package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDailyMenuItemRequest(
    @NotNull(message = "El dishId es obligatorio")
    UUID dishId,

    @NotNull(message = "El courseType es obligatorio")
    CourseType courseType,

    @Min(value = 0, message = "El cupo sugerido no puede ser negativo")
    int suggestedQuota,

    BigDecimal overridePrice,

    boolean isMandatoryInPackage,

    int sortOrder,

    Boolean allowIndividualSale,

    Boolean isUnlimited
) {
    public CreateDailyMenuItemRequest(
        UUID dishId,
        CourseType courseType,
        int suggestedQuota,
        BigDecimal overridePrice,
        boolean isMandatoryInPackage,
        int sortOrder
    ) {
        this(dishId, courseType, suggestedQuota, overridePrice, isMandatoryInPackage, sortOrder, true, false);
    }

    public boolean resolvedAllowIndividualSale() {
        return allowIndividualSale == null || Boolean.TRUE.equals(allowIndividualSale);
    }

    public boolean resolvedIsUnlimited() {
        return Boolean.TRUE.equals(isUnlimited);
    }
}
