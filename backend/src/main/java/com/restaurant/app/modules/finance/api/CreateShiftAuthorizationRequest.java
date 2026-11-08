package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateShiftAuthorizationRequest(
    @NotNull(message = "La sucursal es obligatoria")
    UUID branchId,
    UUID cashShiftId,
    @NotBlank(message = "El tipo de solicitud es obligatorio")
    String requestType,
    UUID referenceId,
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    BigDecimal amount,
    String justification
) {}
