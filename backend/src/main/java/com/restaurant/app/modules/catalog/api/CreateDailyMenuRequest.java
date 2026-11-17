package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateDailyMenuRequest(
    UUID branchId,

    @NotBlank(message = "El nombre del menú es obligatorio")
    String name,

    String description,

    String dayOfWeek,

    @NotNull(message = "El modo de precio es obligatorio")
    PriceMode priceMode,

    BigDecimal packagePrice,

    boolean isTemplate,

    @Valid
    List<CreateDailyMenuItemRequest> items,

    List<UUID> comboIds
) {
    public CreateDailyMenuRequest(
        UUID branchId,
        String name,
        String description,
        String dayOfWeek,
        PriceMode priceMode,
        BigDecimal packagePrice,
        boolean isTemplate,
        List<CreateDailyMenuItemRequest> items
    ) {
        this(branchId, name, description, dayOfWeek, priceMode, packagePrice, isTemplate, items, List.of());
    }

    public CreateDailyMenuRequest {
        items = items != null ? List.copyOf(items) : List.of();
        comboIds = comboIds != null ? List.copyOf(comboIds) : List.of();
    }
}
