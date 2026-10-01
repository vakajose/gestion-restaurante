package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateDishRequest(
    @NotNull(message = "La categoría es obligatoria")
    UUID categoryId,

    UUID branchId,

    @NotBlank(message = "El código del plato es obligatorio")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    String code,

    @NotBlank(message = "El nombre del plato es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String name,

    String description,

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    BigDecimal salePrice,

    Boolean isActive,

    List<@Valid DishRecipeItemRequest> recipeItems
) {
    public boolean resolvedIsActive() {
        return isActive != null ? isActive : true;
    }
}
