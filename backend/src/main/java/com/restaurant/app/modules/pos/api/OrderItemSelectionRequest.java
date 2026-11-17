package com.restaurant.app.modules.pos.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSelectionRequest(
    @NotBlank(message = "El nombre del bloque es obligatorio")
    String slotLabel,

    @NotNull(message = "El plato seleccionado es obligatorio")
    UUID selectedDishId,

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    int quantity,

    BigDecimal priceAdjustment
) {
    public OrderItemSelectionRequest(String slotLabel, UUID selectedDishId, int quantity) {
        this(slotLabel, selectedDishId, quantity, BigDecimal.ZERO);
    }
}
