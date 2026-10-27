package com.restaurant.app.core.security.api;

import com.restaurant.app.core.security.AppUser;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    String username,
    String email,
    String role,
    boolean isActive,
    Instant createdAt
) {
    public static UserSummaryDto fromEntity(AppUser user) {
        return new UserSummaryDto(
            user.getId(),
            user.getTenantId(),
            user.getBranchId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()
        );
    }
}
