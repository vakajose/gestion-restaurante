package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class CommercialNoInventoryStrategy implements InventoryDeductionStrategy {

    private static final Logger log = LoggerFactory.getLogger(CommercialNoInventoryStrategy.class);

    @Override
    public boolean supports(InventoryStrategy strategy) {
        return strategy == InventoryStrategy.COMMERCIAL_NO_INVENTORY;
    }

    @Override
    public void deduct(UUID tenantId, UUID branchId, UUID orderId, OrderItemSummary item, boolean allowNegative) {
        log.debug("Plato comercial libre '{}' ({}). Sin afectación en Kardex ni almacén.", item.dishName(), item.dishId());
    }
}
