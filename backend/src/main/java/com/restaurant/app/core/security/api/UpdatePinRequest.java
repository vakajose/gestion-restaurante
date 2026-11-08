package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePinRequest(
    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{4,6}$", message = "El PIN debe contener entre 4 y 6 dígitos numéricos")
    String pin
) {}
