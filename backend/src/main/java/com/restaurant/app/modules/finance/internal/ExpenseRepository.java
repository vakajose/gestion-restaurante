package com.restaurant.app.modules.finance.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Expense> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Expense> findByTenantIdAndBranchIdOrderByCreatedAtDesc(UUID tenantId, UUID branchId);

    List<Expense> findByTenantIdAndBranchIdAndApprovalStatusOrderByCreatedAtDesc(
        UUID tenantId, UUID branchId, String approvalStatus
    );

    List<Expense> findByTenantIdAndApprovalStatusOrderByCreatedAtDesc(
        UUID tenantId, String approvalStatus
    );

    List<Expense> findByTenantIdAndCashShiftIdOrderByCreatedAtDesc(
        UUID tenantId, UUID cashShiftId
    );

    List<Expense> findByTenantIdAndCashShiftIdAndPaidFromCashDrawerTrueAndApprovalStatus(
        UUID tenantId, UUID cashShiftId, String approvalStatus
    );

    List<Expense> findByTenantIdAndBranchIdAndExpenseDateBetweenOrderByExpenseDateDesc(
        UUID tenantId, UUID branchId, LocalDate startDate, LocalDate endDate
    );

    int countByTenantIdAndCashShiftIdAndApprovalStatus(
        UUID tenantId, UUID cashShiftId, String approvalStatus
    );
}
