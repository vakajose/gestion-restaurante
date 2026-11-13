package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.entitlements.api.TenantEntitlementSnapshot;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.modules.backoffice.internal.PlatformAdminManagementService.CreatePlatformAdminRequest;
import com.restaurant.app.modules.backoffice.internal.PlatformAdminManagementService.UpdateAdminStatusRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.AssignPlanRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.PaymentRecordResultDto;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.ReactivateRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.RecordPaymentRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.SuspendTenantRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.WaiveChargeRequest;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPlanAndLedgerServiceTest {

    @Mock
    private TenantSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository planRepository;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private SubscriptionPaymentRepository paymentRepository;
    @Mock
    private CompanyPublicApi companyPublicApi;
    @Mock
    private SessionPublicApi sessionPublicApi;
    @Mock
    private TenantEntitlementService entitlementService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PlatformAuditService auditService;
    @Mock
    private PlatformAdminRepository adminRepository;
    @Mock
    private PlatformRefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private TenantSubscriptionService subscriptionService;
    private PlatformAdminManagementService adminManagementService;

    private UUID adminId;
    private UUID tenantId;
    private PlatformPrincipal superadminActor;

    @BeforeEach
    void setUp() {
        subscriptionService = new TenantSubscriptionService(
            subscriptionRepository,
            planRepository,
            chargeRepository,
            paymentRepository,
            companyPublicApi,
            sessionPublicApi,
            entitlementService,
            eventPublisher,
            auditService
        );
        adminManagementService = new PlatformAdminManagementService(
            adminRepository,
            refreshTokenRepository,
            passwordEncoder,
            auditService
        );

        adminId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        superadminActor = new PlatformPrincipal(
            adminId,
            "superadmin",
            "superadmin@platform.local",
            "Super Admin",
            PlatformRole.SUPERADMIN,
            false
        );

        lenient().when(companyPublicApi.getCompany(tenantId)).thenReturn(
            new TenantDto(tenantId, "Restaurante Demo", "123456", "demo", true, Instant.now(), Instant.now())
        );
    }

    @Test
    @DisplayName("Snapshot de entitlements: evaluación temporal, gracia condicional y dos modos de suspensión")
    void snapshotShouldEvaluateExpiryGraceAndSuspensionModes() {
        Instant now = Instant.now();

        // 1. Período vencido sin gracia aplica autoSuspendModeOnExpire
        TenantEntitlementSnapshot expiredPosBlock = new TenantEntitlementSnapshot(
            tenantId, true, true, SubscriptionStatus.ACTIVE, SuspensionMode.NONE,
            SuspensionMode.POS_AND_SHIFT_ONLY, null, "WEEKLY", "Semanal",
            now.minus(10, ChronoUnit.DAYS), now.minus(1, ChronoUnit.HOURS),
            null, null, 0, BigDecimal.ZERO, "BOB", Map.of()
        );
        assertThat(expiredPosBlock.resolveEffectiveSuspensionMode(now)).isEqualTo(SuspensionMode.POS_AND_SHIFT_ONLY);

        // 2. Gracia activa por fecha y ventas habilita acceso NONE
        TenantEntitlementSnapshot graceActive = new TenantEntitlementSnapshot(
            tenantId, true, true, SubscriptionStatus.GRACE_PERIOD, SuspensionMode.NONE,
            SuspensionMode.TOTAL, null, "WEEKLY", "Semanal",
            now.minus(10, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS),
            now.plus(3, ChronoUnit.DAYS), 10, 4, new BigDecimal("75.00"), "BOB", Map.of()
        );
        assertThat(graceActive.resolveEffectiveSuspensionMode(now)).isEqualTo(SuspensionMode.NONE);

        // 3. Al alcanzar grace_max_orders entra autoSuspendModeOnExpire (TOTAL)
        TenantEntitlementSnapshot graceSalesExhausted = new TenantEntitlementSnapshot(
            tenantId, true, true, SubscriptionStatus.GRACE_PERIOD, SuspensionMode.NONE,
            SuspensionMode.TOTAL, null, "WEEKLY", "Semanal",
            now.minus(10, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS),
            now.plus(3, ChronoUnit.DAYS), 10, 10, new BigDecimal("75.00"), "BOB", Map.of()
        );
        assertThat(graceSalesExhausted.resolveEffectiveSuspensionMode(now)).isEqualTo(SuspensionMode.TOTAL);
    }

    @Test
    @DisplayName("Uso por adelantado: asignar plan con markAsPaid=false activa período y genera cargo PENDING")
    void assignPlanUnpaidShouldActivatePeriodAndCreatePendingCharge() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlan weekly = new SubscriptionPlan(
            "WEEKLY_FLEX", "Semanal", "Desc", BillingCycle.WEEKLY,
            7, new BigDecimal("75.00"), "BOB", 1, 8, 60, 120, 1200
        );
        ReflectionTestUtils.setField(weekly, "id", planId);

        when(planRepository.findById(planId)).thenReturn(Optional.of(weekly));
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(chargeRepository.save(any())).thenAnswer(inv -> {
            SubscriptionCharge c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
        when(entitlementService.getEntitlementView(tenantId)).thenReturn(
            new TenantEntitlementView(
                tenantId, true, SubscriptionStatus.ACTIVE, SuspensionMode.NONE, null,
                "WEEKLY_FLEX", "Semanal", Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS),
                null, null, 0, new BigDecimal("75.00"), "BOB", true, true, Map.of(), List.of()
            )
        );

        TenantEntitlementView view = subscriptionService.assignPlan(
            tenantId,
            new AssignPlanRequest(planId, null, false, null, null, null, null, null, null, null, "Uso adelantado"),
            superadminActor,
            "10.0.0.1"
        );

        assertThat(view.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(view.pendingBalance()).isEqualByComparingTo("75.00");

        ArgumentCaptor<SubscriptionCharge> chargeCaptor = ArgumentCaptor.forClass(SubscriptionCharge.class);
        verify(chargeRepository).save(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getChargeStatus()).isEqualTo(ChargeStatus.PENDING);
        assertThat(chargeCaptor.getValue().getPaidAmount()).isEqualByComparingTo("0.00");
        verify(entitlementService).evict(tenantId);
    }

    @Test
    @DisplayName("Conciliación FIFO de pagos amortiza cargos pendientes en orden cronológico y recalcula mora")
    void recordPaymentShouldReconcileOpenChargesFifo() {
        Instant t1 = Instant.now().minus(14, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(7, ChronoUnit.DAYS);
        SubscriptionCharge older = new SubscriptionCharge(
            tenantId, UUID.randomUUID(), "Semanal #1", t1, t1.plus(7, ChronoUnit.DAYS),
            new BigDecimal("75.00"), BigDecimal.ZERO, "BOB", null, null, adminId
        );
        ReflectionTestUtils.setField(older, "id", UUID.randomUUID());

        SubscriptionCharge newer = new SubscriptionCharge(
            tenantId, UUID.randomUUID(), "Semanal #2", t2, t2.plus(7, ChronoUnit.DAYS),
            new BigDecimal("100.00"), BigDecimal.ZERO, "BOB", null, null, adminId
        );
        ReflectionTestUtils.setField(newer, "id", UUID.randomUUID());

        when(chargeRepository.findByTenantIdAndChargeStatusInOrderByCreatedAtAsc(
            eq(tenantId),
            eq(List.of(ChargeStatus.PENDING, ChargeStatus.PARTIAL))
        )).thenReturn(List.of(older, newer));
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            SubscriptionPayment p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
            return p;
        });
        when(chargeRepository.sumPendingBalanceByTenantId(tenantId)).thenReturn(new BigDecimal("55.00"));

        PaymentRecordResultDto result = subscriptionService.recordPayment(
            tenantId,
            new RecordPaymentRequest(
                new BigDecimal("120.00"),
                "BOB",
                PlatformPaymentMethod.QR,
                "QR-991",
                null,
                null,
                "Abono parcial multi-ciclo",
                false
            ),
            superadminActor,
            "10.0.0.2"
        );

        assertThat(older.getChargeStatus()).isEqualTo(ChargeStatus.PAID);
        assertThat(older.getPaidAmount()).isEqualByComparingTo("75.00");
        assertThat(newer.getChargeStatus()).isEqualTo(ChargeStatus.PARTIAL);
        assertThat(newer.getPaidAmount()).isEqualByComparingTo("45.00");
        assertThat(result.allocatedChargeIds()).containsExactly(older.getId(), newer.getId());
        assertThat(result.remainingPendingBalance()).isEqualByComparingTo("55.00");
    }

    @Test
    @DisplayName("Suspensión TOTAL revoca sesiones activas del tenant y condonar cargo ya pagado falla")
    void suspendTotalRevokesSessionsAndWaivingPaidChargeFails() {
        TenantSubscription sub = new TenantSubscription(
            tenantId, UUID.randomUUID(), SubscriptionStatus.ACTIVE,
            Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(20, ChronoUnit.DAYS)
        );
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionPublicApi.revokeAllTenantSessions(tenantId)).thenReturn(3);
        when(entitlementService.getEntitlementView(tenantId)).thenReturn(
            new TenantEntitlementView(
                tenantId, true, SubscriptionStatus.SUSPENDED, SuspensionMode.TOTAL, "Mora crítica",
                "MONTHLY", "Mensual", Instant.now(), Instant.now().plus(20, ChronoUnit.DAYS),
                null, null, 0, BigDecimal.ZERO, "BOB", false, false, Map.of(), List.of()
            )
        );

        TenantEntitlementView view = subscriptionService.suspend(
            tenantId,
            new SuspendTenantRequest(SuspensionMode.TOTAL, "Mora crítica"),
            superadminActor,
            "10.0.0.3"
        );
        assertThat(view.loginAllowed()).isFalse();
        verify(sessionPublicApi).revokeAllTenantSessions(tenantId);

        // Cargo pagado no puede condonarse
        UUID paidChargeId = UUID.randomUUID();
        SubscriptionCharge paidCharge = new SubscriptionCharge(
            tenantId, UUID.randomUUID(), "Mensual", Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS),
            new BigDecimal("250.00"), new BigDecimal("250.00"), "BOB", null, null, adminId
        );
        ReflectionTestUtils.setField(paidCharge, "id", paidChargeId);
        when(chargeRepository.findById(paidChargeId)).thenReturn(Optional.of(paidCharge));

        assertThatThrownBy(() -> subscriptionService.waiveCharge(
            paidChargeId, new WaiveChargeRequest("Prueba"), superadminActor, "10.0.0.3"
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Reactivar período vencido sin gracia ni extendDays rechaza con regla de negocio")
    void reactivatingExpiredPeriodWithoutExtensionFails() {
        TenantSubscription expiredSub = new TenantSubscription(
            tenantId, UUID.randomUUID(), SubscriptionStatus.SUSPENDED,
            Instant.now().minus(35, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS)
        );
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(expiredSub));

        assertThatThrownBy(() -> subscriptionService.reactivate(
            tenantId, new ReactivateRequest("Intentar sin extender", null), superadminActor, "10.0.0.4"
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("ABM PlatformAdmin protege contra autodesactivación, último SUPERADMIN activo y duplicados")
    void adminManagementEnforcesSecurityGuards() {
        PlatformAdmin rootAdmin = new PlatformAdmin(
            "superadmin", "superadmin@platform.local", "Root", "hash", false, PlatformRole.SUPERADMIN
        );
        ReflectionTestUtils.setField(rootAdmin, "id", adminId);

        when(adminRepository.findById(adminId)).thenReturn(Optional.of(rootAdmin));

        // 1. Prohibir autodesactivación
        assertThatThrownBy(() -> adminManagementService.updateStatus(
            adminId, new UpdateAdminStatusRequest(false, null), superadminActor, "127.0.0.1"
        )).isInstanceOf(BusinessRuleException.class);

        // 2. Prohibir desactivar al último SUPERADMIN activo desde otro actor
        PlatformPrincipal otherActor = new PlatformPrincipal(
            UUID.randomUUID(), "other", "o@p.com", "Other", PlatformRole.SUPERADMIN, false
        );
        when(adminRepository.countByRoleAndIsActiveTrue(PlatformRole.SUPERADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> adminManagementService.updateStatus(
            adminId, new UpdateAdminStatusRequest(false, null), otherActor, "127.0.0.1"
        )).isInstanceOf(BusinessRuleException.class);

        // 3. Prohibir usuario duplicado
        when(adminRepository.findByUsernameNormalized("superadmin")).thenReturn(Optional.of(rootAdmin));
        assertThatThrownBy(() -> adminManagementService.create(
            new CreatePlatformAdminRequest("superadmin", "otro@p.com", "Dup", "ClaveSuperSegura123!", PlatformRole.PLATFORM_OPERATOR),
            superadminActor,
            "127.0.0.1"
        )).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("TenantSubscription inheritance usa override si existe o hereda del plan")
    void quotaOverridesTakePrecedenceOverPlanDefaults() {
        SubscriptionPlan plan = new SubscriptionPlan(
            "PLAN", "Plan", "Desc", BillingCycle.MONTHLY, 30,
            new BigDecimal("200.00"), "BOB", 3, 15, 100, 200, 5000
        );
        TenantSubscription sub = new TenantSubscription(
            tenantId, UUID.randomUUID(), SubscriptionStatus.ACTIVE, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS)
        );
        sub.updateQuotaOverrides(1, 2, null, null, 250, adminId);

        Map<QuotaResource, Integer> effective = sub.resolveEffectiveQuotas(plan);
        assertThat(effective.get(QuotaResource.BRANCHES)).isEqualTo(1);
        assertThat(effective.get(QuotaResource.USERS)).isEqualTo(2);
        assertThat(effective.get(QuotaResource.DISHES)).isEqualTo(100);
        assertThat(effective.get(QuotaResource.INVENTORY_ITEMS)).isEqualTo(200);
        assertThat(effective.get(QuotaResource.MONTHLY_ORDERS)).isEqualTo(250);
    }
}
