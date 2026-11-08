package com.restaurant.app.modules.finance.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CashShiftRepository extends JpaRepository<CashShift, UUID> {

    Optional<CashShift> findFirstByTenantIdAndBranchIdAndStatusOrderByOpenedAtDesc(UUID tenantId, UUID branchId, String status);

    Optional<CashShift> findFirstByTenantIdAndBranchIdAndOpenedByAndStatusOrderByOpenedAtDesc(UUID tenantId, UUID branchId, UUID openedBy, String status);

    Optional<CashShift> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, String status);

    Optional<CashShift> findByTenantIdAndBranchIdAndOpenedByAndStatus(UUID tenantId, UUID branchId, UUID openedBy, String status);

    List<CashShift> findByTenantIdAndBranchIdAndStatusOrderByOpenedAtDesc(UUID tenantId, UUID branchId, String status);

    Optional<CashShift> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CashShift> findByTenantIdAndBranchIdOrderByOpenedAtDesc(UUID tenantId, UUID branchId);

    List<CashShift> findByTenantIdOrderByOpenedAtDesc(UUID tenantId);

    @Query("""
        SELECT s FROM CashShift s 
        WHERE s.tenantId = :tenantId
          AND (:branchId IS NULL OR s.branchId = :branchId)
          AND (:startUtc IS NULL OR s.openedAt >= :startUtc)
          AND (:endUtc IS NULL OR s.openedAt <= :endUtc)
          AND (:cashierId IS NULL OR s.openedBy = :cashierId)
          AND (:status IS NULL OR :status = 'ALL' OR s.status = :status)
          AND (
            :discrepancyType IS NULL OR :discrepancyType = 'ALL' 
            OR (:discrepancyType = 'CUADRADO' AND (s.difference IS NOT NULL AND s.difference = 0))
            OR (:discrepancyType = 'SOBRANTE' AND (s.difference IS NOT NULL AND s.difference > 0))
            OR (:discrepancyType = 'FALTANTE' AND (s.difference IS NOT NULL AND s.difference < 0))
          )
        ORDER BY s.openedAt DESC
    """)
    List<CashShift> findShiftsWithFilters(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("startUtc") Instant startUtc,
        @Param("endUtc") Instant endUtc,
        @Param("cashierId") UUID cashierId,
        @Param("status") String status,
        @Param("discrepancyType") String discrepancyType,
        Pageable pageable
    );
}
