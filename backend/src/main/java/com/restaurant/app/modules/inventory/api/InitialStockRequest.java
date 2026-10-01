package com.restaurant.app.modules.inventory.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InitialStockRequest(
    @NotNull UUID ingredientId,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal unitCost
) {
}
