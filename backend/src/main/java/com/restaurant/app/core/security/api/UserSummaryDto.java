package com.restaurant.app.core.security.api;

import com.restaurant.app.core.security.AppUser;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    String username,
    String fullName,
    String email,
    String role,
    boolean isActive,
    Instant roleValidUntil,
    boolean mustChangePassword,
    Set<UUID> authorizedBranchIds,
    Instant createdAt
) {
    public UserSummaryDto(
        UUID id,
        UUID tenantId,
        UUID branchId,
        String username,
        String email,
        String role,
        boolean isActive,
        Instant createdAt
    ) {
        this(id, tenantId, branchId, username, null, email, role, isActive, null, false, Collections.emptySet(), createdAt);
    }

    public UserSummaryDto(
        UUID id,
        UUID tenantId,
        UUID branchId,
        String username,
        String fullName,
        String email,
        String role,
        boolean isActive,
        Instant createdAt
    ) {
        this(id, tenantId, branchId, username, fullName, email, role, isActive, null, false, Collections.emptySet(), createdAt);
    }

    public static UserSummaryDto fromEntity(AppUser user) {
        Set<UUID> branches = user.getBranchId() != null ? Set.of(user.getBranchId()) : Collections.emptySet();
        return fromEntity(user, branches);
    }

    public static UserSummaryDto fromEntity(AppUser user, Set<UUID> authorizedBranchIds) {
        return new UserSummaryDto(
            user.getId(),
            user.getTenantId(),
            user.getBranchId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getRoleValidUntil(),
            user.isMustChangePassword(),
            authorizedBranchIds != null ? authorizedBranchIds : Collections.emptySet(),
            user.getCreatedAt()
        );
    }
}
