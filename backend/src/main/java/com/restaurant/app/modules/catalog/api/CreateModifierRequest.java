package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateModifierRequest(
    @NotBlank(message = "El nombre del modificador es obligatorio")
    String name,
    BigDecimal additionalPrice,
    UUID ingredientId,
    BigDecimal quantityConsumed,
    Integer sortOrder
) {
    public CreateModifierRequest {
        additionalPrice = additionalPrice != null ? additionalPrice : BigDecimal.ZERO;
        quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
        sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
