package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {

    List<SubscriptionPayment> findByTenantIdOrderByPaidAtDesc(UUID tenantId);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM SubscriptionPayment p
        WHERE p.paidAt >= :startInclusive
          AND p.paidAt < :endExclusive
        """)
    BigDecimal sumPaymentsInRange(
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive
    );
}
