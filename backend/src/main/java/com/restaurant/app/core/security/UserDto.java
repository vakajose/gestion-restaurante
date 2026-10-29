package com.restaurant.app.core.security;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String fullName,
    String email,
    String role,
    UUID tenantId,
    UUID branchId,
    String tenantSlug,
    Set<String> permissions,
    Set<UUID> authorizedBranchIds,
    Instant roleValidUntil,
    boolean mustChangePassword
) {
    public UserDto(UUID id, String username, String email, String role, UUID tenantId, UUID branchId) {
        this(id, username, null, email, role, tenantId, branchId, null, Collections.emptySet(), Collections.emptySet(), null, false);
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
        this(id, username, null, email, role, tenantId, branchId, tenantSlug, permissions, Collections.emptySet(), null, false);
    }

    public UserDto(
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
        this(id, username, null, email, role, tenantId, branchId, tenantSlug, permissions, authorizedBranchIds, null, false);
    }

    public UserDto(
        UUID id,
        String username,
        String fullName,
        String email,
        String role,
        UUID tenantId,
        UUID branchId,
        String tenantSlug,
        Set<String> permissions,
        Set<UUID> authorizedBranchIds
    ) {
        this(id, username, fullName, email, role, tenantId, branchId, tenantSlug, permissions, authorizedBranchIds, null, false);
    }

    public static UserDto fromEntity(AppUser user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            user.getRoleValidUntil(),
            user.isMustChangePassword()
        );
    }

    public static UserDto fromEntity(AppUser user, String tenantSlug, Set<String> permissions) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            tenantSlug,
            permissions != null ? permissions : Collections.emptySet(),
            Collections.emptySet(),
            user.getRoleValidUntil(),
            user.isMustChangePassword()
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
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            tenantSlug,
            permissions != null ? permissions : Collections.emptySet(),
            authorizedBranchIds != null ? authorizedBranchIds : Collections.emptySet(),
            user.getRoleValidUntil(),
            user.isMustChangePassword()
        );
    }
}
