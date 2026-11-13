package com.restaurant.app.modules.pos.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByTenantIdAndClientTransactionId(UUID tenantId, UUID clientTransactionId);

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Order> findByTenantIdAndBranchIdOrderByCreatedAtDesc(UUID tenantId, UUID branchId);

    List<Order> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Order> findByTenantIdAndBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID tenantId, UUID branchId, Instant start, Instant end
    );

    List<Order> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        UUID tenantId, Instant start, Instant end
    );

    long countByTenantIdAndBranchIdAndCreatedAtBetween(
        UUID tenantId, UUID branchId, Instant start, Instant end
    );

    List<Order> findByTenantIdAndCashShiftId(UUID tenantId, UUID cashShiftId);

    List<Order> findByTenantIdAndCashShiftIdOrderByCreatedAtDesc(UUID tenantId, UUID cashShiftId);

    List<Order> findByTenantIdAndCashShiftIdAndCreatedByOrderByCreatedAtDesc(UUID tenantId, UUID cashShiftId, UUID createdBy);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.tenantId = :tenantId
          AND o.orderStatus <> 'CANCELLED'
          AND o.createdAt >= :startInclusive
          AND o.createdAt < :endExclusive
        """)
    long countActiveOrdersInRange(
        @Param("tenantId") UUID tenantId,
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive
    );
}
