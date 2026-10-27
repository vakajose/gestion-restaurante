package com.restaurant.app.modules.inventory.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record InitialStockRequest(
    @NotNull(message = "El ID del insumo es obligatorio")
    UUID ingredientId,

    @NotNull(message = "La cantidad inicial es obligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad inicial debe ser mayor a 0")
    BigDecimal quantity,

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo unitario no puede ser negativo")
    BigDecimal unitCost
) {
}
