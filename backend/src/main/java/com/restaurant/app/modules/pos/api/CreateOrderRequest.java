package com.restaurant.app.modules.pos.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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
    List<@Valid CreateOrderItemRequest> items
) {
    public CreateOrderRequest {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
