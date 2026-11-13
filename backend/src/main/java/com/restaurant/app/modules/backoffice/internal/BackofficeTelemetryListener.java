package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class BackofficeTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(BackofficeTelemetryListener.class);

    private final BackofficeTelemetryService telemetryService;

    BackofficeTelemetryListener(BackofficeTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    /**
     * Actualización síncrona ACID del consumo de órdenes en período de gracia para bloqueo inmediato sin race condition.
     */
    @EventListener
    void onOrderPaidGraceTracking(OrderPaidEvent event) {
        if (event != null) {
            telemetryService.incrementGraceOrderIfNeeded(event.tenantId());
        }
    }

    /**
     * Proyección CQRS asíncrona/outbox hacia tenant_daily_telemetry (ADR-0029).
     */
    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        try {
            telemetryService.recordPaidTelemetry(event);
        } catch (Exception ex) {
            log.error("Error proyectando telemetría de pago para orden {}: {}",
                event != null ? event.orderId() : null, ex.getMessage(), ex);
            throw ex;
        }
    }

    @ApplicationModuleListener
    void on(OrderCancelledEvent event) {
        try {
            telemetryService.recordCancelledTelemetry(event);
        } catch (Exception ex) {
            log.error("Error proyectando telemetría de anulación para orden {}: {}",
                event != null ? event.orderId() : null, ex.getMessage(), ex);
            throw ex;
        }
    }
}
