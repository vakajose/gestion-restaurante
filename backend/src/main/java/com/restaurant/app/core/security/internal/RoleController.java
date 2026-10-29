package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.RolePublicApi;
import com.restaurant.app.core.security.api.RolePublicApi.CreateRoleRequest;
import com.restaurant.app.core.security.api.RolePublicApi.PermissionCatalogItemDto;
import com.restaurant.app.core.security.api.RolePublicApi.RoleDto;
import com.restaurant.app.core.security.api.RolePublicApi.UpdateRoleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class RoleController {

    private final RolePublicApi rolePublicApi;

    RoleController(RolePublicApi rolePublicApi) {
        this.rolePublicApi = rolePublicApi;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('roles:manage', 'users:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<List<RoleDto>> getRoles() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(rolePublicApi.listRoles(tenantId));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('roles:manage', 'users:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(
            rolePublicApi.getRole(tenantId, id)
                .orElseThrow(() -> new EntityNotFoundException("Rol", id))
        );
    }

    @GetMapping({"/roles/permissions-catalog", "/permissions/catalog"})
    @PreAuthorize("hasAnyAuthority('roles:manage', 'users:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<List<PermissionCatalogItemDto>> getPermissionsCatalog() {
        return ResponseEntity.ok(rolePublicApi.getPermissionsCatalog());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAnyAuthority('roles:manage', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        RoleDto created = rolePublicApi.createRole(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('roles:manage', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<RoleDto> updateRole(
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateRoleRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(rolePublicApi.updateRole(tenantId, id, request));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAnyAuthority('roles:manage', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        rolePublicApi.deleteRole(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
