package com.restaurant.app.modules.finance.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CashShiftRepository extends JpaRepository<CashShift, UUID> {

    Optional<CashShift> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, String status);

    Optional<CashShift> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CashShift> findByTenantIdAndBranchIdOrderByOpenedAtDesc(UUID tenantId, UUID branchId);

    List<CashShift> findByTenantIdOrderByOpenedAtDesc(UUID tenantId);
}
