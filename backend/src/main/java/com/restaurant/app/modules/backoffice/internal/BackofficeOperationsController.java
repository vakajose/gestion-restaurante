package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.config.api.PlatformSettingDto;
import com.restaurant.app.core.storage.api.StorageConnectionTestResult;
import com.restaurant.app.modules.backoffice.internal.BackofficeObservabilityService.EnvironmentOverviewDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeObservabilityService.HealthOverviewDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeObservabilityService.LoggerEntryDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeObservabilityService.LoggersOverviewDto;
import com.restaurant.app.modules.backoffice.internal.BackofficeObservabilityService.MetricsOverviewDto;
import com.restaurant.app.modules.backoffice.internal.PlatformAuditService.AuditLogDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/backoffice")
class BackofficeOperationsController {

    private final BackofficeObservabilityService observabilityService;

    BackofficeOperationsController(BackofficeObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    record UpdateSettingRequest(String value) {
    }

    record ConfigureLoggerRequest(String configuredLevel) {
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<List<PlatformSettingDto>> listSettings() {
        return ResponseEntity.ok(observabilityService.listSettings());
    }

    @PutMapping("/settings/{settingKey}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<PlatformSettingDto> updateSetting(
        @PathVariable String settingKey,
        @RequestBody UpdateSettingRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        String value = request != null ? request.value() : null;
        return ResponseEntity.ok(observabilityService.updateSetting(
            settingKey,
            value,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PostMapping("/storage/test-connection")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<StorageConnectionTestResult> testActiveStorageConnection(
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(observabilityService.testActiveStorageConnection(
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @GetMapping("/observability/health")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<HealthOverviewDto> healthOverview() {
        return ResponseEntity.ok(observabilityService.getHealthOverview());
    }

    @GetMapping("/observability/metrics")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<MetricsOverviewDto> metricsOverview() {
        return ResponseEntity.ok(observabilityService.getMetricsOverview());
    }

    @GetMapping("/observability/loggers")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<LoggersOverviewDto> loggers(
        @RequestParam(required = false, defaultValue = "com.restaurant.app") String prefix
    ) {
        return ResponseEntity.ok(observabilityService.getLoggers(prefix));
    }

    @PutMapping("/observability/loggers/{loggerName}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<LoggerEntryDto> configureLogger(
        @PathVariable String loggerName,
        @RequestBody(required = false) ConfigureLoggerRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        String configuredLevel = request != null ? request.configuredLevel() : null;
        return ResponseEntity.ok(observabilityService.configureLoggerLevel(
            loggerName,
            configuredLevel,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @GetMapping("/observability/environment")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<EnvironmentOverviewDto> environmentOverview() {
        return ResponseEntity.ok(observabilityService.getEnvironmentOverview());
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<Page<AuditLogDto>> searchAuditLogs(
        @RequestParam(required = false) String actor,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(observabilityService.searchAuditLogs(actor, action, from, to, page, size));
    }
}
