package com.restaurant.app.core.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record TenantContext(
    UUID tenantId,
    UUID branchId,
    UUID userId,
    String username,
    String role,
    String tenantSlug,
    Set<String> permissions,
    Set<UUID> authorizedBranchIds
) {
    public TenantContext(UUID tenantId, UUID branchId, UUID userId, String username, String role) {
        this(tenantId, branchId, userId, username, role, null, Collections.emptySet(), Collections.emptySet());
    }

    public TenantContext(
        UUID tenantId,
        UUID branchId,
        UUID userId,
        String username,
        String role,
        String tenantSlug,
        Set<String> permissions
    ) {
        this(tenantId, branchId, userId, username, role, tenantSlug, permissions, Collections.emptySet());
    }

    public TenantContext withBranchId(UUID newBranchId) {
        return new TenantContext(
            tenantId,
            newBranchId,
            userId,
            username,
            role,
            tenantSlug,
            permissions,
            authorizedBranchIds
        );
    }

    public boolean isBranchAuthorized(UUID targetBranchId) {
        if (targetBranchId == null) {
            return false;
        }
        if ("SUPERADMIN".equalsIgnoreCase(role) || "ADMIN_TENANT".equalsIgnoreCase(role)) {
            return true;
        }
        return authorizedBranchIds != null && authorizedBranchIds.contains(targetBranchId);
    }

    public boolean hasPermission(String permission) {
        if (permission == null) {
            return false;
        }
        if ("SUPERADMIN".equalsIgnoreCase(role) || "ADMIN_TENANT".equalsIgnoreCase(role)) {
            return true;
        }
        return permissions != null && permissions.contains(permission);
    }
}
