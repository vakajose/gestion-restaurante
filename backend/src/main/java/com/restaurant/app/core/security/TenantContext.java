package com.restaurant.app.core.security;

import java.util.UUID;

public record TenantContext(
    UUID tenantId,
    UUID branchId,
    UUID userId,
    String username,
    String role
) {
}
