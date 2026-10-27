package com.restaurant.app.core.security.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPublicApi {

    List<UserSummaryDto> getUsersByTenant(UUID tenantId);

    UserSummaryDto getUserById(UUID tenantId, UUID userId);

    Optional<UserSummaryDto> findUserSummary(UUID userId);

    boolean isUserActive(UUID userId);

    UserSummaryDto createUser(UUID tenantId, CreateUserRequest request);

    UserSummaryDto updateUser(UUID tenantId, UUID userId, UpdateUserRequest request);

    UserSummaryDto toggleUserStatus(UUID tenantId, UUID userId, boolean isActive);

    UserSummaryDto updateProfile(UUID tenantId, UUID userId, UpdateProfileRequest request);

    void changePassword(UUID tenantId, UUID userId, ChangePasswordRequest request);
}
