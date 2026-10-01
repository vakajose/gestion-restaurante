package com.restaurant.app.modules.pos.api;

import java.util.UUID;

public interface PosPublicApi {

    OrderDto placeOrder(CreateOrderRequest request);

    /**
     * Retorna el resumen de ventas agregadas (efectivo, tarjeta, otros métodos) para un turno de caja.
     */
    ShiftSalesSummaryDto getShiftSalesSummary(UUID tenantId, UUID cashShiftId);
}
