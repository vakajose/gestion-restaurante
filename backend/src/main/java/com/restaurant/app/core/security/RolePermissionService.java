package com.restaurant.app.core.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> getPermissionsForRole(UUID tenantId, String roleCode) {
        if (roleCode == null) {
            return Set.of();
        }

        List<String> dbPermissions = rolePermissionRepository.findPermissionsByTenantIdAndRoleCode(tenantId, roleCode);
        if (!dbPermissions.isEmpty()) {
            return new HashSet<>(dbPermissions);
        }

        // Si aún no se han configurado permisos personalizados en BD, retornar los defaults del sistema
        return SystemPermissions.getDefaultPermissionsForRole(roleCode);
    }
}
