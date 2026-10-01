package com.restaurant.app.modules.catalog.api;

import java.util.List;
import java.util.UUID;

public record DishRecipeDto(
    UUID dishId,
    String dishName,
    List<RecipeIngredientDto> items
) {
}
