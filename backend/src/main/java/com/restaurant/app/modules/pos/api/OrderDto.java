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
    UUID cashierId,
    String cashierName,
    String ticketNumber,
    String orderStatus,
    String paymentMethod,
    BigDecimal totalAmount,
    UUID clientTransactionId,
    String notes,
    Instant createdAt,
    Instant closedAt,
    List<OrderItemDto> items,
    String orderType,
    String customerName
) {
    public OrderDto {
        items = items != null ? List.copyOf(items) : List.of();
        orderType = orderType != null ? orderType : "DINE_IN";
    }

    public OrderDto(
        UUID id, UUID tenantId, UUID branchId, UUID cashShiftId,
        String ticketNumber, String orderStatus, String paymentMethod,
        BigDecimal totalAmount, UUID clientTransactionId, String notes,
        Instant createdAt, Instant closedAt, List<OrderItemDto> items
    ) {
        this(id, tenantId, branchId, cashShiftId, null, null, ticketNumber, orderStatus, paymentMethod, totalAmount, clientTransactionId, notes, createdAt, closedAt, items, "DINE_IN", null);
    }

    public OrderDto(
        UUID id, UUID tenantId, UUID branchId, UUID cashShiftId, UUID cashierId,
        String ticketNumber, String orderStatus, String paymentMethod,
        BigDecimal totalAmount, UUID clientTransactionId, String notes,
        Instant createdAt, Instant closedAt, List<OrderItemDto> items
    ) {
        this(id, tenantId, branchId, cashShiftId, cashierId, null, ticketNumber, orderStatus, paymentMethod, totalAmount, clientTransactionId, notes, createdAt, closedAt, items, "DINE_IN", null);
    }
}
