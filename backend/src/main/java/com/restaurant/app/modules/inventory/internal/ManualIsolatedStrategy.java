package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class ManualIsolatedStrategy implements DeductionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ManualIsolatedStrategy.class);

    @Override
    public void executeDeduction(UUID tenantId, UUID branchId, OrderPaidEvent event, List<RecipeDeductionItem> items) {
        log.info("Estrategia MANUAL_ISOLATED activa para tenant {}. Se omite descuento automático de {} ingredientes para el ticket {}",
            tenantId, items.size(), event.ticketNumber());
    }
}
