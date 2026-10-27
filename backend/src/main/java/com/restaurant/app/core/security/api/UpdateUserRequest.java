package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    String username,

    String email,

    @NotBlank(message = "El rol es obligatorio")
    String role,

    UUID branchId,

    Boolean isActive,

    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    String newPassword
) {}
