package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfigureServiceSessionRequest(
    @NotNull(message = "El branchId es obligatorio")
    UUID branchId,
    String menuMode,
    UUID dailyMenuId,
    Map<UUID, Integer> customQuotas,
    List<UUID> unlimitedDishIds,
    String notes
) {
    public ConfigureServiceSessionRequest {
        customQuotas = customQuotas != null ? Map.copyOf(customQuotas) : Map.of();
        unlimitedDishIds = unlimitedDishIds != null ? List.copyOf(unlimitedDishIds) : List.of();
    }

    public String resolvedMenuMode() {
        return (menuMode != null && !menuMode.isBlank()) ? menuMode.trim().toUpperCase() : "FULL_CATALOG";
    }
}
