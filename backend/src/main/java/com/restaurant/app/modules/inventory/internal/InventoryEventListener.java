package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.exception.DomainException;
import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
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
        try {
            kardexService.processOrderDeduction(event);
            log.info("Deducción de Kardex completada exitosamente para el ticket {}", event.ticketNumber());
        } catch (DomainException ex) {
            log.warn("Deducción de Kardex para el ticket {} no aplicada por regla de inventario/stock: {}",
                event.ticketNumber(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Fallo inesperado al procesar deducción de Kardex para el ticket {}: {}",
                event.ticketNumber(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @ApplicationModuleListener
    void on(OrderCancelledEvent event) {
        log.info("OrderCancelledEvent recibido en InventoryEventListener para el ticket {}. Procesando cancelación en Kardex...",
            event.ticketNumber());
        try {
            kardexService.processOrderCancellation(event);
            log.info("Tratamiento de cancelación completado exitosamente para el ticket {}", event.ticketNumber());
        } catch (DomainException ex) {
            log.warn("Cancelación para el ticket {} no aplicada en Kardex: {}",
                event.ticketNumber(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Fallo inesperado al procesar cancelación para el ticket {}: {}",
                event.ticketNumber(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
