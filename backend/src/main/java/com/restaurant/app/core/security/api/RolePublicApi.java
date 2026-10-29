package com.restaurant.app.core.security.api;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RolePublicApi {

    record PermissionCatalogItemDto(
        String code,
        String name,
        String description,
        String module
    ) {}

    record RoleDto(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String description,
        boolean isSystem,
        Set<String> permissions
    ) {}

    record CreateRoleRequest(
        String code,
        String name,
        String description,
        Set<String> permissions
    ) {}

    record UpdateRoleRequest(
        String name,
        String description,
        Set<String> permissions
    ) {}

    record ResetPasswordRequest(
        String temporaryPassword,
        String newPassword
    ) {
        public ResetPasswordRequest(String temporaryPassword) {
            this(temporaryPassword, temporaryPassword);
        }

        public String resolvedPassword() {
            if (temporaryPassword != null && !temporaryPassword.isBlank()) {
                return temporaryPassword;
            }
            return newPassword;
        }
    }

    record ResetPasswordResponse(
        UUID userId,
        boolean mustChangePassword,
        Instant resetAt
    ) {}

    record ChangePasswordRequest(
        String currentPassword,
        String newPassword
    ) {}

    record AssignBranchesRequest(
        UUID defaultBranchId,
        Set<UUID> authorizedBranchIds,
        Set<UUID> branchIds
    ) {
        public AssignBranchesRequest(UUID defaultBranchId, Set<UUID> authorizedBranchIds) {
            this(defaultBranchId, authorizedBranchIds, authorizedBranchIds);
        }

        public Set<UUID> resolvedBranchIds() {
            if (authorizedBranchIds != null && !authorizedBranchIds.isEmpty()) {
                return authorizedBranchIds;
            }
            return branchIds != null ? branchIds : Collections.emptySet();
        }
    }

    List<RoleDto> listRoles(UUID tenantId);

    Optional<RoleDto> getRole(UUID tenantId, UUID roleId);

    RoleDto createRole(UUID tenantId, CreateRoleRequest request);

    RoleDto updateRole(UUID tenantId, UUID roleId, UpdateRoleRequest request);

    void deleteRole(UUID tenantId, UUID roleId);

    List<PermissionCatalogItemDto> getPermissionsCatalog();

    ResetPasswordResponse resetUserPassword(UUID callerId, UUID targetUserId, ResetPasswordRequest request);

    void changeOwnPassword(UUID userId, ChangePasswordRequest request);

    void assignUserBranches(UUID tenantId, UUID targetUserId, AssignBranchesRequest request);
}
