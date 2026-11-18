package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackofficeTenantUsersTest {

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
    @DisplayName("listTenantUsers verifica existencia del tenant y retorna sus usuarios")
    void shouldListTenantUsers() {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenant = new TenantDto(tenantId, "Restaurante Test", "123", "test", true, Instant.now(), Instant.now());
        UserSummaryDto user = new UserSummaryDto(UUID.randomUUID(), tenantId, null, "admin", "admin@test.com", "ADMIN_TENANT", true, Instant.now());

        when(companyPublicApi.getCompany(tenantId)).thenReturn(tenant);
        when(userPublicApi.getUsersByTenant(tenantId)).thenReturn(List.of(user));

        List<UserSummaryDto> result = service.listTenantUsers(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("resetTenantUserPassword delega reset y registra auditoría inmutable")
    void shouldResetTenantUserPasswordAndAudit() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PlatformPrincipal actor = new PlatformPrincipal(adminId, "superadmin", "sa@platform.local", "SA", PlatformRole.SUPERADMIN, false);
        TenantDto tenant = new TenantDto(tenantId, "Restaurante Test", "123", "test", true, Instant.now(), Instant.now());
        UserSummaryDto updated = new UserSummaryDto(userId, tenantId, null, "admin", "admin@test.com", "ADMIN_TENANT", true, Instant.now());

        when(companyPublicApi.getCompany(tenantId)).thenReturn(tenant);
        when(userPublicApi.adminResetPassword(tenantId, userId, "TempPass123!")).thenReturn(updated);

        UserSummaryDto result = service.resetTenantUserPassword(tenantId, userId, "TempPass123!", actor, "10.0.0.5");

        assertThat(result.id()).isEqualTo(userId);
        verify(auditService).record(
            eq(actor),
            eq("TENANT_USER_PASSWORD_RESET"),
            eq("APP_USER"),
            eq(userId.toString()),
            eq(Map.of(
                "tenantId", tenantId.toString(),
                "username", "admin",
                "mustChangePassword", true
            )),
            eq("10.0.0.5")
        );
    }

    @Test
    @DisplayName("toggleTenantUserStatus alterna estado del usuario y registra auditoría")
    void shouldToggleTenantUserStatusAndAudit() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PlatformPrincipal actor = new PlatformPrincipal(adminId, "superadmin", "sa@platform.local", "SA", PlatformRole.SUPERADMIN, false);
        TenantDto tenant = new TenantDto(tenantId, "Restaurante Test", "123", "test", true, Instant.now(), Instant.now());
        UserSummaryDto updated = new UserSummaryDto(userId, tenantId, null, "cajero1", null, "CASHIER", false, Instant.now());

        when(companyPublicApi.getCompany(tenantId)).thenReturn(tenant);
        when(userPublicApi.toggleUserStatus(tenantId, userId, false)).thenReturn(updated);

        UserSummaryDto result = service.toggleTenantUserStatus(tenantId, userId, false, actor, "10.0.0.5");

        assertThat(result.isActive()).isFalse();
        verify(auditService).record(
            eq(actor),
            eq("TENANT_USER_STATUS_CHANGED"),
            eq("APP_USER"),
            eq(userId.toString()),
            eq(Map.of(
                "tenantId", tenantId.toString(),
                "username", "cajero1",
                "isActive", false
            )),
            eq("10.0.0.5")
        );
    }
}
