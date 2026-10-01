package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPaidEvent(
    UUID eventId,
    UUID tenantId,
    UUID branchId,
    UUID orderId,
    UUID clientTransactionId,
    String ticketNumber,
    List<OrderItemSummary> items,
    BigDecimal totalAmount,
    String paymentMethod,
    Instant paidAt,
    UUID cashShiftId
) {
    public OrderPaidEvent {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public OrderPaidEvent(
        UUID eventId,
        UUID tenantId,
        UUID branchId,
        UUID orderId,
        UUID clientTransactionId,
        String ticketNumber,
        List<OrderItemSummary> items,
        BigDecimal totalAmount,
        String paymentMethod,
        Instant paidAt
    ) {
        this(eventId, tenantId, branchId, orderId, clientTransactionId, ticketNumber, items, totalAmount, paymentMethod, paidAt, null);
    }
}
