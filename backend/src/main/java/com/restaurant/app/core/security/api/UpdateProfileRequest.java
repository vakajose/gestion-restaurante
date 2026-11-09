package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String fullName,

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    String phoneNumber,

    @Email(message = "Formato de correo no válido")
    String email
) {
    public UpdateProfileRequest(String email) {
        this("Usuario", null, email);
    }
}
