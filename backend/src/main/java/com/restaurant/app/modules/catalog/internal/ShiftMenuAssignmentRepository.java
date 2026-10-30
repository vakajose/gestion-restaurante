package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ShiftMenuAssignmentRepository extends JpaRepository<ShiftMenuAssignment, UUID> {

    Optional<ShiftMenuAssignment> findByCashShiftId(UUID cashShiftId);

    Optional<ShiftMenuAssignment> findByTenantIdAndBranchIdAndCashShiftId(UUID tenantId, UUID branchId, UUID cashShiftId);
}
