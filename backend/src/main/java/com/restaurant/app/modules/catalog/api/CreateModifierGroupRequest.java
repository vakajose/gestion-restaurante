package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateModifierGroupRequest(
    @NotBlank(message = "El nombre del grupo es obligatorio")
    String name,
    @Min(value = 0, message = "minSelectable no puede ser negativo")
    int minSelectable,
    @Min(value = 1, message = "maxSelectable debe ser al menos 1")
    int maxSelectable,
    boolean isRequired,
    List<@Valid CreateModifierRequest> modifiers
) {
    public CreateModifierGroupRequest {
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
    }
}
