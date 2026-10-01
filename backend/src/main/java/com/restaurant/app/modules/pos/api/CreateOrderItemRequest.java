package com.restaurant.app.modules.pos.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
    @NotNull(message = "El ID del plato es obligatorio")
    UUID dishId,

    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    int quantity,

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio unitario no puede ser negativo")
    BigDecimal unitPrice,

    String notes
) {
}
