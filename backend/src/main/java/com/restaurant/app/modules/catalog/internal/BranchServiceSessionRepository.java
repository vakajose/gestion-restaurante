package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface BranchServiceSessionRepository extends JpaRepository<BranchServiceSession, UUID> {

    @Query("SELECT s FROM BranchServiceSession s WHERE s.tenantId = :tenantId AND s.branchId = :branchId AND s.status IN ('CONFIGURED', 'ACTIVE')")
    Optional<BranchServiceSession> findOpenSessionByBranch(@Param("tenantId") UUID tenantId, @Param("branchId") UUID branchId);

    Optional<BranchServiceSession> findByIdAndTenantId(UUID id, UUID tenantId);
}
