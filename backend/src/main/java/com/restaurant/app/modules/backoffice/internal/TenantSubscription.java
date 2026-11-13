package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
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

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_subscriptions")
class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "plan_id")
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_mode", nullable = false, length = 30)
    private SuspensionMode suspensionMode = SuspensionMode.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_suspend_mode_on_expire", nullable = false, length = 30)
    private SuspensionMode autoSuspendModeOnExpire = SuspensionMode.POS_AND_SHIFT_ONLY;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "grace_until")
    private Instant graceUntil;

    @Column(name = "grace_max_orders")
    private Integer graceMaxOrders;

    @Column(name = "grace_orders_used", nullable = false)
    private int graceOrdersUsed = 0;

    @Column(name = "grace_notes", columnDefinition = "TEXT")
    private String graceNotes;

    @Column(name = "override_max_branches")
    private Integer overrideMaxBranches;

    @Column(name = "override_max_users")
    private Integer overrideMaxUsers;

    @Column(name = "override_max_dishes")
    private Integer overrideMaxDishes;

    @Column(name = "override_max_inventory_items")
    private Integer overrideMaxInventoryItems;

    @Column(name = "override_max_monthly_orders")
    private Integer overrideMaxMonthlyOrders;

    @Column(name = "updated_by_admin_id")
    private UUID updatedByAdminId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    protected TenantSubscription() {
    }

    TenantSubscription(UUID tenantId, UUID planId, SubscriptionStatus status, Instant start, Instant end) {
        this.tenantId = tenantId;
        this.planId = planId;
        this.status = status;
        this.suspensionMode = SuspensionMode.NONE;
        this.autoSuspendModeOnExpire = SuspensionMode.POS_AND_SHIFT_ONLY;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
    }

    Map<QuotaResource, Integer> resolveEffectiveQuotas(SubscriptionPlan plan) {
        Map<QuotaResource, Integer> map = new EnumMap<>(QuotaResource.class);
        for (QuotaResource resource : QuotaResource.values()) {
            Integer overrideVal = overrideFor(resource);
            Integer baseVal = plan != null ? plan.limitFor(resource) : null;
            map.put(resource, overrideVal != null ? overrideVal : baseVal);
        }
        return map;
    }

    Integer overrideFor(QuotaResource resource) {
        return switch (resource) {
            case BRANCHES -> overrideMaxBranches;
            case USERS -> overrideMaxUsers;
            case DISHES -> overrideMaxDishes;
            case INVENTORY_ITEMS -> overrideMaxInventoryItems;
            case MONTHLY_ORDERS -> overrideMaxMonthlyOrders;
        };
    }

    void activatePlanPeriod(UUID planId, Instant periodStart, Instant periodEnd, SubscriptionStatus targetStatus, UUID adminId) {
        this.planId = planId;
        this.status = targetStatus != null ? targetStatus : SubscriptionStatus.ACTIVE;
        this.suspensionMode = SuspensionMode.NONE;
        this.suspensionReason = null;
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        clearGraceFields();
        this.updatedByAdminId = adminId;
    }

    void grantConditionalGrace(Instant graceUntil, Integer graceMaxOrders, String graceNotes, UUID adminId) {
        this.status = SubscriptionStatus.GRACE_PERIOD;
        this.suspensionMode = SuspensionMode.NONE;
        this.suspensionReason = null;
        this.graceUntil = graceUntil;
        this.graceMaxOrders = graceMaxOrders;
        this.graceOrdersUsed = 0;
        this.graceNotes = graceNotes;
        this.updatedByAdminId = adminId;
    }

    void suspend(SuspensionMode mode, String reason, UUID adminId) {
        this.status = SubscriptionStatus.SUSPENDED;
        this.suspensionMode = mode == SuspensionMode.TOTAL ? SuspensionMode.TOTAL : SuspensionMode.POS_AND_SHIFT_ONLY;
        this.suspensionReason = reason;
        this.updatedByAdminId = adminId;
    }

    void reactivate(Instant now, UUID adminId) {
        this.suspensionMode = SuspensionMode.NONE;
        this.suspensionReason = null;
        this.updatedByAdminId = adminId;
        if (graceUntil != null || graceMaxOrders != null) {
            boolean graceValidByTime = graceUntil == null || graceUntil.isAfter(now);
            boolean graceValidByOrders = graceMaxOrders == null || graceOrdersUsed < graceMaxOrders;
            if (graceValidByTime && graceValidByOrders) {
                this.status = SubscriptionStatus.GRACE_PERIOD;
                return;
            }
        }
        this.status = SubscriptionStatus.ACTIVE;
    }

    void updateQuotaOverrides(
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders,
        UUID adminId
    ) {
        this.overrideMaxBranches = maxBranches;
        this.overrideMaxUsers = maxUsers;
        this.overrideMaxDishes = maxDishes;
        this.overrideMaxInventoryItems = maxInventoryItems;
        this.overrideMaxMonthlyOrders = maxMonthlyOrders;
        this.updatedByAdminId = adminId;
    }

    boolean incrementGraceOrderAndCheckAutoSuspend(Instant now) {
        if (this.status != SubscriptionStatus.GRACE_PERIOD) {
            return false;
        }
        this.graceOrdersUsed = this.graceOrdersUsed + 1;
        boolean timeExpired = this.graceUntil != null && !this.graceUntil.isAfter(now);
        boolean salesReached = this.graceMaxOrders != null && this.graceOrdersUsed >= this.graceMaxOrders;
        if (timeExpired || salesReached) {
            this.status = SubscriptionStatus.SUSPENDED;
            this.suspensionMode = this.autoSuspendModeOnExpire != null
                ? this.autoSuspendModeOnExpire
                : SuspensionMode.POS_AND_SHIFT_ONLY;
            this.suspensionReason = salesReached
                ? "Límite de órdenes de período de gracia alcanzado (" + this.graceOrdersUsed + "/" + this.graceMaxOrders + ")"
                : "Período de gracia expirado";
            return true;
        }
        return false;
    }

    private void clearGraceFields() {
        this.graceUntil = null;
        this.graceMaxOrders = null;
        this.graceOrdersUsed = 0;
        this.graceNotes = null;
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

    SubscriptionStatus getStatus() {
        return status;
    }

    SuspensionMode getSuspensionMode() {
        return suspensionMode;
    }

    SuspensionMode getAutoSuspendModeOnExpire() {
        return autoSuspendModeOnExpire;
    }

    void setAutoSuspendModeOnExpire(SuspensionMode mode, UUID adminId) {
        this.autoSuspendModeOnExpire = mode == SuspensionMode.TOTAL
            ? SuspensionMode.TOTAL
            : SuspensionMode.POS_AND_SHIFT_ONLY;
        this.updatedByAdminId = adminId;
    }

    String getSuspensionReason() {
        return suspensionReason;
    }

    Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    Instant getGraceUntil() {
        return graceUntil;
    }

    Integer getGraceMaxOrders() {
        return graceMaxOrders;
    }

    int getGraceOrdersUsed() {
        return graceOrdersUsed;
    }

    String getGraceNotes() {
        return graceNotes;
    }

    Integer getOverrideMaxBranches() {
        return overrideMaxBranches;
    }

    Integer getOverrideMaxUsers() {
        return overrideMaxUsers;
    }

    Integer getOverrideMaxDishes() {
        return overrideMaxDishes;
    }

    Integer getOverrideMaxInventoryItems() {
        return overrideMaxInventoryItems;
    }

    Integer getOverrideMaxMonthlyOrders() {
        return overrideMaxMonthlyOrders;
    }

    UUID getUpdatedByAdminId() {
        return updatedByAdminId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
