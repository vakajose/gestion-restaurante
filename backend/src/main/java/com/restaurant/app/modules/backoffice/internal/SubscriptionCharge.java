package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscription_charges")
class SubscriptionCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_name_snapshot", nullable = false, length = 120)
    private String planNameSnapshot;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "BOB";

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_status", nullable = false, length = 20)
    private ChargeStatus chargeStatus = ChargeStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by_admin_id")
    private UUID createdByAdminId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    protected SubscriptionCharge() {
    }

    SubscriptionCharge(
        UUID tenantId,
        UUID planId,
        String planNameSnapshot,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal amountDue,
        BigDecimal paidAmount,
        String currency,
        LocalDate dueDate,
        String notes,
        UUID createdByAdminId
    ) {
        this.tenantId = tenantId;
        this.planId = planId;
        this.planNameSnapshot = planNameSnapshot;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amountDue = (amountDue != null ? amountDue : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.paidAmount = (paidAmount != null ? paidAmount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        this.currency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "BOB";
        this.dueDate = dueDate;
        this.notes = notes;
        this.createdByAdminId = createdByAdminId;
        recalculateStatus();
    }

    BigDecimal getRemainingAmount() {
        if (chargeStatus == ChargeStatus.WAIVED || chargeStatus == ChargeStatus.PAID) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal diff = amountDue.subtract(paidAmount);
        return diff.compareTo(BigDecimal.ZERO) > 0
            ? diff.setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal applyPayment(BigDecimal availableAmount) {
        if (availableAmount == null || availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal remaining = getRemainingAmount();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal applied = availableAmount.min(remaining).setScale(2, RoundingMode.HALF_UP);
        this.paidAmount = this.paidAmount.add(applied).setScale(2, RoundingMode.HALF_UP);
        recalculateStatus();
        return applied;
    }

    void waive(String reason) {
        this.chargeStatus = ChargeStatus.WAIVED;
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null && !this.notes.isBlank())
                ? this.notes + " | Condonado: " + reason.trim()
                : "Condonado: " + reason.trim();
        }
    }

    private void recalculateStatus() {
        if (this.chargeStatus == ChargeStatus.WAIVED) {
            return;
        }
        if (this.amountDue.compareTo(BigDecimal.ZERO) == 0 || this.paidAmount.compareTo(this.amountDue) >= 0) {
            this.chargeStatus = ChargeStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.chargeStatus = ChargeStatus.PARTIAL;
        } else {
            this.chargeStatus = ChargeStatus.PENDING;
        }
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    UUID getPlanId() {
        return planId;
    }

    String getPlanNameSnapshot() {
        return planNameSnapshot;
    }

    Instant getPeriodStart() {
        return periodStart;
    }

    Instant getPeriodEnd() {
        return periodEnd;
    }

    BigDecimal getAmountDue() {
        return amountDue;
    }

    BigDecimal getPaidAmount() {
        return paidAmount;
    }

    String getCurrency() {
        return currency;
    }

    ChargeStatus getChargeStatus() {
        return chargeStatus;
    }

    LocalDate getDueDate() {
        return dueDate;
    }

    String getNotes() {
        return notes;
    }

    UUID getCreatedByAdminId() {
        return createdByAdminId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
