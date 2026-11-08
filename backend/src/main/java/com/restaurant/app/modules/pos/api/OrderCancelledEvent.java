package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID eventId,
    UUID tenantId,
    UUID branchId,
    UUID orderId,
    UUID cashShiftId,
    String ticketNumber,
    BigDecimal totalAmount,
    String refundMethod,
    boolean wasPrepared,
    String wastePolicyApplied,
    List<OrderItemSummary> items,
    Instant cancelledAt,
    UUID cancelledBy,
    UUID authorizedBy
) {
    public OrderCancelledEvent {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
