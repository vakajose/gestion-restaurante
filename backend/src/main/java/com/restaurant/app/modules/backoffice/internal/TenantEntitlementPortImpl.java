package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.TenantEntitlementChangedEvent;
import com.restaurant.app.core.entitlements.api.TenantEntitlementPort;
import com.restaurant.app.core.entitlements.api.TenantEntitlementSnapshot;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import com.restaurant.app.modules.company.api.TenantOnboardedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto SPI de core.entitlements dentro de modules.backoffice
 * para mantener cero dependencias circulares en Spring Modulith (ADR-0029).
 */
@Component
class TenantEntitlementPortImpl implements TenantEntitlementPort {

    private static final Logger log = LoggerFactory.getLogger(TenantEntitlementPortImpl.class);

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionChargeRepository chargeRepository;
    private final CompanyPublicApi companyPublicApi;
    private final TenantUsageSnapshotReader usageReader;
    private final ApplicationEventPublisher eventPublisher;

    TenantEntitlementPortImpl(
        TenantSubscriptionRepository subscriptionRepository,
        SubscriptionPlanRepository planRepository,
        SubscriptionChargeRepository chargeRepository,
        CompanyPublicApi companyPublicApi,
        TenantUsageSnapshotReader usageReader,
        ApplicationEventPublisher eventPublisher
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.chargeRepository = chargeRepository;
        this.companyPublicApi = companyPublicApi;
        this.usageReader = usageReader;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantEntitlementSnapshot> loadSnapshot(UUID tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        Optional<TenantSubscription> subOpt = subscriptionRepository.findByTenantId(tenantId);
        if (subOpt.isEmpty()) {
            return Optional.empty();
        }

        TenantSubscription sub = subOpt.get();
        boolean tenantActive = companyPublicApi.findCompanyById(tenantId)
            .map(TenantDto::isActive)
            .orElse(true);

        SubscriptionPlan plan = sub.getPlanId() != null
            ? planRepository.findById(sub.getPlanId()).orElse(null)
            : null;

        BigDecimal pendingBalance = chargeRepository.sumPendingBalanceByTenantId(tenantId);
        String currency = plan != null ? plan.getCurrency() : "BOB";

        return Optional.of(new TenantEntitlementSnapshot(
            tenantId,
            tenantActive,
            true,
            sub.getStatus(),
            sub.getSuspensionMode(),
            sub.getAutoSuspendModeOnExpire(),
            sub.getSuspensionReason(),
            plan != null ? plan.getCode() : null,
            plan != null ? plan.getName() : null,
            sub.getCurrentPeriodStart(),
            sub.getCurrentPeriodEnd(),
            sub.getGraceUntil(),
            sub.getGraceMaxOrders(),
            sub.getGraceOrdersUsed(),
            pendingBalance,
            currency,
            sub.resolveEffectiveQuotas(plan)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuotaResource, Long> loadUsageCounts(UUID tenantId) {
        return usageReader.loadQuotaUsageCounts(tenantId);
    }

    @EventListener
    @Transactional
    public void onTenantOnboarded(TenantOnboardedEvent event) {
        if (event == null || event.tenantId() == null) {
            return;
        }
        if (subscriptionRepository.findByTenantId(event.tenantId()).isPresent()) {
            return;
        }
        planRepository.findByCodeIgnoreCase("TRIAL_14D").ifPresentOrElse(trialPlan -> {
            Instant now = Instant.now();
            TenantSubscription sub = new TenantSubscription(
                event.tenantId(),
                trialPlan.getId(),
                SubscriptionStatus.TRIAL,
                now,
                now.plus(trialPlan.getDurationDays(), ChronoUnit.DAYS)
            );
            subscriptionRepository.save(sub);
            eventPublisher.publishEvent(new TenantEntitlementChangedEvent(event.tenantId()));
            log.info("Suscripción TRIAL_14D aprovisionada automáticamente para tenant={}", event.tenantId());
        }, () -> log.warn("Plan base TRIAL_14D no encontrado al onboardear tenant={}", event.tenantId()));
    }
}
