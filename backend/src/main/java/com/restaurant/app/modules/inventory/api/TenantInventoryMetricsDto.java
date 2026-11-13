package com.restaurant.app.modules.inventory.api;

import java.math.BigDecimal;

/**
 * Métricas agregadas del tamaño y valorización del inventario de un inquilino.
 */
public record TenantInventoryMetricsDto(
    long stockRecordsCount,
    long distinctIngredientsCount,
    BigDecimal totalUnitsOnHand,
    BigDecimal totalValuation
) {
}
