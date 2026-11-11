package com.restaurant.app.modules.finance.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface ShiftAuthorizationRepository extends JpaRepository<ShiftAuthorization, UUID> {

    Optional<ShiftAuthorization> findByTenantIdAndId(UUID tenantId, UUID id);

    List<ShiftAuthorization> findByTenantIdAndBranchIdOrderByCreatedAtDesc(UUID tenantId, UUID branchId);

    List<ShiftAuthorization> findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(UUID tenantId, UUID branchId, String status);

    List<ShiftAuthorization> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<ShiftAuthorization> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);
}
