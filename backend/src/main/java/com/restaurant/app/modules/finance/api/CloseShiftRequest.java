package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseShiftRequest(
    @NotNull(message = "El monto de efectivo real contado es obligatorio")
    @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo")
    BigDecimal actualCash,

    String notes
) {}
