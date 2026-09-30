package com.restaurant.app.core.security;

import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    String role,
    UUID tenantId,
    UUID branchId
) {
    public static UserDto fromEntity(AppUser user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId()
        );
    }
}
