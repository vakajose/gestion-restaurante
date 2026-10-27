package com.restaurant.app.modules.pos.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectedModifierRequest(
    UUID modifierId,
    @NotNull(message = "El nombre del modificador es obligatorio")
    String name,
    BigDecimal unitPrice,
    UUID ingredientId,
    BigDecimal quantityConsumed
) {
    public SelectedModifierRequest {
        unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
    }
}
