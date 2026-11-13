package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface SubscriptionChargeRepository extends JpaRepository<SubscriptionCharge, UUID> {

    List<SubscriptionCharge> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<SubscriptionCharge> findByTenantIdAndChargeStatusInOrderByCreatedAtAsc(
        UUID tenantId,
        Collection<ChargeStatus> statuses
    );

    @Query("""
        SELECT COALESCE(SUM(c.amountDue - c.paidAmount), 0)
        FROM SubscriptionCharge c
        WHERE c.tenantId = :tenantId
          AND c.chargeStatus IN ('PENDING', 'PARTIAL')
        """)
    BigDecimal sumPendingBalanceByTenantId(@Param("tenantId") UUID tenantId);

    @Query("""
        SELECT c.tenantId, COALESCE(SUM(c.amountDue - c.paidAmount), 0)
        FROM SubscriptionCharge c
        WHERE c.chargeStatus IN ('PENDING', 'PARTIAL')
        GROUP BY c.tenantId
        """)
    List<Object[]> sumPendingBalanceGroupedByTenant();

    @Query("""
        SELECT COALESCE(SUM(c.amountDue - c.paidAmount), 0)
        FROM SubscriptionCharge c
        WHERE c.chargeStatus IN ('PENDING', 'PARTIAL')
        """)
    BigDecimal sumTotalPendingDebt();
}
