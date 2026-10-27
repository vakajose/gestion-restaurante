package com.restaurant.app.core.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    String role,
    UUID tenantId,
    UUID branchId,
    String tenantSlug,
    Set<String> permissions,
    Set<UUID> authorizedBranchIds
) {
    public UserDto(UUID id, String username, String email, String role, UUID tenantId, UUID branchId) {
        this(id, username, email, role, tenantId, branchId, null, Collections.emptySet(), Collections.emptySet());
    }

    public UserDto(
        UUID id,
        String username,
        String email,
        String role,
        UUID tenantId,
        UUID branchId,
        String tenantSlug,
        Set<String> permissions
    ) {
        this(id, username, email, role, tenantId, branchId, tenantSlug, permissions, Collections.emptySet());
    }

    public static UserDto fromEntity(AppUser user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            null,
            Collections.emptySet(),
            Collections.emptySet()
        );
    }

    public static UserDto fromEntity(AppUser user, String tenantSlug, Set<String> permissions) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            tenantSlug,
            permissions != null ? permissions : Collections.emptySet(),
            Collections.emptySet()
        );
    }

    public static UserDto fromEntity(
        AppUser user,
        String tenantSlug,
        Set<String> permissions,
        Set<UUID> authorizedBranchIds
    ) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            tenantSlug,
            permissions != null ? permissions : Collections.emptySet(),
            authorizedBranchIds != null ? authorizedBranchIds : Collections.emptySet()
        );
    }
}
