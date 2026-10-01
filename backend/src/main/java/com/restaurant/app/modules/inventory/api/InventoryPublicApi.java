package com.restaurant.app.modules.inventory.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryPublicApi {

    Optional<BigDecimal> getAvailableStock(UUID tenantId, UUID branchId, UUID ingredientId);

    List<StockBalanceDto> getBranchStockSummary(UUID tenantId, UUID branchId);

    void registerInitialStock(UUID tenantId, UUID branchId, InitialStockRequest request);

    StockBalanceDto adjustStock(UUID tenantId, UUID branchId, StockAdjustmentRequest request, UUID userId);
}
