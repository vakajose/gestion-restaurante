package com.restaurant.app.modules.inventory.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record KardexMovementDto(
    UUID id,
    UUID ingredientId,
    String ingredientName,
    String unitOfMeasure,
    String movementType,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal totalCost,
    BigDecimal balanceQuantity,
    UUID referenceId,
    Instant movementDate,
    UUID createdBy
) {
}
