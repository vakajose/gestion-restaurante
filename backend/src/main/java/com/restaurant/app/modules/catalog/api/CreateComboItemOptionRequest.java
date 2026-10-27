package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateComboItemOptionRequest(
    @NotNull(message = "El ID del plato es obligatorio") UUID dishId,
    BigDecimal priceAdjustment,
    boolean isDefault
) {
}
