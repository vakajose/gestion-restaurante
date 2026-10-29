package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.modules.configuration.api.ParameterScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CascadingParameterResolverTest {

    @Mock
    private SystemParameterValueRepository repository;

    private ParameterCacheService cacheService;
    private CascadingParameterResolver resolver;

    private UUID tenantId;
    private UUID branchId;

    @BeforeEach
    void setUp() {
        cacheService = new ParameterCacheService();
        resolver = new CascadingParameterResolver(repository, cacheService);
        tenantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Acierto en memoria (Cache Hit) no consulta la base de datos")
    void cacheHitBypassesDatabase() {
        cacheService.put(tenantId, branchId, "pos.display_dish_images", "false");

        String value = resolver.resolveEffectiveValue("pos.display_dish_images", branchId, tenantId);

        assertThat(value).isEqualTo("false");
    }

    @Test
    @DisplayName("Fallback 1: Override en Sucursal tiene precedencia sobre Empresa y Default")
    void branchOverrideTakesPrecedence() {
        SystemParameterValue branchVal = new SystemParameterValue(
            "pos.display_dish_images", ParameterScope.BRANCH, tenantId, branchId, "false", null, null
        );
        SystemParameterValue tenantVal = new SystemParameterValue(
            "pos.display_dish_images", ParameterScope.TENANT, tenantId, null, "true", null, null
        );

        when(repository.findOverridesHierarchical("pos.display_dish_images", tenantId, branchId))
            .thenReturn(List.of(branchVal, tenantVal));

        String value = resolver.resolveEffectiveValue("pos.display_dish_images", branchId, tenantId);

        assertThat(value).isEqualTo("false");
        // Verifica que se guardó en caché
        assertThat(cacheService.get(tenantId, branchId, "pos.display_dish_images")).contains("false");
    }

    @Test
    @DisplayName("Fallback 2: Si no hay fila en Sucursal, hereda de Empresa")
    void inheritsFromTenantWhenNoBranchRow() {
        SystemParameterValue tenantVal = new SystemParameterValue(
            "inventory.deduction_mode", ParameterScope.TENANT, tenantId, null, "MANUAL_ISOLATED", null, null
        );

        when(repository.findOverridesHierarchical("inventory.deduction_mode", tenantId, branchId))
            .thenReturn(List.of(tenantVal));

        String value = resolver.resolveEffectiveValue("inventory.deduction_mode", branchId, tenantId);

        assertThat(value).isEqualTo("MANUAL_ISOLATED");
    }

    @Test
    @DisplayName("Fallback 3: Si no hay fila en BD, hereda el valor por defecto del catálogo")
    void inheritsFromCodeDefaultWhenNoRowsInDatabase() {
        when(repository.findOverridesHierarchical("inventory.deduction_mode", tenantId, branchId))
            .thenReturn(List.of());

        String value = resolver.resolveEffectiveValue("inventory.deduction_mode", branchId, tenantId);

        assertThat(value).isEqualTo("AUTOMATIC_PERMISSIVE");
    }
}
