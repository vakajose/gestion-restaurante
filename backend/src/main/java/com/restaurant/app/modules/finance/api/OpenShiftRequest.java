package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenShiftRequest(
    @NotNull(message = "El branchId es obligatorio")
    UUID branchId,

    @NotNull(message = "El monto inicial de caja es obligatorio")
    @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
    BigDecimal initialCash
) {}
