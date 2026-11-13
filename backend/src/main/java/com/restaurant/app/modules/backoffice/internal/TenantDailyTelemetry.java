package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenant_daily_telemetry")
class TenantDailyTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "paid_orders_count", nullable = false)
    private int paidOrdersCount = 0;

    @Column(name = "gross_sales_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossSalesAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "cancelled_orders_count", nullable = false)
    private int cancelledOrdersCount = 0;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    protected TenantDailyTelemetry() {
    }

    TenantDailyTelemetry(UUID tenantId, UUID branchId, LocalDate statDate) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.statDate = statDate;
    }

    void recordPaidOrder(BigDecimal totalAmount, Instant eventAt) {
        this.paidOrdersCount = this.paidOrdersCount + 1;
        if (totalAmount != null) {
            this.grossSalesAmount = this.grossSalesAmount.add(totalAmount).setScale(2, RoundingMode.HALF_UP);
        }
        this.lastEventAt = eventAt != null ? eventAt : Instant.now();
    }

    void recordCancelledOrder(Instant eventAt) {
        this.cancelledOrdersCount = this.cancelledOrdersCount + 1;
        this.lastEventAt = eventAt != null ? eventAt : Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    UUID getBranchId() {
        return branchId;
    }

    LocalDate getStatDate() {
        return statDate;
    }

    int getPaidOrdersCount() {
        return paidOrdersCount;
    }

    BigDecimal getGrossSalesAmount() {
        return grossSalesAmount;
    }

    int getCancelledOrdersCount() {
        return cancelledOrdersCount;
    }

    Instant getLastEventAt() {
        return lastEventAt;
    }
}
