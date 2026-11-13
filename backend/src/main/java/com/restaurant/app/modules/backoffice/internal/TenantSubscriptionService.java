package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
import com.restaurant.app.core.entitlements.api.TenantEntitlementChangedEvent;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
class TenantSubscriptionService {

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionChargeRepository chargeRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final CompanyPublicApi companyPublicApi;
    private final SessionPublicApi sessionPublicApi;
    private final TenantEntitlementService entitlementService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformAuditService auditService;

    TenantSubscriptionService(
        TenantSubscriptionRepository subscriptionRepository,
        SubscriptionPlanRepository planRepository,
        SubscriptionChargeRepository chargeRepository,
        SubscriptionPaymentRepository paymentRepository,
        CompanyPublicApi companyPublicApi,
        SessionPublicApi sessionPublicApi,
        TenantEntitlementService entitlementService,
        ApplicationEventPublisher eventPublisher,
        PlatformAuditService auditService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.chargeRepository = chargeRepository;
        this.paymentRepository = paymentRepository;
        this.companyPublicApi = companyPublicApi;
        this.sessionPublicApi = sessionPublicApi;
        this.entitlementService = entitlementService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    record AssignPlanRequest(
        UUID planId,
        String planCode,
        Boolean markAsPaid,
        PlatformPaymentMethod paymentMethod,
        String referenceCode,
        String receiptUrl,
        BigDecimal customAmount,
        Integer customDurationDays,
        Instant periodStart,
        LocalDate dueDate,
        String notes
    ) {
    }

    record UpdateQuotasRequest(
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
    }

    record GrantGraceRequest(
        Instant graceUntil,
        Integer graceMaxOrders,
        String notes
    ) {
    }

    record SuspendTenantRequest(
        SuspensionMode mode,
        String reason
    ) {
    }

    record ReactivateRequest(
        String reason,
        Integer extendDays
    ) {
    }

    record ExpirePolicyRequest(
        SuspensionMode mode
    ) {
    }

    record RecordPaymentRequest(
        BigDecimal amount,
        String currency,
        PlatformPaymentMethod paymentMethod,
        String referenceCode,
        String receiptUrl,
        UUID chargeId,
        String notes,
        Boolean autoReactivateIfSettled
    ) {
    }

    record WaiveChargeRequest(
        String reason
    ) {
    }

    record SubscriptionChargeDto(
        UUID id,
        UUID tenantId,
        UUID planId,
        String planNameSnapshot,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal amountDue,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String currency,
        ChargeStatus chargeStatus,
        LocalDate dueDate,
        String notes,
        Instant createdAt
    ) {
        static SubscriptionChargeDto fromEntity(SubscriptionCharge entity) {
            return new SubscriptionChargeDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getPlanId(),
                entity.getPlanNameSnapshot(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getAmountDue(),
                entity.getPaidAmount(),
                entity.getRemainingAmount(),
                entity.getCurrency(),
                entity.getChargeStatus(),
                entity.getDueDate(),
                entity.getNotes(),
                entity.getCreatedAt()
            );
        }
    }

    record SubscriptionPaymentDto(
        UUID id,
        UUID tenantId,
        UUID chargeId,
        BigDecimal amount,
        String currency,
        PlatformPaymentMethod paymentMethod,
        String referenceCode,
        String receiptUrl,
        Instant paidAt,
        UUID recordedByAdminId,
        String notes,
        Instant createdAt
    ) {
        static SubscriptionPaymentDto fromEntity(SubscriptionPayment entity) {
            return new SubscriptionPaymentDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getChargeId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getPaymentMethod(),
                entity.getReferenceCode(),
                entity.getReceiptUrl(),
                entity.getPaidAt(),
                entity.getRecordedByAdminId(),
                entity.getNotes(),
                entity.getCreatedAt()
            );
        }
    }

    record TenantChargesSummaryDto(
        UUID tenantId,
        BigDecimal totalPendingBalance,
        String currency,
        List<SubscriptionChargeDto> charges
    ) {
    }

    record PaymentRecordResultDto(
        SubscriptionPaymentDto payment,
        BigDecimal remainingPendingBalance,
        List<UUID> allocatedChargeIds
    ) {
    }

