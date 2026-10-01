package com.restaurant.app.modules.inventory.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StockAdjustmentRequest(
    @NotNull UUID ingredientId,
    @NotNull BigDecimal newQuantity,
    BigDecimal unitCost,
    String reason
) {
}
