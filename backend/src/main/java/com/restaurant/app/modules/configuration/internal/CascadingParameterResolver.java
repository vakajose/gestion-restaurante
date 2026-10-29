package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class CascadingParameterResolver {

    record ResolvedParameter(
        String effectiveValue,
        ParameterScope inheritedFrom,
        boolean isOverridden,
        SystemParameterValue overrideEntity
    ) {}

    private final SystemParameterValueRepository repository;
    private final ParameterCacheService cacheService;

    CascadingParameterResolver(
        SystemParameterValueRepository repository,
        ParameterCacheService cacheService
    ) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    public String resolveEffectiveValue(String key, UUID branchId, UUID tenantId) {
        // 1. Verificar caché en memoria ($O(1)$)
        Optional<String> cached = cacheService.get(tenantId, branchId, key);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Fallback jerárquico a base de datos
        ResolvedParameter details = resolveDetails(key, branchId != null ? ParameterScope.BRANCH : ParameterScope.TENANT, branchId, tenantId);
        String effectiveValue = details.effectiveValue();

        // 3. Poblar caché concurrente
        cacheService.put(tenantId, branchId, key, effectiveValue);
        return effectiveValue;
    }

    public ResolvedParameter resolveDetails(String key, ParameterScope requestedScope, UUID branchId, UUID tenantId) {
        SystemParameterCatalog.ParameterDefinition definition = SystemParameterCatalog.getDefinition(key)
            .orElseThrow(() -> new EntityNotFoundException("Parámetro no encontrado en catálogo", key));

        List<SystemParameterValue> overrides = repository.findOverridesHierarchical(definition.key(), tenantId, branchId);

        SystemParameterValue branchOverride = overrides.stream()
            .filter(o -> o.getScope() == ParameterScope.BRANCH && branchId != null && branchId.equals(o.getBranchId()))
            .findFirst()
            .orElse(null);

        SystemParameterValue tenantOverride = overrides.stream()
            .filter(o -> o.getScope() == ParameterScope.TENANT && tenantId != null && tenantId.equals(o.getTenantId()))
            .findFirst()
            .orElse(null);

        SystemParameterValue globalOverride = overrides.stream()
            .filter(o -> o.getScope() == ParameterScope.GLOBAL)
            .findFirst()
            .orElse(null);

        if (requestedScope == ParameterScope.BRANCH) {
            if (branchOverride != null) {
                return new ResolvedParameter(branchOverride.getParameterValue(), ParameterScope.BRANCH, true, branchOverride);
            }
            if (tenantOverride != null) {
                return new ResolvedParameter(tenantOverride.getParameterValue(), ParameterScope.TENANT, false, tenantOverride);
            }
            if (globalOverride != null) {
                return new ResolvedParameter(globalOverride.getParameterValue(), ParameterScope.GLOBAL, false, globalOverride);
            }
            return new ResolvedParameter(definition.defaultValue(), ParameterScope.GLOBAL, false, null);
        } else if (requestedScope == ParameterScope.TENANT) {
            if (tenantOverride != null) {
                return new ResolvedParameter(tenantOverride.getParameterValue(), ParameterScope.TENANT, true, tenantOverride);
            }
            if (globalOverride != null) {
                return new ResolvedParameter(globalOverride.getParameterValue(), ParameterScope.GLOBAL, false, globalOverride);
            }
            return new ResolvedParameter(definition.defaultValue(), ParameterScope.GLOBAL, false, null);
        } else {
            // GLOBAL
            if (globalOverride != null) {
                return new ResolvedParameter(globalOverride.getParameterValue(), ParameterScope.GLOBAL, true, globalOverride);
            }
            return new ResolvedParameter(definition.defaultValue(), ParameterScope.GLOBAL, false, null);
        }
    }
}
