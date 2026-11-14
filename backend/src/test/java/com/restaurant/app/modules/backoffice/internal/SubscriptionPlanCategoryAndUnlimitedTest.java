package com.restaurant.app.modules.backoffice.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanCategoryAndUnlimitedTest {

    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private TenantSubscriptionRepository subscriptionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PlatformAuditService auditService;

    private SubscriptionPlanService planService;

    @BeforeEach
    void setUp() {
        planService = new SubscriptionPlanService(
            planRepository,
            subscriptionRepository,
            eventPublisher,
            auditService
        );
    }

    @Test
    @DisplayName("Permite crear plan gratuito ilimitado con durationDays=0, cuotas null y categoría CUSTOM_PRIVATE")
    void shouldAllowUnlimitedFreeCustomPlan() {
        PlatformPrincipal actor = new PlatformPrincipal(
            UUID.randomUUID(), "superadmin", "sa@platform.local", "SA", PlatformRole.SUPERADMIN, false
        );
        when(planRepository.findByCodeIgnoreCase("VIP_FREE")).thenReturn(Optional.empty());
        when(planRepository.save(any(SubscriptionPlan.class))).thenAnswer(inv -> {
            SubscriptionPlan p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });

        var dto = planService.create(
            new SubscriptionPlanService.CreatePlanRequest(
                "vip_free",
                "Cortesía Ilimitada",
                "Sin expiración",
                BillingCycle.CUSTOM,
                PlanCategory.CUSTOM_PRIVATE,
                0,
                BigDecimal.ZERO,
                "BOB",
                null, null, null, null, null
            ),
            actor,
            "127.0.0.1"
        );

        assertThat(dto.code()).isEqualTo("VIP_FREE");
        assertThat(dto.durationDays()).isZero();
        assertThat(dto.planCategory()).isEqualTo(PlanCategory.CUSTOM_PRIVATE);
        assertThat(dto.maxBranches()).isNull();
        assertThat(dto.maxUsers()).isNull();
    }

    @Test
    @DisplayName("listAll(category) filtra los planes por su PlanCategory")
    void shouldFilterPlansByCategory() {
        SubscriptionPlan vip = new SubscriptionPlan(
            "VIP", "VIP", null, BillingCycle.CUSTOM, PlanCategory.CUSTOM_PRIVATE,
            0, BigDecimal.ZERO, "BOB", null, null, null, null, null
        );
        when(planRepository.findByPlanCategoryOrderByPriceAscCreatedAtAsc(PlanCategory.CUSTOM_PRIVATE))
            .thenReturn(List.of(vip));

        var result = planService.listAll(PlanCategory.CUSTOM_PRIVATE);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().planCategory()).isEqualTo(PlanCategory.CUSTOM_PRIVATE);
        verify(planRepository).findByPlanCategoryOrderByPriceAscCreatedAtAsc(PlanCategory.CUSTOM_PRIVATE);
    }
}
