package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
    @Email(message = "El formato de correo electrónico no es válido")
    String email
) {}
