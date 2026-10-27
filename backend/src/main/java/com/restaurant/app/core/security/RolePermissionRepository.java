package com.restaurant.app.core.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.roleId = :roleId")
    List<String> findPermissionsByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT rp.permission FROM RolePermission rp JOIN Role r ON rp.roleId = r.id WHERE (r.tenantId = :tenantId OR r.tenantId IS NULL) AND r.code = :roleCode")
    List<String> findPermissionsByTenantIdAndRoleCode(@Param("tenantId") UUID tenantId, @Param("roleCode") String roleCode);
}
