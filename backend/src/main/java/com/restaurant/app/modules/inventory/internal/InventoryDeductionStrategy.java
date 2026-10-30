package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.pos.api.OrderItemSummary;

import java.util.UUID;

interface InventoryDeductionStrategy {

    boolean supports(InventoryStrategy strategy);

    void deduct(UUID tenantId, UUID branchId, UUID orderId, OrderItemSummary item, boolean allowNegative);
}
