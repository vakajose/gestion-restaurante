package com.restaurant.app.modules.inventory.internal;

import java.math.BigDecimal;
import java.util.UUID;

record RecipeDeductionItem(
    UUID ingredientId,
    String ingredientName,
    BigDecimal totalQuantityNeeded
) {
}
