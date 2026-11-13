package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.config.api.PlatformSettingDto;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import com.restaurant.app.core.storage.api.MediaStorageService;
import com.restaurant.app.core.storage.api.StorageConnectionTestResult;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevelsDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggersDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint.SingleLoggerLevelsDescriptor;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint.MetricDescriptor;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
class BackofficeObservabilityService {

    private static final List<String> CURATED_METRICS = List.of(
        "jvm.memory.used",
        "jvm.memory.max",
        "jvm.threads.live",
        "jvm.threads.daemon",
        "process.uptime",
        "system.cpu.usage",
        "hikaricp.connections.active",
        "hikaricp.connections.idle",
        "hikaricp.connections.max",
        "http.server.requests"
    );

    private static final List<String> CURATED_ENV_KEYS = List.of(
        "spring.application.name",
        "server.port",
        "spring.threads.virtual.enabled",
        "spring.datasource.url",
        "spring.jpa.hibernate.ddl-auto",
        "spring.flyway.enabled",
        "spring.servlet.multipart.max-file-size",
        "restaurant.storage.provider",
        "restaurant.storage.local.base-dir",
        "restaurant.storage.s3.endpoint",
        "restaurant.storage.s3.region",
        "restaurant.storage.s3.bucket",
        "restaurant.security.platform-jwt.expiration-seconds",
        "management.endpoints.web.exposure.include"
    );

    private final PlatformSettingsService platformSettingsService;
    private final MediaStorageService mediaStorageService;
    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;
    private final ObjectProvider<MetricsEndpoint> metricsEndpointProvider;
    private final ObjectProvider<LoggersEndpoint> loggersEndpointProvider;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ObjectProvider<LoggingSystem> loggingSystemProvider;
    private final Environment environment;
    private final PlatformAuditService auditService;

    BackofficeObservabilityService(
        PlatformSettingsService platformSettingsService,
        MediaStorageService mediaStorageService,
        ObjectProvider<HealthEndpoint> healthEndpointProvider,
        ObjectProvider<MetricsEndpoint> metricsEndpointProvider,
        ObjectProvider<LoggersEndpoint> loggersEndpointProvider,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<LoggingSystem> loggingSystemProvider,
        Environment environment,
        PlatformAuditService auditService
    ) {
        this.platformSettingsService = platformSettingsService;
        this.mediaStorageService = mediaStorageService;
        this.healthEndpointProvider = healthEndpointProvider;
        this.metricsEndpointProvider = metricsEndpointProvider;
        this.loggersEndpointProvider = loggersEndpointProvider;
        this.meterRegistryProvider = meterRegistryProvider;
        this.loggingSystemProvider = loggingSystemProvider;
        this.environment = environment;
        this.auditService = auditService;
    }

    record HealthNodeDto(
        String status,
        Map<String, Object> details,
        Map<String, HealthNodeDto> components
    ) {
    }

    record HealthOverviewDto(
        String status,
        Instant checkedAt,
        Map<String, HealthNodeDto> components
    ) {
    }

    record MetricMeasurementDto(String statistic, Double value) {
    }

    record MetricEntryDto(
        String name,
        String description,
        String baseUnit,
        List<MetricMeasurementDto> measurements
    ) {
    }

    record MetricsOverviewDto(
        Instant collectedAt,
        List<MetricEntryDto> metrics,
        int totalAvailableMeters
    ) {
    }

    record LoggerEntryDto(
        String name,
        String configuredLevel,
        String effectiveLevel
    ) {
    }

    record LoggersOverviewDto(
        List<String> supportedLevels,
        List<LoggerEntryDto> loggers
    ) {
    }

    record EnvironmentOverviewDto(
        List<String> activeProfiles,
        List<String> defaultProfiles,
        Map<String, String> properties
    ) {
    }

    public List<PlatformSettingDto> listSettings() {
        return platformSettingsService.findAll();
    }

    public PlatformSettingDto updateSetting(String key, String value, PlatformPrincipal actor, String ipAddress) {
        PlatformSettingDto updated = platformSettingsService.updateSetting(
            key,
            value,
            actor != null ? actor.adminId() : null
        );
        auditService.record(
            actor,
            "PLATFORM_SETTING_UPDATED",
            "PLATFORM_SETTING",
            updated.key(),
            Map.of("key", updated.key(), "value", updated.value()),
            ipAddress
        );
        return updated;
    }

    public StorageConnectionTestResult testActiveStorageConnection(PlatformPrincipal actor, String ipAddress) {
        StorageConnectionTestResult result = mediaStorageService.testActiveConnection();
        auditService.record(
            actor,
            "STORAGE_CONNECTION_TESTED",
            "STORAGE",
            result.provider().name(),
            Map.of(
                "provider", result.provider().name(),
                "reachable", result.reachable(),
                "targetDescription", result.targetDescription(),
                "message", result.message()
            ),
            ipAddress
        );
        return result;
    }

