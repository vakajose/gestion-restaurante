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
    Set<String> permissions
) {
    public TenantContext(UUID tenantId, UUID branchId, UUID userId, String username, String role) {
        this(tenantId, branchId, userId, username, role, null, Collections.emptySet());
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
