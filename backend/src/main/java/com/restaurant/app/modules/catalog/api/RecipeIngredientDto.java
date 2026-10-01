package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientDto(
    UUID ingredientId,
    String ingredientName,
    String unitOfMeasure,
    BigDecimal quantity
) {
}