    public HealthOverviewDto getHealthOverview() {
        HealthEndpoint healthEndpoint = healthEndpointProvider.getIfAvailable();
        if (healthEndpoint == null) {
            return new HealthOverviewDto("UNKNOWN", Instant.now(), Map.of());
        }
        HealthDescriptor root = healthEndpoint.health();
        HealthNodeDto node = toHealthNode(root);
        return new HealthOverviewDto(node.status(), Instant.now(), node.components());
    }

    public MetricsOverviewDto getMetricsOverview() {
        MetricsEndpoint metricsEndpoint = metricsEndpointProvider.getIfAvailable();
        if (metricsEndpoint != null) {
            List<MetricEntryDto> entries = new ArrayList<>();
            for (String name : CURATED_METRICS) {
                MetricDescriptor descriptor = metricsEndpoint.metric(name, null);
                if (descriptor != null) {
                    List<MetricMeasurementDto> samples = descriptor.getMeasurements().stream()
                        .map(s -> new MetricMeasurementDto(s.getStatistic().name(), s.getValue()))
                        .toList();
                    entries.add(new MetricEntryDto(
                        descriptor.getName(),
                        descriptor.getDescription(),
                        descriptor.getBaseUnit(),
                        samples
                    ));
                }
            }
            int totalNames = metricsEndpoint.listNames().getNames().size();
            return new MetricsOverviewDto(Instant.now(), entries, totalNames);
        }

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return new MetricsOverviewDto(Instant.now(), List.of(), 0);
        }
        List<MetricEntryDto> entries = new ArrayList<>();
        for (String name : CURATED_METRICS) {
            Collection<Meter> meters = meterRegistry.find(name).meters();
            if (!meters.isEmpty()) {
                Meter first = meters.iterator().next();
                Map<Statistic, Double> aggregated = new LinkedHashMap<>();
                for (Meter meter : meters) {
                    for (Measurement measurement : meter.measure()) {
                        Statistic stat = measurement.getStatistic();
                        aggregated.merge(stat, measurement.getValue(), (a, b) ->
                            stat == Statistic.MAX ? Math.max(a, b) : a + b);
                    }
                }
                List<MetricMeasurementDto> samples = aggregated.entrySet().stream()
                    .map(e -> new MetricMeasurementDto(e.getKey().name(), e.getValue()))
                    .toList();
                entries.add(new MetricEntryDto(
                    first.getId().getName(),
                    first.getId().getDescription(),
                    first.getId().getBaseUnit(),
                    samples
                ));
            }
        }
        int totalNames = (int) meterRegistry.getMeters().stream()
            .map(m -> m.getId().getName())
            .distinct()
            .count();
        return new MetricsOverviewDto(Instant.now(), entries, totalNames);
    }

    public LoggersOverviewDto getLoggers(String prefix) {
        String normalizedPrefix = (prefix != null && !prefix.isBlank()) ? prefix.trim() : "";
        LoggersEndpoint loggersEndpoint = loggersEndpointProvider.getIfAvailable();
        if (loggersEndpoint != null) {
            LoggersDescriptor descriptor = loggersEndpoint.loggers();
            List<String> levels = descriptor.getLevels().stream().map(Enum::name).toList();

            Map<String, LoggerLevelsDescriptor> sorted = new TreeMap<>(descriptor.getLoggers());
            List<LoggerEntryDto> entries = new ArrayList<>();
            for (Map.Entry<String, LoggerLevelsDescriptor> entry : sorted.entrySet()) {
                String name = entry.getKey();
                if (!normalizedPrefix.isEmpty() && !name.startsWith(normalizedPrefix)) {
                    continue;
                }
                LoggerLevelsDescriptor levelDesc = entry.getValue();
                String configured = levelDesc.getConfiguredLevel();
                String effective = levelDesc instanceof SingleLoggerLevelsDescriptor single
                    ? single.getEffectiveLevel()
                    : configured;
                entries.add(new LoggerEntryDto(name, configured, effective));
            }
            return new LoggersOverviewDto(levels, entries);
        }

        LoggingSystem loggingSystem = resolveLoggingSystem();
        Set<LogLevel> supportedLevels = loggingSystem.getSupportedLogLevels();
        List<String> levels = Arrays.stream(LogLevel.values())
            .filter(supportedLevels::contains)
            .map(Enum::name)
            .toList();
        List<LoggerEntryDto> entries = loggingSystem.getLoggerConfigurations().stream()
            .filter(cfg -> normalizedPrefix.isEmpty() || cfg.getName().startsWith(normalizedPrefix))
            .sorted(Comparator.comparing(LoggerConfiguration::getName))
            .map(cfg -> new LoggerEntryDto(
                cfg.getName(),
                cfg.getConfiguredLevel() != null ? cfg.getConfiguredLevel().name() : null,
                cfg.getEffectiveLevel() != null ? cfg.getEffectiveLevel().name() : null
            ))
            .toList();
        return new LoggersOverviewDto(levels, entries);
    }

    public LoggerEntryDto configureLoggerLevel(
        String loggerName,
        String configuredLevel,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (loggerName == null || loggerName.isBlank()) {
            throw new ValidationException("El nombre del logger es obligatorio");
        }
        LogLevel targetLevel = parseLogLevel(configuredLevel);
        String trimmedName = loggerName.trim();

        String configured;
        String effective;
        LoggersEndpoint loggersEndpoint = loggersEndpointProvider.getIfAvailable();
        if (loggersEndpoint != null) {
            loggersEndpoint.configureLogLevel(trimmedName, targetLevel);
            LoggerLevelsDescriptor updated = loggersEndpoint.loggerLevels(trimmedName);
            configured = updated != null ? updated.getConfiguredLevel() : (targetLevel != null ? targetLevel.name() : null);
            effective = updated instanceof SingleLoggerLevelsDescriptor single
                ? single.getEffectiveLevel()
                : configured;
        } else {
            LoggingSystem loggingSystem = resolveLoggingSystem();
            loggingSystem.setLogLevel(trimmedName, targetLevel);
            LoggerConfiguration updated = loggingSystem.getLoggerConfiguration(trimmedName);
            configured = updated != null && updated.getConfiguredLevel() != null
                ? updated.getConfiguredLevel().name()
                : (targetLevel != null ? targetLevel.name() : null);
            effective = updated != null && updated.getEffectiveLevel() != null
                ? updated.getEffectiveLevel().name()
                : configured;
        }

        auditService.record(
            actor,
            "LOGGER_LEVEL_CHANGED",
            "LOGGER",
            trimmedName,
            Map.of(
                "loggerName", trimmedName,
                "configuredLevel", configured != null ? configured : "INHERITED",
                "effectiveLevel", effective != null ? effective : "UNKNOWN"
            ),
            ipAddress
        );
        return new LoggerEntryDto(trimmedName, configured, effective);
    }

    private LoggingSystem resolveLoggingSystem() {
        LoggingSystem bean = loggingSystemProvider.getIfAvailable();
        if (bean != null) {
            return bean;
        }
        return LoggingSystem.get(BackofficeObservabilityService.class.getClassLoader());
    }

    public EnvironmentOverviewDto getEnvironmentOverview() {
        Map<String, String> safeProps = new LinkedHashMap<>();
        for (String key : CURATED_ENV_KEYS) {
            String value = environment.getProperty(key);
            if (value != null) {
                safeProps.put(key, maskIfSensitive(key, value));
            }
        }
        return new EnvironmentOverviewDto(
            Arrays.asList(environment.getActiveProfiles()),
            Arrays.asList(environment.getDefaultProfiles()),
            safeProps
        );
    }

    public Page<PlatformAuditService.AuditLogDto> searchAuditLogs(
        String actor,
        String action,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        return auditService.search(actor, action, from, to, page, size);
    }

    private LogLevel parseLogLevel(String raw) {
        if (raw == null || raw.isBlank() || "RESET".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        try {
            return LogLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                "Nivel de log inválido '" + raw + "'. Valores permitidos: TRACE, DEBUG, INFO, WARN, ERROR, OFF, RESET"
            );
        }
    }

    private HealthNodeDto toHealthNode(HealthDescriptor descriptor) {
        if (descriptor == null) {
            return new HealthNodeDto("UNKNOWN", Map.of(), Map.of());
        }
        String status = descriptor.getStatus() != null ? descriptor.getStatus().getCode() : "UNKNOWN";
        if (descriptor instanceof CompositeHealthDescriptor composite) {
            Map<String, HealthNodeDto> children = new LinkedHashMap<>();
            composite.getComponents().forEach((name, child) -> children.put(name, toHealthNode(child)));
            return new HealthNodeDto(status, Map.of(), children);
        }
        if (descriptor instanceof IndicatedHealthDescriptor indicated) {
            return new HealthNodeDto(status, indicated.getDetails(), Map.of());
        }
        return new HealthNodeDto(status, Map.of(), Map.of());
    }

    private String maskIfSensitive(String key, String value) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("secret") || lower.contains("password") || lower.contains("token")
            || lower.contains("credential") || lower.contains("private") || lower.endsWith("-key")) {
            return "***";
        }
        if ("spring.datasource.url".equals(key)) {
            int q = value.indexOf('?');
            if (q > 0 && value.toLowerCase(Locale.ROOT).contains("password")) {
                return value.substring(0, q) + "?***";
            }
        }
        return value;
    }
}
