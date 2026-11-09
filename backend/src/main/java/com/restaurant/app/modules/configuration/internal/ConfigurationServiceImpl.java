package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.AuthService;
import com.restaurant.app.core.security.LoginRequest;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterCriticality;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.ParameterUpdatedEvent;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.configuration.api.SystemParameterDto;
import com.restaurant.app.modules.configuration.api.UpdateParameterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ConfigurationServiceImpl implements ConfigurationPublicApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final SystemParameterValueRepository repository;
    private final CascadingParameterResolver resolver;
    private final ParameterCacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthService authService;

    ConfigurationServiceImpl(
        SystemParameterValueRepository repository,
        CascadingParameterResolver resolver,
        ParameterCacheService cacheService,
        ApplicationEventPublisher eventPublisher,
        AuthService authService
    ) {
        this.repository = repository;
        this.resolver = resolver;
        this.cacheService = cacheService;
        this.eventPublisher = eventPublisher;
        this.authService = authService;
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(SystemParameter param, UUID branchId, UUID tenantId) {
        if (param == null) {
            throw new IllegalArgumentException("El parámetro no puede ser nulo");
        }
        return getString(param.getKey(), branchId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(String key, UUID branchId, UUID tenantId) {
        return resolver.resolveEffectiveValue(key, branchId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(SystemParameter param, UUID branchId, UUID tenantId) {
        String val = getString(param, branchId, tenantId);
        return Boolean.parseBoolean(val);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, UUID branchId, UUID tenantId) {
        String val = getString(key, branchId, tenantId);
        return Boolean.parseBoolean(val);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(SystemParameter param, UUID branchId, UUID tenantId) {
        String val = getString(param, branchId, tenantId);
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parsing parameter '{}' as integer with value '{}'. Falling back to 0", param.getKey(), val);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(String key, UUID branchId, UUID tenantId) {
        String val = getString(key, branchId, tenantId);
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parsing parameter '{}' as integer with value '{}'. Falling back to 0", key, val);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getDouble(SystemParameter param, UUID branchId, UUID tenantId) {
        String val = getString(param, branchId, tenantId);
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parsing parameter '{}' as double with value '{}'. Falling back to 0.0", param.getKey(), val);
            return 0.0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getDouble(String key, UUID branchId, UUID tenantId) {
        String val = getString(key, branchId, tenantId);
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Error parsing parameter '{}' as double with value '{}'. Falling back to 0.0", key, val);
            return 0.0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <E extends Enum<E>> E getEnum(SystemParameter param, UUID branchId, UUID tenantId, Class<E> enumClass) {
        String val = getString(param, branchId, tenantId);
        try {
            return Enum.valueOf(enumClass, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Value '{}' not found in enum '{}' for parameter '{}'", val, enumClass.getSimpleName(), param.getKey());
            throw new ValidationException("Valor inválido '" + val + "' para el enum " + enumClass.getSimpleName());
        }
    }

    @Override
    @Transactional
    public void setOverride(SystemParameter param, ParameterScope scope, UUID tenantId, UUID branchId, String value, UUID userId, String reason) {
        if (param == null) {
            throw new IllegalArgumentException("El parámetro no puede ser nulo");
        }
        setOverride(param.getKey(), scope, tenantId, branchId, value, userId, reason);
    }

    @Override
    @Transactional
    public void setOverride(String key, ParameterScope scope, UUID tenantId, UUID branchId, String value, UUID userId, String reason) {
        SystemParameterCatalog.ParameterDefinition def = SystemParameterCatalog.getDefinition(key)
            .orElseThrow(() -> new EntityNotFoundException("Parámetro no registrado", key));

        SystemParameterCatalog.validate(def, scope, value);

        String oldValue = resolver.resolveEffectiveValue(key, branchId, tenantId);

        // Guardado disperso (Sparse Storage Upsert)
        Optional<SystemParameterValue> existingOpt;
        if (scope == ParameterScope.BRANCH) {
            if (branchId == null) {
                throw new ValidationException("branchId es obligatorio para scope BRANCH");
            }
            existingOpt = repository.findByParameterKeyAndScopeAndBranchId(def.key(), scope, branchId);
        } else if (scope == ParameterScope.TENANT) {
            if (tenantId == null) {
                throw new ValidationException("tenantId es obligatorio para scope TENANT");
            }
            existingOpt = repository.findByParameterKeyAndScopeAndTenantId(def.key(), scope, tenantId);
        } else {
            existingOpt = repository.findByParameterKeyAndScope(def.key(), scope);
        }

        SystemParameterValue entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setParameterValue(value.trim());
            entity.setUpdatedBy(userId);
            entity.setAuditReason(reason);
        } else {
            entity = new SystemParameterValue(
                def.key(),
                scope,
                tenantId,
                branchId,
                value.trim(),
                userId,
                reason
            );
        }
        repository.save(entity);

        // Desalojo concurrente de caché (Hot-Reload)
        cacheService.evictForParameter(def.key(), scope, tenantId, branchId);

        // Publicación desacoplada del evento
        eventPublisher.publishEvent(new ParameterUpdatedEvent(
            def.key(),
            scope,
            tenantId,
            branchId,
            oldValue,
            value.trim()
        ));

        log.info("Parámetro '{}' actualizado en scope {} para tenant {} sucursal {} por usuario {}: '{}' -> '{}'",
            def.key(), scope, tenantId, branchId, userId, oldValue, value.trim());
    }

    @Override
    @Transactional
    public void resetOverride(SystemParameter param, ParameterScope scope, UUID tenantId, UUID branchId) {
        if (param == null) {
            throw new IllegalArgumentException("El parámetro no puede ser nulo");
        }
        resetOverride(param.getKey(), scope, tenantId, branchId);
    }

    @Override
    @Transactional
    public void resetOverride(String key, ParameterScope scope, UUID tenantId, UUID branchId) {
        SystemParameterCatalog.ParameterDefinition def = SystemParameterCatalog.getDefinition(key)
            .orElseThrow(() -> new EntityNotFoundException("Parámetro no registrado", key));

        String oldValue = resolver.resolveEffectiveValue(key, branchId, tenantId);

        if (scope == ParameterScope.BRANCH) {
            if (branchId == null) {
                throw new ValidationException("branchId es obligatorio para resetear scope BRANCH");
            }
            repository.deleteByBranchScope(def.key(), scope, branchId);
        } else if (scope == ParameterScope.TENANT) {
            if (tenantId == null) {
                throw new ValidationException("tenantId es obligatorio para resetear scope TENANT");
            }
            repository.deleteByTenantScope(def.key(), scope, tenantId);
        } else {
            repository.deleteByGlobalScope(def.key(), scope);
        }

        cacheService.evictForParameter(def.key(), scope, tenantId, branchId);

        String newValue = resolver.resolveEffectiveValue(key, branchId, tenantId);

        eventPublisher.publishEvent(new ParameterUpdatedEvent(
            def.key(),
            scope,
            tenantId,
            branchId,
            oldValue,
            newValue
        ));

        log.info("Parámetro '{}' restablecido en scope {} para tenant {} sucursal {}. Nuevo valor heredado: '{}'",
            def.key(), scope, tenantId, branchId, newValue);
    }

    @Transactional(readOnly = true)
    public List<SystemParameterDto> getParameters(
        ParameterScope scope,
        ParameterCategory category,
        UUID branchId,
        UUID tenantId
    ) {
        List<SystemParameterCatalog.ParameterDefinition> definitions = category != null
            ? SystemParameterCatalog.getDefinitionsByCategory(category)
            : SystemParameterCatalog.getAllDefinitions();

        List<SystemParameterDto> result = new ArrayList<>(definitions.size());

        for (SystemParameterCatalog.ParameterDefinition def : definitions) {
            CascadingParameterResolver.ResolvedParameter details = resolver.resolveDetails(
                def.key(),
                scope,
                branchId,
                tenantId
            );

            Instant updatedAt = details.overrideEntity() != null ? details.overrideEntity().getUpdatedAt() : null;
            UUID updatedBy = details.overrideEntity() != null ? details.overrideEntity().getUpdatedBy() : null;
            String auditReason = details.overrideEntity() != null ? details.overrideEntity().getAuditReason() : null;

            result.add(new SystemParameterDto(
                def.key(),
                def.name(),
                def.description(),
                def.category(),
                def.type(),
                details.effectiveValue(),
                def.defaultValue(),
                scope,
                details.inheritedFrom(),
                details.isOverridden(),
                def.criticality(),
                def.requiresRestart(),
                def.options(),
                def.help(),
                updatedAt,
                updatedBy,
                auditReason
            ));
        }

        return result;
    }

    @Transactional
    public SystemParameterDto updateParameterFromRequest(
        String key,
        UpdateParameterRequest request,
        UUID tenantId,
        UUID currentUserId,
        String currentUsername,
        String currentTenantSlug
    ) {
        SystemParameterCatalog.ParameterDefinition def = SystemParameterCatalog.getDefinition(key)
            .orElseThrow(() -> new EntityNotFoundException("Parámetro no registrado", key));

        ParameterScope targetScope = request.scope() != null ? request.scope() : ParameterScope.BRANCH;
        UUID targetBranchId = request.branchId();

        // Control de Criticidad y Reautenticación Forense (ADR-0008 / UC-311.3)
        if (def.criticality() == ParameterCriticality.HIGH) {
            if (request.passwordConfirmation() == null || request.passwordConfirmation().isBlank()) {
                throw new ValidationException("La reautenticación con contraseña es obligatoria para modificar parámetros de alta criticidad");
            }
            if (request.auditReason() == null || request.auditReason().trim().length() < 10) {
                throw new ValidationException("Debe ingresar un motivo de justificación de al menos 10 caracteres para parámetros críticos");
            }

            // Validar contraseña del usuario administrador
            try {
                authService.login(new LoginRequest(
                    currentTenantSlug != null ? currentTenantSlug : "",
                    currentUsername != null ? currentUsername : "",
                    request.passwordConfirmation()
                ));
            } catch (Exception e) {
                throw new BusinessRuleException("Contraseña de confirmación incorrecta: no se pudo autorizar la modificación crítica");
            }
        }

        setOverride(
            def.key(),
            targetScope,
            tenantId,
            targetBranchId,
            request.value(),
            currentUserId,
            request.auditReason()
        );

        CascadingParameterResolver.ResolvedParameter details = resolver.resolveDetails(
            def.key(),
            targetScope,
            targetBranchId,
            tenantId
        );

        Instant updatedAt = details.overrideEntity() != null ? details.overrideEntity().getUpdatedAt() : Instant.now();
        UUID updatedBy = details.overrideEntity() != null ? details.overrideEntity().getUpdatedBy() : currentUserId;
        String auditReason = details.overrideEntity() != null ? details.overrideEntity().getAuditReason() : request.auditReason();

        return new SystemParameterDto(
            def.key(),
            def.name(),
            def.description(),
            def.category(),
            def.type(),
            details.effectiveValue(),
            def.defaultValue(),
            targetScope,
            details.inheritedFrom(),
            details.isOverridden(),
            def.criticality(),
            def.requiresRestart(),
            def.options(),
            def.help(),
            updatedAt,
            updatedBy,
            auditReason
        );
    }
}
