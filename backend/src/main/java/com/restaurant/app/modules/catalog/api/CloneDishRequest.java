package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CloneDishRequest(
    UUID targetBranchId,

    @NotBlank(message = "El nuevo código es obligatorio")
    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    String newCode,

    @NotBlank(message = "El nuevo nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String newName,

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    BigDecimal newSalePrice,

    List<@Valid DishRecipeItemRequest> customRecipe
) {
}
