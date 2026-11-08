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
    String customerName,
    String customerPhone,
    String deliveryAddress,
    BigDecimal deliveryFee,
    String deliveryNotes,
    String cancellationReason,
    String cancellationNotes,
    Instant cancelledAt,
    UUID cancelledBy,
    UUID cancellationAuthorizedBy,
    String refundMethod,
    String wastePolicyApplied
) {
    public OrderDto {
        items = items != null ? List.copyOf(items) : List.of();
        orderType = (orderType != null && !orderType.isBlank()) ? orderType : "DINE_IN";
        deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
    }

    public OrderDto(
        UUID id, UUID tenantId, UUID branchId, UUID cashShiftId,
        UUID cashierId, String cashierName, String ticketNumber, String orderStatus,
        String paymentMethod, BigDecimal totalAmount, UUID clientTransactionId, String notes,
        Instant createdAt, Instant closedAt, List<OrderItemDto> items, String orderType,
        String customerName
    ) {
        this(id, tenantId, branchId, cashShiftId, cashierId, cashierName, ticketNumber, orderStatus,
            paymentMethod, totalAmount, clientTransactionId, notes, createdAt, closedAt, items,
            orderType, customerName, null, null, BigDecimal.ZERO, null, null, null, null, null, null, null, null);
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
