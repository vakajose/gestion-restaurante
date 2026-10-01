package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchOverrideRequest(
    @NotNull(message = "El ID de la sucursal es obligatorio")
    UUID branchId,

    Boolean isAvailable,

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    BigDecimal priceOverride
) {
    public boolean resolvedIsAvailable() {
        return isAvailable != null ? isAvailable : true;
    }
}
