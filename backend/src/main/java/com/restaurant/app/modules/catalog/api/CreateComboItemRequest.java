package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateComboItemRequest(
    @NotBlank(message = "El nombre del componente es obligatorio") String name,
    String selectionMode,
    boolean isRequired,
    UUID defaultDishId,
    @Min(value = 1, message = "La cantidad debe ser al menos 1") int quantity,
    int sortOrder,
    List<@Valid CreateComboItemOptionRequest> options
) {
    public CreateComboItemRequest {
        options = options != null ? List.copyOf(options) : List.of();
    }
}
