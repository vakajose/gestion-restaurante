package com.restaurant.app.core.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
    String username,

    @Size(max = 100, message = "El nombre completo no puede exceder 100 caracteres")
    String fullName,

    String email,

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    String phoneNumber,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,

    @NotBlank(message = "El rol es obligatorio")
    String role,

    UUID branchId,

    Set<UUID> authorizedBranchIds,

    Instant roleValidUntil
) {
    public CreateUserRequest(String username, String email, String password, String role, UUID branchId) {
        this(username, null, email, null, password, role, branchId, null, null);
    }

    public CreateUserRequest(String username, String fullName, String email, String password, String role, UUID branchId) {
        this(username, fullName, email, null, password, role, branchId, null, null);
    }

    public CreateUserRequest(String username, String fullName, String email, String password, String role, UUID branchId, Set<UUID> authorizedBranchIds, Instant roleValidUntil) {
        this(username, fullName, email, null, password, role, branchId, authorizedBranchIds, roleValidUntil);
    }
}
