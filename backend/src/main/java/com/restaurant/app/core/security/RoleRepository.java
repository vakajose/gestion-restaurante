package com.restaurant.app.core.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByTenantId(UUID tenantId);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    @Query("SELECT r FROM Role r WHERE (r.tenantId = :tenantId OR r.tenantId IS NULL) AND r.code = :code")
    Optional<Role> findByTenantIdOrSystemAndCode(@Param("tenantId") UUID tenantId, @Param("code") String code);
}
