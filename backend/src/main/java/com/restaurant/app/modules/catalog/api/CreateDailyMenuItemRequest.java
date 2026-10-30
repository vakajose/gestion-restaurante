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

    int sortOrder
) {}
