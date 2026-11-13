package com.restaurant.app.core.entitlements.internal;

import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.entitlements.api.EntitlementBlockedException;
import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
import com.restaurant.app.core.entitlements.api.TenantEntitlementChangedEvent;
import com.restaurant.app.core.entitlements.api.TenantEntitlementPort;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.entitlements.api.TenantEntitlementSnapshot;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView.ResourceQuotaUsage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
class TenantEntitlementServiceImpl implements TenantEntitlementService {

    private final ObjectProvider<TenantEntitlementPort> entitlementPortProvider;
    private final TenantRepository tenantRepository;
    private final ConcurrentHashMap<UUID, TenantEntitlementSnapshot> snapshotCache = new ConcurrentHashMap<>();

    TenantEntitlementServiceImpl(
        ObjectProvider<TenantEntitlementPort> entitlementPortProvider,
        TenantRepository tenantRepository
    ) {
        this.entitlementPortProvider = entitlementPortProvider;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public TenantEntitlementView getEntitlementView(UUID tenantId) {
        TenantEntitlementSnapshot snapshot = getSnapshot(tenantId);
        SuspensionMode effective = snapshot.resolveEffectiveSuspensionMode(Instant.now());

        Map<QuotaResource, Long> usageCounts = loadUsageSafe(tenantId);
        Map<QuotaResource, ResourceQuotaUsage> quotas = new EnumMap<>(QuotaResource.class);
        List<QuotaResource> overQuotaResources = new ArrayList<>();

        for (QuotaResource resource : QuotaResource.values()) {
            long used = usageCounts.getOrDefault(resource, 0L);
            Integer limit = snapshot.limitFor(resource);
            boolean overQuota = limit != null && used > limit;
            if (overQuota) {
                overQuotaResources.add(resource);
            }
            quotas.put(resource, new ResourceQuotaUsage(used, limit, overQuota));
        }

        boolean loginAllowed = effective != SuspensionMode.TOTAL;
        boolean hasBlockingDowngradeExcess = overQuotaResources.contains(QuotaResource.BRANCHES)
            || overQuotaResources.contains(QuotaResource.USERS);
        boolean posAllowed = effective == SuspensionMode.NONE && !hasBlockingDowngradeExcess;

        return new TenantEntitlementView(
            tenantId,
            snapshot.managedSubscription(),
            snapshot.status(),
            effective,
            snapshot.suspensionReason(),
            snapshot.planCode(),
            snapshot.planName(),
            snapshot.currentPeriodStart(),
            snapshot.currentPeriodEnd(),
            snapshot.graceUntil(),
            snapshot.graceMaxOrders(),
            snapshot.graceOrdersUsed(),
            snapshot.pendingBalance(),
            snapshot.currency(),
            loginAllowed,
            posAllowed,
            quotas,
            List.copyOf(overQuotaResources)
        );
    }

    @Override
    public void assertLoginAllowed(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        TenantEntitlementSnapshot snapshot = getSnapshot(tenantId);
        SuspensionMode effective = snapshot.resolveEffectiveSuspensionMode(Instant.now());
        if (effective == SuspensionMode.TOTAL) {
            throw EntitlementBlockedException.totalSuspension(snapshot.suspensionReason());
        }
    }

    @Override
    public void assertPosAndShiftOperationsAllowed(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        TenantEntitlementSnapshot snapshot = getSnapshot(tenantId);
        SuspensionMode effective = snapshot.resolveEffectiveSuspensionMode(Instant.now());
        if (effective == SuspensionMode.TOTAL) {
            throw EntitlementBlockedException.totalSuspension(snapshot.suspensionReason());
        }
        if (effective == SuspensionMode.POS_AND_SHIFT_ONLY) {
            throw EntitlementBlockedException.operationsSuspended(snapshot.suspensionReason());
        }

        if (snapshot.managedSubscription() && snapshot.hasBranchOrUserLimits()) {
            Map<QuotaResource, Long> usageCounts = loadUsageSafe(tenantId);
            List<QuotaResource> excess = new ArrayList<>();
            checkExcess(snapshot, usageCounts, QuotaResource.BRANCHES, excess);
            checkExcess(snapshot, usageCounts, QuotaResource.USERS, excess);
            if (!excess.isEmpty()) {
                throw EntitlementBlockedException.overQuotaResolutionRequired(excess);
            }
        }
    }

    @Override
    public void assertQuotaAvailable(UUID tenantId, QuotaResource resource, long targetCountAfterAction) {
        if (tenantId == null) {
            return;
        }
        TenantEntitlementSnapshot snapshot = getSnapshot(tenantId);
        if (!snapshot.managedSubscription()) {
            return;
        }
        SuspensionMode effective = snapshot.resolveEffectiveSuspensionMode(Instant.now());
        if (effective == SuspensionMode.TOTAL) {
            throw EntitlementBlockedException.totalSuspension(snapshot.suspensionReason());
        }
        if (resource == QuotaResource.MONTHLY_ORDERS && effective == SuspensionMode.POS_AND_SHIFT_ONLY) {
            throw EntitlementBlockedException.operationsSuspended(snapshot.suspensionReason());
        }

        Integer limit = snapshot.limitFor(resource);
        if (limit != null && targetCountAfterAction > limit) {
            throw EntitlementBlockedException.quotaExceeded(resource, targetCountAfterAction, limit);
        }
    }

    @Override
    public void evict(UUID tenantId) {
        if (tenantId != null) {
            snapshotCache.remove(tenantId);
        }
    }

    @EventListener
    void onEntitlementChanged(TenantEntitlementChangedEvent event) {
        if (event != null && event.tenantId() != null) {
            snapshotCache.remove(event.tenantId());
        }
    }

    private TenantEntitlementSnapshot getSnapshot(UUID tenantId) {
        return snapshotCache.computeIfAbsent(tenantId, id -> {
            TenantEntitlementPort port = entitlementPortProvider.getIfAvailable();
            if (port != null) {
                var loaded = port.loadSnapshot(id);
                if (loaded.isPresent()) {
                    return loaded.get();
                }
            }
            boolean tenantActive = tenantRepository.findById(id)
                .map(t -> t.isActive())
                .orElse(true);
            return TenantEntitlementSnapshot.unrestricted(id, tenantActive);
        });
    }

    private Map<QuotaResource, Long> loadUsageSafe(UUID tenantId) {
        TenantEntitlementPort port = entitlementPortProvider.getIfAvailable();
        return port != null ? port.loadUsageCounts(tenantId) : Map.of();
    }

    private void checkExcess(
        TenantEntitlementSnapshot snapshot,
        Map<QuotaResource, Long> usageCounts,
        QuotaResource resource,
        List<QuotaResource> excess
    ) {
        Integer limit = snapshot.limitFor(resource);
        if (limit != null && usageCounts.getOrDefault(resource, 0L) > limit) {
            excess.add(resource);
        }
    }
}
