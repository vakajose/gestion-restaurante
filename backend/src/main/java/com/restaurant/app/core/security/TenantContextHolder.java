package com.restaurant.app.core.security;

import java.util.Optional;
import java.util.UUID;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> getContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static UUID getTenantId() {
        return getContext().map(TenantContext::tenantId).orElse(null);
    }

    public static UUID getBranchId() {
        return getContext().map(TenantContext::branchId).orElse(null);
    }

    public static UUID getUserId() {
        return getContext().map(TenantContext::userId).orElse(null);
    }

    public static String getUsername() {
        return getContext().map(TenantContext::username).orElse(null);
    }

    public static String getRole() {
        return getContext().map(TenantContext::role).orElse(null);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
