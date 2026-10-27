package com.restaurant.app.modules.catalog.api;

import java.util.List;
import java.util.UUID;

public record ModifierGroupDto(
    UUID id,
    String name,
    int minSelectable,
    int maxSelectable,
    boolean isRequired,
    List<ModifierDto> modifiers
) {
    public ModifierGroupDto {
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
    }
}
