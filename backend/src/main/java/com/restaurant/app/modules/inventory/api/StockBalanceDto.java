package com.restaurant.app.modules.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockBalanceDto(
    UUID id,
    UUID ingredientId,
    String ingredientName,
    String unitOfMeasure,
    BigDecimal currentQuantity,
    BigDecimal averageUnitCost,
    BigDecimal totalValue,
    BigDecimal minStockAlert,
    boolean isLowStock,
    Instant lastUpdated
) {
}
