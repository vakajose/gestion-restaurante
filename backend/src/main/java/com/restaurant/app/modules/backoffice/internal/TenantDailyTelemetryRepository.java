package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TenantDailyTelemetryRepository extends JpaRepository<TenantDailyTelemetry, UUID> {

    Optional<TenantDailyTelemetry> findByTenantIdAndBranchIdAndStatDate(
        UUID tenantId,
        UUID branchId,
        LocalDate statDate
    );

    List<TenantDailyTelemetry> findByTenantIdAndStatDateBetweenOrderByStatDateAsc(
        UUID tenantId,
        LocalDate from,
        LocalDate to
    );

    @Query("""
        SELECT t.tenantId,
               COALESCE(SUM(t.paidOrdersCount), 0),
               COALESCE(SUM(t.grossSalesAmount), 0),
               COALESCE(SUM(t.cancelledOrdersCount), 0)
        FROM TenantDailyTelemetry t
        WHERE t.statDate >= :from AND t.statDate <= :to
        GROUP BY t.tenantId
        """)
    List<Object[]> aggregateByTenantInRange(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
