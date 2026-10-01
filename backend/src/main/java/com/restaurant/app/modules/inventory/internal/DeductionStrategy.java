package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;

import java.util.List;
import java.util.UUID;

interface DeductionStrategy {
    void executeDeduction(UUID tenantId, UUID branchId, OrderPaidEvent event, List<RecipeDeductionItem> items);
}
