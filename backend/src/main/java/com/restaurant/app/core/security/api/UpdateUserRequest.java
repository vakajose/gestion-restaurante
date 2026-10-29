package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    String username,

    @Size(max = 100, message = "El nombre completo no puede exceder 100 caracteres")
    String fullName,

    String email,

    @NotBlank(message = "El rol es obligatorio")
    String role,

    UUID branchId,

    Set<UUID> authorizedBranchIds,

    Instant roleValidUntil,

    Boolean isActive,

    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    String newPassword
) {
    public UpdateUserRequest(
        String username,
        String email,
        String role,
        UUID branchId,
        Boolean isActive,
        String newPassword
    ) {
        this(username, null, email, role, branchId, null, null, isActive, newPassword);
    }

    public UpdateUserRequest(
        String username,
        String fullName,
        String email,
        String role,
        UUID branchId,
        Boolean isActive,
        String newPassword
    ) {
        this(username, fullName, email, role, branchId, null, null, isActive, newPassword);
    }
}
