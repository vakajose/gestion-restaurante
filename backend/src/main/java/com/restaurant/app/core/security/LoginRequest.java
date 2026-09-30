package com.restaurant.app.core.security;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El login (username o email) es obligatorio")
    String login,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {
}
