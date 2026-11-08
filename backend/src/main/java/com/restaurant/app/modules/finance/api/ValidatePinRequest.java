package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ValidatePinRequest(
    UUID branchId,
    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4,6}$", message = "El PIN debe tener entre 4 y 6 dígitos")
    String pin
) {}