    public TenantEntitlementView assignPlan(
        UUID tenantId,
        AssignPlanRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        verifyTenantExists(tenantId);

        SubscriptionPlan plan = resolvePlan(request.planId(), request.planCode());
        int durationDays = (request.customDurationDays() != null && request.customDurationDays() > 0)
            ? request.customDurationDays()
            : plan.getDurationDays();

        BigDecimal amountDue = request.customAmount() != null
            ? request.customAmount().setScale(2, RoundingMode.HALF_UP)
            : plan.getPrice();
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El monto del cargo no puede ser negativo");
        }

        Instant periodStart = request.periodStart() != null ? request.periodStart() : Instant.now();
        Instant periodEnd = periodStart.plus(durationDays, ChronoUnit.DAYS);
        UUID actorId = actor != null ? actor.adminId() : null;

        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseGet(() -> new TenantSubscription(tenantId, plan.getId(), SubscriptionStatus.ACTIVE, periodStart, periodEnd));
        subscription.activatePlanPeriod(plan.getId(), periodStart, periodEnd, SubscriptionStatus.ACTIVE, actorId);
        subscriptionRepository.save(subscription);

        LocalDate dueDate = request.dueDate() != null
            ? request.dueDate()
            : LocalDate.now(ZoneOffset.UTC).plusDays(7);

        SubscriptionCharge charge = new SubscriptionCharge(
            tenantId,
            plan.getId(),
            plan.getName(),
            periodStart,
            periodEnd,
            amountDue,
            BigDecimal.ZERO,
            plan.getCurrency(),
            dueDate,
            request.notes(),
            actorId
        );
        SubscriptionCharge savedCharge = chargeRepository.save(charge);

        boolean paidImmediately = Boolean.TRUE.equals(request.markAsPaid()) && amountDue.compareTo(BigDecimal.ZERO) > 0;
        if (paidImmediately) {
            PlatformPaymentMethod method = request.paymentMethod() != null
                ? request.paymentMethod()
                : PlatformPaymentMethod.TRANSFER;
            savedCharge.applyPayment(amountDue);
            savedCharge = chargeRepository.save(savedCharge);
            paymentRepository.save(new SubscriptionPayment(
                tenantId,
                savedCharge.getId(),
                amountDue,
                plan.getCurrency(),
                method,
                request.referenceCode(),
                request.receiptUrl(),
                Instant.now(),
                actorId,
                request.notes()
            ));
        }

        notifyEntitlementChange(tenantId, "PLAN_ASSIGNED");

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("planCode", plan.getCode());
        details.put("amountDue", amountDue);
        details.put("markAsPaid", paidImmediately);
        details.put("chargeId", savedCharge.getId().toString());
        details.put("chargeStatus", savedCharge.getChargeStatus().name());
        auditService.record(actor, "TENANT_PLAN_ASSIGNED", "TENANT", tenantId.toString(), details, ipAddress);

