package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DishRecipeItemRequest(
    @NotNull(message = "El ID del ingrediente es obligatorio")
    UUID ingredientId,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a 0")
    BigDecimal quantity
) {
}
