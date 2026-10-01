package com.restaurant.app.modules.finance.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_shifts")
class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "closed_by")
    private UUID closedBy;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "initial_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal initialCash = BigDecimal.ZERO;

    @Column(name = "total_cash_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCashSales = BigDecimal.ZERO;

    @Column(name = "total_card_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCardSales = BigDecimal.ZERO;

    @Column(name = "total_other_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOtherSales = BigDecimal.ZERO;

    @Column(name = "total_cash_expenses", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCashExpenses = BigDecimal.ZERO;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "actual_cash", precision = 12, scale = 2)
    private BigDecimal actualCash;

    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(name = "status", nullable = false, length = 15)
    private String status = "OPEN";

    CashShift() {
    }

    CashShift(UUID tenantId, UUID branchId, UUID openedBy, BigDecimal initialCash) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.openedBy = openedBy;
        this.initialCash = initialCash != null ? initialCash : BigDecimal.ZERO;
        this.status = "OPEN";
        this.openedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(UUID openedBy) {
        this.openedBy = openedBy;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public BigDecimal getTotalCashSales() {
        return totalCashSales;
    }

    public void setTotalCashSales(BigDecimal totalCashSales) {
        this.totalCashSales = totalCashSales;
    }

    public BigDecimal getTotalCardSales() {
        return totalCardSales;
    }

    public void setTotalCardSales(BigDecimal totalCardSales) {
        this.totalCardSales = totalCardSales;
    }

    public BigDecimal getTotalOtherSales() {
        return totalOtherSales;
    }

    public void setTotalOtherSales(BigDecimal totalOtherSales) {
        this.totalOtherSales = totalOtherSales;
    }

    public BigDecimal getTotalCashExpenses() {
        return totalCashExpenses;
    }

    public void setTotalCashExpenses(BigDecimal totalCashExpenses) {
        this.totalCashExpenses = totalCashExpenses;
    }

    public BigDecimal getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(BigDecimal expectedCash) {
        this.expectedCash = expectedCash;
    }

    public BigDecimal getActualCash() {
        return actualCash;
    }

    public void setActualCash(BigDecimal actualCash) {
        this.actualCash = actualCash;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
