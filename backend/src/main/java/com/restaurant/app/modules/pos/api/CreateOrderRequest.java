package com.restaurant.app.modules.pos.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    UUID clientTransactionId,

    @NotNull(message = "El branchId es obligatorio")
    UUID branchId,

    UUID cashShiftId,

    String paymentMethod,

    String notes,

    @NotEmpty(message = "La orden debe contener al menos un ítem")
    List<@Valid CreateOrderItemRequest> items,

    UUID appliedPromotionId,

    String orderType,

    String customerName,

    String customerPhone,

    String deliveryAddress,

    BigDecimal deliveryFee,

    String deliveryNotes
) {
    public CreateOrderRequest(UUID clientTransactionId, UUID branchId, UUID cashShiftId,
                              String paymentMethod, String notes, List<CreateOrderItemRequest> items) {
        this(clientTransactionId, branchId, cashShiftId, paymentMethod, notes, items, null, "DINE_IN", null, null, null, BigDecimal.ZERO, null);
    }

    public CreateOrderRequest(UUID clientTransactionId, UUID branchId, UUID cashShiftId,
                              String paymentMethod, String notes, List<CreateOrderItemRequest> items,
                              UUID appliedPromotionId) {
        this(clientTransactionId, branchId, cashShiftId, paymentMethod, notes, items, appliedPromotionId, "DINE_IN", null, null, null, BigDecimal.ZERO, null);
    }

    public CreateOrderRequest(UUID clientTransactionId, UUID branchId, UUID cashShiftId,
                              String paymentMethod, String notes, List<CreateOrderItemRequest> items,
                              UUID appliedPromotionId, String orderType, String customerName) {
        this(clientTransactionId, branchId, cashShiftId, paymentMethod, notes, items, appliedPromotionId, orderType, customerName, null, null, BigDecimal.ZERO, null);
    }

    public CreateOrderRequest {
        items = items != null ? List.copyOf(items) : List.of();
        orderType = (orderType != null && !orderType.isBlank()) ? orderType : "DINE_IN";
        deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        if (deliveryFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.restaurant.app.core.common.exception.ValidationException("El costo de envío no puede ser negativo");
        }
    }
}