        return entitlementService.getEntitlementView(tenantId);
    }

    public TenantEntitlementView updateQuotas(
        UUID tenantId,
        UpdateQuotasRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        SubscriptionPlanService.validateNonNegativeLimit("maxBranches", request.maxBranches());
        SubscriptionPlanService.validateNonNegativeLimit("maxUsers", request.maxUsers());
        SubscriptionPlanService.validateNonNegativeLimit("maxDishes", request.maxDishes());
        SubscriptionPlanService.validateNonNegativeLimit("maxInventoryItems", request.maxInventoryItems());
        SubscriptionPlanService.validateNonNegativeLimit("maxMonthlyOrders", request.maxMonthlyOrders());

        TenantSubscription sub = ensureSubscriptionExists(tenantId);
        sub.updateQuotaOverrides(
            request.maxBranches(),
            request.maxUsers(),
            request.maxDishes(),
            request.maxInventoryItems(),
            request.maxMonthlyOrders(),
            actor != null ? actor.adminId() : null
        );
        subscriptionRepository.save(sub);
        notifyEntitlementChange(tenantId, "QUOTAS_OVERRIDDEN");

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("maxBranches", request.maxBranches());
        details.put("maxUsers", request.maxUsers());
        details.put("maxDishes", request.maxDishes());
        details.put("maxInventoryItems", request.maxInventoryItems());
        details.put("maxMonthlyOrders", request.maxMonthlyOrders());
        auditService.record(actor, "TENANT_QUOTAS_UPDATED", "TENANT", tenantId.toString(), details, ipAddress);

        return entitlementService.getEntitlementView(tenantId);
    }

    public TenantEntitlementView grantGrace(
        UUID tenantId,
        GrantGraceRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        boolean hasDate = request.graceUntil() != null;
        boolean hasMaxOrders = request.graceMaxOrders() != null;
        if (!hasDate && !hasMaxOrders) {
            throw new ValidationException("Debe definir al menos graceUntil o graceMaxOrders para otorgar gracia");
        }
        if (hasDate && !request.graceUntil().isAfter(Instant.now())) {
            throw new ValidationException("La fecha de fin de gracia (graceUntil) debe estar en el futuro");
        }
        if (hasMaxOrders && request.graceMaxOrders() <= 0) {
            throw new ValidationException("graceMaxOrders debe ser mayor a 0");
        }

        TenantSubscription sub = ensureSubscriptionExists(tenantId);
        sub.grantConditionalGrace(
            request.graceUntil(),
            request.graceMaxOrders(),
            request.notes(),
            actor != null ? actor.adminId() : null
        );
        subscriptionRepository.save(sub);
        notifyEntitlementChange(tenantId, "GRACE_GRANTED");

        Map<String, Object> details = new LinkedHashMap<>();
        if (request.graceUntil() != null) {
            details.put("graceUntil", request.graceUntil().toString());
        }
        if (request.graceMaxOrders() != null) {
            details.put("graceMaxOrders", request.graceMaxOrders());
        }
        if (request.notes() != null) {
            details.put("notes", request.notes());
        }
        auditService.record(actor, "TENANT_GRACE_GRANTED", "TENANT", tenantId.toString(), details, ipAddress);

        return entitlementService.getEntitlementView(tenantId);
    }

    public TenantEntitlementView suspend(
        UUID tenantId,
        SuspendTenantRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null || request.mode() == null || request.mode() == SuspensionMode.NONE) {
            throw new ValidationException("Debe indicar un modo de suspensión válido: POS_AND_SHIFT_ONLY o TOTAL");
        }
        TenantSubscription sub = ensureSubscriptionExists(tenantId);
        String reason = (request.reason() != null && !request.reason().isBlank())
            ? request.reason().trim()
            : "Suspensión administrativa de plataforma";
        sub.suspend(request.mode(), reason, actor != null ? actor.adminId() : null);
        subscriptionRepository.save(sub);

        int revokedSessions = 0;
        if (request.mode() == SuspensionMode.TOTAL) {
            revokedSessions = sessionPublicApi.revokeAllTenantSessions(tenantId);
        }

        notifyEntitlementChange(tenantId, "TENANT_SUSPENDED_" + request.mode().name());
        auditService.record(
            actor,
            "TENANT_SUSPENDED",
            "TENANT",
            tenantId.toString(),
            Map.of("mode", request.mode().name(), "reason", reason, "revokedSessions", revokedSessions),
            ipAddress
        );

        return entitlementService.getEntitlementView(tenantId);
    }

    public TenantEntitlementView reactivate(
        UUID tenantId,
        ReactivateRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        TenantSubscription sub = ensureSubscriptionExists(tenantId);
        Instant now = Instant.now();
        UUID actorId = actor != null ? actor.adminId() : null;

        if (request != null && request.extendDays() != null && request.extendDays() > 0) {
            Instant baseEnd = (sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().isAfter(now))
                ? sub.getCurrentPeriodEnd()
                : now;
            sub.activatePlanPeriod(
                sub.getPlanId(),
                sub.getCurrentPeriodStart() != null ? sub.getCurrentPeriodStart() : now,
                baseEnd.plus(request.extendDays(), ChronoUnit.DAYS),
                SubscriptionStatus.ACTIVE,
                actorId
            );
        } else {
            sub.reactivate(now, actorId);
            boolean hasActivePeriod = sub.getCurrentPeriodEnd() == null || sub.getCurrentPeriodEnd().isAfter(now);
            boolean hasValidGrace = sub.getStatus() == SubscriptionStatus.GRACE_PERIOD;
            if (!hasActivePeriod && !hasValidGrace) {
                throw new BusinessRuleException(
                    "El período del plan está vencido; asigne un plan nuevo, otorgue gracia o indique extendDays para reactivar"
                );
            }
        }

        subscriptionRepository.save(sub);
        notifyEntitlementChange(tenantId, "TENANT_REACTIVATED");

        auditService.record(
            actor,
            "TENANT_REACTIVATED",
            "TENANT",
            tenantId.toString(),
            Map.of(
                "status", sub.getStatus().name(),
                "reason", request != null && request.reason() != null ? request.reason() : ""
            ),
            ipAddress
        );

        return entitlementService.getEntitlementView(tenantId);
    }

    public TenantEntitlementView updateExpirePolicy(
        UUID tenantId,
        ExpirePolicyRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null || request.mode() == null || request.mode() == SuspensionMode.NONE) {
            throw new ValidationException("Debe indicar POS_AND_SHIFT_ONLY o TOTAL como política automática de vencimiento");
        }
        TenantSubscription sub = ensureSubscriptionExists(tenantId);
        sub.setAutoSuspendModeOnExpire(request.mode(), actor != null ? actor.adminId() : null);
        subscriptionRepository.save(sub);
        notifyEntitlementChange(tenantId, "EXPIRE_POLICY_UPDATED");

        auditService.record(
            actor,
            "TENANT_EXPIRE_POLICY_UPDATED",
            "TENANT",
            tenantId.toString(),
            Map.of("autoSuspendModeOnExpire", request.mode().name()),
            ipAddress
        );
        return entitlementService.getEntitlementView(tenantId);
    }

    public PaymentRecordResultDto recordPayment(
        UUID tenantId,
        RecordPaymentRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("El monto del pago debe ser mayor a 0");
        }
        if (request.paymentMethod() == null) {
            throw new ValidationException("El método de pago es obligatorio");
        }
        verifyTenantExists(tenantId);

        BigDecimal remainingToAllocate = request.amount().setScale(2, RoundingMode.HALF_UP);
        List<UUID> allocatedChargeIds = new ArrayList<>();

        if (request.chargeId() != null) {
            SubscriptionCharge explicitCharge = chargeRepository.findById(request.chargeId())
                .orElseThrow(() -> new EntityNotFoundException("Cargo de suscripción", request.chargeId()));
            if (!explicitCharge.getTenantId().equals(tenantId)) {
                throw new ValidationException("El cargo especificado no pertenece al tenant indicado");
            }
            if (explicitCharge.getChargeStatus() == ChargeStatus.WAIVED) {
                throw new BusinessRuleException("No se puede aplicar un pago a un cargo condonado");
            }
            BigDecimal applied = explicitCharge.applyPayment(remainingToAllocate);
            if (applied.compareTo(BigDecimal.ZERO) > 0) {
                chargeRepository.save(explicitCharge);
                allocatedChargeIds.add(explicitCharge.getId());
                remainingToAllocate = remainingToAllocate.subtract(applied);
            }
        }

        if (remainingToAllocate.compareTo(BigDecimal.ZERO) > 0) {
            List<SubscriptionCharge> openCharges = chargeRepository.findByTenantIdAndChargeStatusInOrderByCreatedAtAsc(
                tenantId,
                List.of(ChargeStatus.PENDING, ChargeStatus.PARTIAL)
            );
            for (SubscriptionCharge openCharge : openCharges) {
                if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal applied = openCharge.applyPayment(remainingToAllocate);
                if (applied.compareTo(BigDecimal.ZERO) > 0) {
                    chargeRepository.save(openCharge);
                    if (!allocatedChargeIds.contains(openCharge.getId())) {
                        allocatedChargeIds.add(openCharge.getId());
                    }
                    remainingToAllocate = remainingToAllocate.subtract(applied);
                }
            }
        }

        UUID primaryChargeId = !allocatedChargeIds.isEmpty() ? allocatedChargeIds.get(0) : request.chargeId();
        UUID actorId = actor != null ? actor.adminId() : null;
        String currency = (request.currency() != null && !request.currency().isBlank())
            ? request.currency().trim().toUpperCase()
            : "BOB";

        SubscriptionPayment savedPayment = paymentRepository.save(new SubscriptionPayment(
            tenantId,
            primaryChargeId,
            request.amount().setScale(2, RoundingMode.HALF_UP),
            currency,
            request.paymentMethod(),
            request.referenceCode(),
            request.receiptUrl(),
            Instant.now(),
            actorId,
            request.notes()
        ));

        BigDecimal remainingDebt = chargeRepository.sumPendingBalanceByTenantId(tenantId);

        if (Boolean.TRUE.equals(request.autoReactivateIfSettled()) && remainingDebt.compareTo(BigDecimal.ZERO) == 0) {
            subscriptionRepository.findByTenantId(tenantId).ifPresent(sub -> {
                if (sub.getStatus() == SubscriptionStatus.SUSPENDED) {
                    sub.reactivate(Instant.now(), actorId);
                    subscriptionRepository.save(sub);
                }
            });
        }

        notifyEntitlementChange(tenantId, "PAYMENT_RECORDED");

        auditService.record(
            actor,
            "SUBSCRIPTION_PAYMENT_RECORDED",
            "TENANT",
            tenantId.toString(),
            Map.of(
                "paymentId", savedPayment.getId().toString(),
                "amount", savedPayment.getAmount(),
                "method", savedPayment.getPaymentMethod().name(),
                "allocatedChargeIds", allocatedChargeIds.stream().map(UUID::toString).toList(),
                "remainingPendingBalance", remainingDebt
            ),
            ipAddress
        );

        return new PaymentRecordResultDto(
            SubscriptionPaymentDto.fromEntity(savedPayment),
            remainingDebt,
            allocatedChargeIds
        );
    }

    public SubscriptionChargeDto waiveCharge(
        UUID chargeId,
        WaiveChargeRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        SubscriptionCharge charge = chargeRepository.findById(chargeId)
            .orElseThrow(() -> new EntityNotFoundException("Cargo de suscripción", chargeId));

        if (charge.getChargeStatus() == ChargeStatus.PAID) {
            throw new BusinessRuleException("No se puede condonar un cargo que ya fue pagado totalmente");
        }
        if (charge.getChargeStatus() == ChargeStatus.WAIVED) {
            throw new BusinessRuleException("El cargo ya se encuentra condonado");
        }

        String reason = request != null ? request.reason() : "Condonación administrativa";
        charge.waive(reason);
        SubscriptionCharge saved = chargeRepository.save(charge);

        notifyEntitlementChange(saved.getTenantId(), "CHARGE_WAIVED");
        auditService.record(
            actor,
            "SUBSCRIPTION_CHARGE_WAIVED",
            "SUBSCRIPTION_CHARGE",
            saved.getId().toString(),
            Map.of("tenantId", saved.getTenantId().toString(), "reason", reason != null ? reason : ""),
            ipAddress
        );
        return SubscriptionChargeDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TenantChargesSummaryDto listCharges(UUID tenantId) {
        verifyTenantExists(tenantId);
        List<SubscriptionChargeDto> charges = chargeRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(SubscriptionChargeDto::fromEntity)
            .toList();
        BigDecimal totalPending = chargeRepository.sumPendingBalanceByTenantId(tenantId);
        String currency = charges.isEmpty() ? "BOB" : charges.get(0).currency();
        return new TenantChargesSummaryDto(tenantId, totalPending, currency, charges);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPaymentDto> listPayments(UUID tenantId) {
        verifyTenantExists(tenantId);
        return paymentRepository.findByTenantIdOrderByPaidAtDesc(tenantId)
            .stream()
            .map(SubscriptionPaymentDto::fromEntity)
            .toList();
    }

    TenantSubscription ensureSubscriptionExists(UUID tenantId) {
        verifyTenantExists(tenantId);
        return subscriptionRepository.findByTenantId(tenantId).orElseGet(() -> {
            SubscriptionPlan defaultPlan = planRepository.findByCodeIgnoreCase("TRIAL_14D").orElse(null);
            Instant now = Instant.now();
            Instant end = defaultPlan != null
                ? now.plus(defaultPlan.getDurationDays(), ChronoUnit.DAYS)
                : now.plus(30, ChronoUnit.DAYS);
            SubscriptionStatus initialStatus = defaultPlan != null
                ? SubscriptionStatus.TRIAL
                : SubscriptionStatus.ACTIVE;
            return subscriptionRepository.save(new TenantSubscription(
                tenantId,
                defaultPlan != null ? defaultPlan.getId() : null,
                initialStatus,
                now,
                end
            ));
        });
    }

    private SubscriptionPlan resolvePlan(UUID planId, String planCode) {
        if (planId != null) {
            return planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan de suscripción", planId));
        }
        if (planCode != null && !planCode.isBlank()) {
            return planRepository.findByCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new EntityNotFoundException("Plan de suscripción no encontrado con código: " + planCode));
        }
        throw new ValidationException("Debe indicar planId o planCode");
    }

    private void verifyTenantExists(UUID tenantId) {
        companyPublicApi.getCompany(tenantId);
    }

    private void notifyEntitlementChange(UUID tenantId, String reason) {
        entitlementService.evict(tenantId);
        eventPublisher.publishEvent(new TenantEntitlementChangedEvent(tenantId));
    }
}
