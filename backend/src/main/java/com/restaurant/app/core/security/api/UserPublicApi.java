package com.restaurant.app.core.security.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserPublicApi {

    List<UserSummaryDto> getUsersByTenant(UUID tenantId);

    UserSummaryDto getUserById(UUID tenantId, UUID userId);

    Optional<UserSummaryDto> findUserSummary(UUID userId);

    Optional<UserSummaryDto> findByTenantIdAndUsername(UUID tenantId, String username);

    boolean isUserActive(UUID userId);

    void seedDefaultRolesForTenant(UUID tenantId);

    UserSummaryDto createUser(UUID tenantId, CreateUserRequest request);

    UserSummaryDto updateUser(UUID tenantId, UUID userId, UpdateUserRequest request);

    UserSummaryDto toggleUserStatus(UUID tenantId, UUID userId, boolean isActive);

    UserSummaryDto updateProfile(UUID tenantId, UUID userId, UpdateProfileRequest request);

    void changePassword(UUID tenantId, UUID userId, ChangePasswordRequest request);

    UserSummaryDto adminResetPassword(UUID tenantId, UUID userId, String newPassword);

    void updatePin(UUID tenantId, UUID userId, String pin);

    Optional<UserSummaryDto> validateSupervisorPin(UUID tenantId, UUID branchId, String pin);

    long countUsersByTenant(UUID tenantId, boolean activeOnly);

    void recordLoginTimestamp(UUID userId);

    Optional<Instant> findFirstTenantLoginAt(UUID tenantId);

    Map<UUID, Instant> findFirstTenantLoginAtMap();
}
