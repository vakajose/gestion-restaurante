package com.restaurant.app.modules.catalog.api;

import java.util.List;
import java.util.UUID;

public record ComboItemDto(
    UUID id,
    String name,
    String selectionMode,
    boolean isRequired,
    UUID defaultDishId,
    String defaultDishName,
    int quantity,
    int sortOrder,
    List<ComboItemOptionDto> options
) {
    public ComboItemDto {
        options = options != null ? List.copyOf(options) : List.of();
    }
}
