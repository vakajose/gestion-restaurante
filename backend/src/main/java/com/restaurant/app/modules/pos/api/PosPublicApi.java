package com.restaurant.app.modules.pos.api;

import java.util.UUID;

public interface PosPublicApi {

    OrderDto placeOrder(CreateOrderRequest request);

    /**
     * Anula una orden pagada registrando motivos, canal de reembolso, política de merma y autorización (ADR-0019 / PB-031).
     */
    OrderDto cancelOrder(UUID tenantId, UUID orderId, CancelOrderRequest request, UUID currentUserId);

    /**
     * Retorna el resumen de ventas agregadas (efectivo, tarjeta, otros métodos) para un turno de caja.
     */
    ShiftSalesSummaryDto getShiftSalesSummary(UUID tenantId, UUID cashShiftId);
}
