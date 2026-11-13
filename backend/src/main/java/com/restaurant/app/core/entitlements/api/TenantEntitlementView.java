package com.restaurant.app.core.entitlements.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proyección de estado de suscripción, saldos pendientes y uso vs cuotas para consumo de Backoffice y ERP.
 */
public record TenantEntitlementView(
    UUID tenantId,
    boolean managedSubscription,
    SubscriptionStatus status,
    SuspensionMode effectiveSuspensionMode,
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
    boolean loginAllowed,
    boolean posOperationsAllowed,
    Map<QuotaResource, ResourceQuotaUsage> quotas,
    List<QuotaResource> overQuotaResources
) {
    public record ResourceQuotaUsage(
        long used,
        Integer limit,
        boolean overQuota
    ) {
    }
}
