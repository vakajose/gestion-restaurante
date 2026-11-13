package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.modules.backoffice.internal.BackofficeTenantService.BackofficeTenantDetailDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeTenantService.BackofficeTenantListItemDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeTenantService.DashboardOverviewDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeTenantService.TenantTelemetryReportDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice")
@PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
class BackofficeTenantController {

    private final BackofficeTenantService tenantService;

    BackofficeTenantController(BackofficeTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/dashboard/overview")
    ResponseEntity<DashboardOverviewDto> dashboardOverview() {
        return ResponseEntity.ok(tenantService.getDashboardOverview());
    }

    @GetMapping("/tenants")
    ResponseEntity<List<BackofficeTenantListItemDto>> listTenants(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) SubscriptionStatus status,
        @RequestParam(required = false) Boolean hasDebt,
        @RequestParam(required = false) Boolean overQuota
    ) {
        return ResponseEntity.ok(tenantService.listTenants(search, status, hasDebt, overQuota));
    }

    @GetMapping("/tenants/{tenantId}")
    ResponseEntity<BackofficeTenantDetailDto> getTenantDetail(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantService.getTenantDetail(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/telemetry")
    ResponseEntity<TenantTelemetryReportDto> getTenantTelemetry(
        @PathVariable UUID tenantId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(tenantService.getTenantTelemetry(tenantId, from, to));
    }
}
