package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView.ResourceQuotaUsage;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackofficeTenantFirstLoginTest {

    @Mock
    private CompanyPublicApi companyPublicApi;
    @Mock
    private TenantSubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private SubscriptionPaymentRepository paymentRepository;
    @Mock
    private TenantDailyTelemetryRepository telemetryRepository;
    @Mock
    private TenantUsageSnapshotReader usageReader;
    @Mock
    private TenantEntitlementService entitlementService;
    @Mock
    private UserPublicApi userPublicApi;
    @Mock
    private PlatformAuditService auditService;

    private BackofficeTenantService service;

    @BeforeEach
    void setUp() {
        service = new BackofficeTenantService(
            companyPublicApi,
            subscriptionRepository,
            chargeRepository,
            paymentRepository,
            telemetryRepository,
            usageReader,
            entitlementService,
            userPublicApi,
            auditService
        );
    }

    @Test
    @DisplayName("listTenants marca hasLoggedIn=false cuando ningún usuario del tenant ha iniciado sesión")
    void shouldExposePendingFirstLoginInList() {
        UUID tPending = UUID.randomUUID();
        UUID tActive = UUID.randomUUID();
        Instant firstLogin = Instant.parse("2026-11-10T14:30:00Z");

        when(companyPublicApi.listAllCompanies()).thenReturn(List.of(
            new TenantDto(tPending, "Sin Ingreso S.R.L.", "111", "sin-ingreso", true, Instant.now(), Instant.now()),
            new TenantDto(tActive, "Con Ingreso S.R.L.", "222", "con-ingreso", true, Instant.now(), Instant.now())
        ));
        when(chargeRepository.sumPendingBalanceGroupedByTenant()).thenReturn(List.of());
        when(telemetryRepository.aggregateByTenantInRange(any(), any())).thenReturn(List.of());
        when(userPublicApi.findFirstTenantLoginAtMap()).thenReturn(Map.of(tActive, firstLogin));
        when(entitlementService.getEntitlementView(tPending)).thenReturn(entitlementView(tPending));
        when(entitlementService.getEntitlementView(tActive)).thenReturn(entitlementView(tActive));

        var items = service.listTenants(null, null, null, null);

        assertThat(items).hasSize(2);
        var pendingItem = items.stream().filter(i -> i.tenantId().equals(tPending)).findFirst().orElseThrow();
        var activeItem = items.stream().filter(i -> i.tenantId().equals(tActive)).findFirst().orElseThrow();

        assertThat(pendingItem.hasLoggedIn()).isFalse();
        assertThat(pendingItem.firstLoginAt()).isNull();
        assertThat(activeItem.hasLoggedIn()).isTrue();
        assertThat(activeItem.firstLoginAt()).isEqualTo(firstLogin);
    }

    @Test
    @DisplayName("getTenantDetail expone firstLoginAt null y hasLoggedIn=false antes del primer ingreso")
    void shouldExposeFirstLoginInDetail() {
        UUID tenantId = UUID.randomUUID();
        when(companyPublicApi.getCompany(tenantId)).thenReturn(
            new TenantDto(tenantId, "Nuevo S.R.L.", "333", "nuevo", true, Instant.now(), Instant.now())
        );
        when(entitlementService.getEntitlementView(tenantId)).thenReturn(entitlementView(tenantId));
        when(usageReader.readFullUsage(tenantId)).thenReturn(new TenantUsageSnapshotReader.TenantStructuralUsage(
            1, 1, 1, 1, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), ZoneId.of("America/La_Paz")
        ));
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(telemetryRepository.findByTenantIdAndStatDateBetweenOrderByStatDateAsc(eq(tenantId), any(), any()))
            .thenReturn(List.of());
        when(userPublicApi.findFirstTenantLoginAt(tenantId)).thenReturn(Optional.empty());

        var detail = service.getTenantDetail(tenantId);

        assertThat(detail.hasLoggedIn()).isFalse();
        assertThat(detail.firstLoginAt()).isNull();
    }

    private TenantEntitlementView entitlementView(UUID tenantId) {
        return new TenantEntitlementView(
            tenantId,
            true,
            SubscriptionStatus.ACTIVE,
            SuspensionMode.NONE,
            null,
            "BASIC",
            "Básico",
            Instant.now().minusSeconds(86400),
            Instant.now().plusSeconds(86400),
            null,
            null,
            0,
            BigDecimal.ZERO,
            "BOB",
            true,
            true,
            Map.of(
                QuotaResource.BRANCHES, new ResourceQuotaUsage(1, 2, false),
                QuotaResource.USERS, new ResourceQuotaUsage(1, 5, false),
                QuotaResource.MONTHLY_ORDERS, new ResourceQuotaUsage(0, 100, false)
            ),
            List.of()
        );
    }
}
