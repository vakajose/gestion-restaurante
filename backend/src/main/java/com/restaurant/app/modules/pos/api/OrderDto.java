package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID cashShiftId,
    String ticketNumber,
    String orderStatus,
    String paymentMethod,
    BigDecimal totalAmount,
    UUID clientTransactionId,
    String notes,
    Instant createdAt,
    Instant closedAt,
    List<OrderItemDto> items
) {
    public OrderDto {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
