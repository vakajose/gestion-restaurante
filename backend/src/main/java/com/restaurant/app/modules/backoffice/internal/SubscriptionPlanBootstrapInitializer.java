package com.restaurant.app.modules.backoffice.internal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Semilla idempotente de planes base (garantiza disponibilidad también en perfil H2 de pruebas sin Flyway).
 */
@Component
@Order(1)
class SubscriptionPlanBootstrapInitializer implements ApplicationRunner {

    private final SubscriptionPlanRepository planRepository;

    SubscriptionPlanBootstrapInitializer(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultPlansIfMissing();
    }

    void seedDefaultPlansIfMissing() {
        List<SubscriptionPlan> defaults = List.of(
            new SubscriptionPlan(
                "TRIAL_14D",
                "Prueba Inicial 14 Días",
                "Plan inicial automático para onboarding de nuevos restaurantes",
                BillingCycle.CUSTOM,
                14,
                BigDecimal.ZERO,
                "BOB",
                3, 15, 100, 200, 2000
            ),
            new SubscriptionPlan(
                "WEEKLY_FLEX",
                "Plan Semanal Flexible",
                "Ciclo semanal ideal para operación por adelantado o crédito corto",
                BillingCycle.WEEKLY,
                7,
                new BigDecimal("75.00"),
                "BOB",
                1, 8, 60, 120, 1200
            ),
            new SubscriptionPlan(
                "MONTHLY_STANDARD",
                "Plan Mensual Estándar",
                "Plan mensual para restaurantes con 1-2 sucursales",
                BillingCycle.MONTHLY,
                30,
                new BigDecimal("250.00"),
                "BOB",
                2, 15, 150, 300, 5000
            ),
            new SubscriptionPlan(
                "MONTHLY_PRO",
                "Plan Mensual Multi-Sucursal",
                "Plan ampliado para cadenas y alta demanda POS",
                BillingCycle.MONTHLY,
                30,
                new BigDecimal("550.00"),
                "BOB",
                10, 60, null, null, null
            )
        );

        for (SubscriptionPlan candidate : defaults) {
            if (planRepository.findByCodeIgnoreCase(candidate.getCode()).isEmpty()) {
                planRepository.save(candidate);
            }
        }
    }
}
