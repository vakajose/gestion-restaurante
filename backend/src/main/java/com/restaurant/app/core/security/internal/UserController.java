package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.CreateUserRequest;
import com.restaurant.app.core.security.api.RolePublicApi;
import com.restaurant.app.core.security.api.RolePublicApi.AssignBranchesRequest;
import com.restaurant.app.core.security.api.RolePublicApi.ResetPasswordRequest;
import com.restaurant.app.core.security.api.RolePublicApi.ResetPasswordResponse;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.core.security.api.UpdateUserRequest;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final UserPublicApi userPublicApi;
    private final RolePublicApi rolePublicApi;
    private final SessionPublicApi sessionPublicApi;

    UserController(
        UserPublicApi userPublicApi,
        RolePublicApi rolePublicApi,
        SessionPublicApi sessionPublicApi
    ) {
        this.userPublicApi = userPublicApi;
        this.rolePublicApi = rolePublicApi;
        this.sessionPublicApi = sessionPublicApi;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ResponseEntity<List<UserSummaryDto>> getUsers() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(userPublicApi.getUsersByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    public ResponseEntity<UserSummaryDto> getUserById(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(userPublicApi.getUserById(tenantId, id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:write')")
    public ResponseEntity<UserSummaryDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UserSummaryDto created = userPublicApi.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:write')")
    public ResponseEntity<UserSummaryDto> updateUser(
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(userPublicApi.updateUser(tenantId, id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('users:write')")
    public ResponseEntity<UserSummaryDto> toggleUserStatus(
        @PathVariable("id") UUID id,
        @RequestParam("isActive") boolean isActive
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UserSummaryDto updated = userPublicApi.toggleUserStatus(tenantId, id, isActive);
        if (!isActive) {
            sessionPublicApi.revokeAllUserSessions(id);
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('users:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(
        @PathVariable("id") UUID id,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        UUID callerId = TenantContextHolder.getUserId();
        ResetPasswordResponse response = rolePublicApi.resetUserPassword(callerId, id, request);
        sessionPublicApi.revokeAllUserSessions(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/branches")
    @PutMapping("/{id}/branches")
    @PreAuthorize("hasAnyAuthority('users:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<UserSummaryDto> assignUserBranches(
        @PathVariable("id") UUID id,
        @RequestBody AssignBranchesRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        rolePublicApi.assignUserBranches(tenantId, id, request);
        return ResponseEntity.ok(userPublicApi.getUserById(tenantId, id));
    }

    @PostMapping("/{id}/revoke-sessions")
    @PreAuthorize("hasAnyAuthority('users:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<Map<String, String>> revokeUserSessions(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        userPublicApi.getUserById(tenantId, id); // Verifica pertenencia al tenant
        sessionPublicApi.revokeAllUserSessions(id);
        return ResponseEntity.ok(Map.of("message", "Sesiones del usuario revocadas exitosamente"));
    }
}
