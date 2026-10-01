package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final KardexService kardexService;

    InventoryEventListener(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        log.info("OrderPaidEvent recibido en InventoryEventListener para el ticket {}. Iniciando deducción de Kardex...",
            event.ticketNumber());
        kardexService.processOrderDeduction(event);
        log.info("Deducción de Kardex completada exitosamente para el ticket {}", event.ticketNumber());
    }
}
