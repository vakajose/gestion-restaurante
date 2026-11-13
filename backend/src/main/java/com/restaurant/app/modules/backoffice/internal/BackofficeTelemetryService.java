package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.TenantEntitlementChangedEvent;
import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
class BackofficeTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeTelemetryService.class);

    private final TenantDailyTelemetryRepository telemetryRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantUsageSnapshotReader usageReader;
    private final PlatformAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    BackofficeTelemetryService(
        TenantDailyTelemetryRepository telemetryRepository,
        TenantSubscriptionRepository subscriptionRepository,
        TenantUsageSnapshotReader usageReader,
        PlatformAuditService auditService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.telemetryRepository = telemetryRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.usageReader = usageReader;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void recordPaidTelemetry(OrderPaidEvent event) {
        if (event == null || event.tenantId() == null || event.branchId() == null) {
            return;
        }
        Instant eventAt = event.paidAt() != null ? event.paidAt() : Instant.now();
        ZoneId branchZone = usageReader.resolveBranchZone(event.branchId());
        LocalDate statDate = LocalDate.ofInstant(eventAt, branchZone);

        TenantDailyTelemetry row = telemetryRepository
            .findByTenantIdAndBranchIdAndStatDate(event.tenantId(), event.branchId(), statDate)
            .orElseGet(() -> new TenantDailyTelemetry(event.tenantId(), event.branchId(), statDate));

        row.recordPaidOrder(event.totalAmount(), eventAt);
        telemetryRepository.save(row);
    }

    @Transactional
    public void recordCancelledTelemetry(OrderCancelledEvent event) {
        if (event == null || event.tenantId() == null || event.branchId() == null) {
            return;
        }
        Instant eventAt = event.cancelledAt() != null ? event.cancelledAt() : Instant.now();
        ZoneId branchZone = usageReader.resolveBranchZone(event.branchId());
        LocalDate statDate = LocalDate.ofInstant(eventAt, branchZone);

        TenantDailyTelemetry row = telemetryRepository
            .findByTenantIdAndBranchIdAndStatDate(event.tenantId(), event.branchId(), statDate)
            .orElseGet(() -> new TenantDailyTelemetry(event.tenantId(), event.branchId(), statDate));

        row.recordCancelledOrder(eventAt);
        telemetryRepository.save(row);
    }

    @Transactional
    public void incrementGraceOrderIfNeeded(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        subscriptionRepository.findByTenantId(tenantId).ifPresent(sub -> {
            if (sub.getStatus() != SubscriptionStatus.GRACE_PERIOD) {
                return;
            }
            boolean autoSuspended = sub.incrementGraceOrderAndCheckAutoSuspend(Instant.now());
            subscriptionRepository.save(sub);

            if (autoSuspended) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("graceOrdersUsed", sub.getGraceOrdersUsed());
                if (sub.getGraceMaxOrders() != null) {
                    details.put("graceMaxOrders", sub.getGraceMaxOrders());
                }
                details.put("suspensionMode", sub.getSuspensionMode().name());
                auditService.recordSystem(
                    "AUTO_SUSPENDED_GRACE_EXHAUSTED",
                    "TENANT",
                    tenantId.toString(),
                    details
                );
                eventPublisher.publishEvent(new TenantEntitlementChangedEvent(tenantId));
                log.info("Tenant {} auto-suspendido ({}) al agotar gracia condicional", tenantId, sub.getSuspensionMode());
            } else {
                eventPublisher.publishEvent(new TenantEntitlementChangedEvent(tenantId));
            }
        });
    }
}
