package com.restaurant.app.core.security;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    String tenantIdentifier,

    @NotBlank(message = "El login (username o email) es obligatorio")
    String login,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {
    public LoginRequest(String login, String password) {
        this(null, login, password);
    }
}
