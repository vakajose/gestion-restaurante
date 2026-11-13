package com.restaurant.app.core.entitlements.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Instantánea cacheable de estado de suscripción, bloqueo y cuotas efectivas de un tenant.
 */
public record TenantEntitlementSnapshot(
    UUID tenantId,
    boolean tenantActive,
    boolean managedSubscription,
    SubscriptionStatus status,
    SuspensionMode storedSuspensionMode,
    SuspensionMode autoSuspendModeOnExpire,
    String suspensionReason,
    String planCode,
    String planName,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Instant graceUntil,
    Integer graceMaxOrders,
    int graceOrdersUsed,
    BigDecimal pendingBalance,
    String currency,
    Map<QuotaResource, Integer> quotas
) {
    public TenantEntitlementSnapshot {
        Map<QuotaResource, Integer> copy = new EnumMap<>(QuotaResource.class);
        if (quotas != null) {
            copy.putAll(quotas);
        }
        quotas = Collections.unmodifiableMap(copy);
        pendingBalance = pendingBalance != null ? pendingBalance : BigDecimal.ZERO;
        currency = (currency != null && !currency.isBlank()) ? currency : "BOB";
        storedSuspensionMode = storedSuspensionMode != null ? storedSuspensionMode : SuspensionMode.NONE;
        autoSuspendModeOnExpire = autoSuspendModeOnExpire != null
            ? autoSuspendModeOnExpire
            : SuspensionMode.POS_AND_SHIFT_ONLY;
    }

    public static TenantEntitlementSnapshot unrestricted(UUID tenantId, boolean tenantActive) {
        return new TenantEntitlementSnapshot(
            tenantId,
            tenantActive,
            false,
            tenantActive ? SubscriptionStatus.ACTIVE : SubscriptionStatus.SUSPENDED,
            tenantActive ? SuspensionMode.NONE : SuspensionMode.TOTAL,
            SuspensionMode.POS_AND_SHIFT_ONLY,
            tenantActive ? null : "Empresa desactivada",
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            BigDecimal.ZERO,
            "BOB",
            Map.of()
        );
    }

    public SuspensionMode resolveEffectiveSuspensionMode(Instant now) {
        if (!tenantActive) {
            return SuspensionMode.TOTAL;
        }
        if (!managedSubscription) {
            return SuspensionMode.NONE;
        }
        if (status == SubscriptionStatus.CANCELLED) {
            return SuspensionMode.TOTAL;
        }
        if (status == SubscriptionStatus.SUSPENDED) {
            return storedSuspensionMode != SuspensionMode.NONE
                ? storedSuspensionMode
                : autoSuspendModeOnExpire;
        }
        if (status == SubscriptionStatus.GRACE_PERIOD) {
            boolean timeExpired = graceUntil != null && !graceUntil.isAfter(now);
            boolean salesExpired = graceMaxOrders != null && graceOrdersUsed >= graceMaxOrders;
            boolean unconfiguredGrace = graceUntil == null && graceMaxOrders == null;
            if (timeExpired || salesExpired || unconfiguredGrace) {
                return autoSuspendModeOnExpire;
            }
            return SuspensionMode.NONE;
        }
        if (currentPeriodEnd != null && !currentPeriodEnd.isAfter(now)) {
            return autoSuspendModeOnExpire;
        }
        return SuspensionMode.NONE;
    }

    public Integer limitFor(QuotaResource resource) {
        return quotas.get(resource);
    }

    public boolean hasBranchOrUserLimits() {
        return limitFor(QuotaResource.BRANCHES) != null || limitFor(QuotaResource.USERS) != null;
    }
}
