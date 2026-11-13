package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.entitlements.api.TenantEntitlementChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformAuditService auditService;

    SubscriptionPlanService(
        SubscriptionPlanRepository planRepository,
        TenantSubscriptionRepository subscriptionRepository,
        ApplicationEventPublisher eventPublisher,
        PlatformAuditService auditService
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    record SubscriptionPlanDto(
        UUID id,
        String code,
        String name,
        String description,
        BillingCycle billingCycle,
        int durationDays,
        BigDecimal price,
        String currency,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
    ) {
        static SubscriptionPlanDto fromEntity(SubscriptionPlan plan) {
            return new SubscriptionPlanDto(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getBillingCycle(),
                plan.getDurationDays(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getMaxBranches(),
                plan.getMaxUsers(),
                plan.getMaxDishes(),
                plan.getMaxInventoryItems(),
                plan.getMaxMonthlyOrders(),
                plan.isActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
            );
        }
    }

    record CreatePlanRequest(
        String code,
        String name,
        String description,
        BillingCycle billingCycle,
        Integer durationDays,
        BigDecimal price,
        String currency,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
    }

    record UpdatePlanRequest(
        String name,
        String description,
        BillingCycle billingCycle,
        Integer durationDays,
        BigDecimal price,
        String currency,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
    }

    record PlanStatusRequest(
        Boolean isActive,
        Boolean active
    ) {
        boolean resolvedActive() {
            if (isActive != null) {
                return isActive;
            }
            if (active != null) {
                return active;
            }
            throw new ValidationException("Debe indicar el campo isActive");
        }
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanDto> listAll() {
        return planRepository.findAllByOrderByPriceAscCreatedAtAsc()
            .stream()
            .map(SubscriptionPlanDto::fromEntity)
            .toList();
    }

    public SubscriptionPlanDto create(CreatePlanRequest request, PlatformPrincipal actor, String ipAddress) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            throw new ValidationException("El código del plan es obligatorio");
        }
        String normalizedCode = request.code().trim().toUpperCase();
        if (planRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new ResourceConflictException("Ya existe un plan con código '" + normalizedCode + "'");
        }

        validateCommonPlanFields(
            request.name(),
            request.durationDays(),
            request.price(),
            request.maxBranches(),
            request.maxUsers(),
            request.maxDishes(),
            request.maxInventoryItems(),
            request.maxMonthlyOrders()
        );

        SubscriptionPlan plan = new SubscriptionPlan(
            normalizedCode,
            request.name(),
            request.description(),
            request.billingCycle(),
            request.durationDays(),
            request.price().setScale(2, RoundingMode.HALF_UP),
            request.currency(),
            request.maxBranches(),
            request.maxUsers(),
            request.maxDishes(),
            request.maxInventoryItems(),
            request.maxMonthlyOrders()
        );
        SubscriptionPlan saved = planRepository.save(plan);

        auditService.record(
            actor,
            "SUBSCRIPTION_PLAN_CREATED",
            "SUBSCRIPTION_PLAN",
            saved.getId().toString(),
            Map.of("code", saved.getCode(), "price", saved.getPrice(), "durationDays", saved.getDurationDays()),
            ipAddress
        );
        return SubscriptionPlanDto.fromEntity(saved);
    }

    public SubscriptionPlanDto update(UUID planId, UpdatePlanRequest request, PlatformPrincipal actor, String ipAddress) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        SubscriptionPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new EntityNotFoundException("Plan de suscripción", planId));

        validateCommonPlanFields(
            request.name(),
            request.durationDays(),
            request.price(),
            request.maxBranches(),
            request.maxUsers(),
            request.maxDishes(),
            request.maxInventoryItems(),
            request.maxMonthlyOrders()
        );

        plan.updateDetails(
            request.name(),
            request.description(),
            request.billingCycle(),
            request.durationDays(),
            request.price().setScale(2, RoundingMode.HALF_UP),
            request.currency(),
            request.maxBranches(),
            request.maxUsers(),
            request.maxDishes(),
            request.maxInventoryItems(),
            request.maxMonthlyOrders()
        );
        SubscriptionPlan saved = planRepository.save(plan);

        // Invalidar cachés de tenants vinculados a este plan
        subscriptionRepository.findAll().stream()
            .filter(sub -> planId.equals(sub.getPlanId()))
            .forEach(sub -> eventPublisher.publishEvent(new TenantEntitlementChangedEvent(sub.getTenantId())));

        auditService.record(
            actor,
            "SUBSCRIPTION_PLAN_UPDATED",
            "SUBSCRIPTION_PLAN",
            saved.getId().toString(),
            Map.of("code", saved.getCode(), "price", saved.getPrice(), "durationDays", saved.getDurationDays()),
            ipAddress
        );
        return SubscriptionPlanDto.fromEntity(saved);
    }

    public SubscriptionPlanDto updateStatus(
        UUID planId,
        PlanStatusRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        boolean targetActive = request.resolvedActive();
        SubscriptionPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new EntityNotFoundException("Plan de suscripción", planId));

        plan.setActive(targetActive);
        SubscriptionPlan saved = planRepository.save(plan);

        auditService.record(
            actor,
            targetActive ? "SUBSCRIPTION_PLAN_ACTIVATED" : "SUBSCRIPTION_PLAN_DEACTIVATED",
            "SUBSCRIPTION_PLAN",
            saved.getId().toString(),
            Map.of("code", saved.getCode(), "isActive", targetActive),
            ipAddress
        );
        return SubscriptionPlanDto.fromEntity(saved);
    }

    private void validateCommonPlanFields(
        String name,
        Integer durationDays,
        BigDecimal price,
        Integer maxBranches,
        Integer maxUsers,
        Integer maxDishes,
        Integer maxInventoryItems,
        Integer maxMonthlyOrders
    ) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("El nombre del plan es obligatorio");
        }
        if (durationDays == null || durationDays <= 0) {
            throw new ValidationException("La duración del plan (durationDays) debe ser mayor a 0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El precio del plan no puede ser negativo");
        }
        validateNonNegativeLimit("maxBranches", maxBranches);
        validateNonNegativeLimit("maxUsers", maxUsers);
        validateNonNegativeLimit("maxDishes", maxDishes);
        validateNonNegativeLimit("maxInventoryItems", maxInventoryItems);
        validateNonNegativeLimit("maxMonthlyOrders", maxMonthlyOrders);
    }

    static void validateNonNegativeLimit(String field, Integer value) {
        if (value != null && value < 0) {
            throw new ValidationException("El límite '" + field + "' no puede ser negativo");
        }
    }
}
