package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.AuthService;
import com.restaurant.app.core.security.LoginRequest;
import com.restaurant.app.core.security.LoginResponse;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.ParameterUpdatedEvent;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.configuration.api.SystemParameterDto;
import com.restaurant.app.modules.configuration.api.UpdateParameterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private SystemParameterValueRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthService authService;

    private ParameterCacheService cacheService;
    private CascadingParameterResolver resolver;
    private ConfigurationServiceImpl service;

    private UUID tenantId;
    private UUID branchId;
    private UUID userId;

    private enum TestMode {
        MANUAL_ISOLATED,
        AUTOMATIC_PERMISSIVE,
        AUTOMATIC_STRICT
    }

    @BeforeEach
    void setUp() {
        cacheService = new ParameterCacheService();
        resolver = new CascadingParameterResolver(repository, cacheService);
        service = new ConfigurationServiceImpl(repository, resolver, cacheService, eventPublisher, authService);

        tenantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Lectura tipada: getBoolean, getInt, getDouble, getEnum")
    void typedGetters() {
        when(repository.findOverridesHierarchical(any(), any(), any())).thenReturn(List.of());

        boolean boolVal = service.getBoolean(SystemParameter.POS_DISPLAY_DISH_IMAGES, branchId, tenantId);
        assertThat(boolVal).isTrue();

        int intVal = service.getInt(SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES, branchId, tenantId);
        assertThat(intVal).isEqualTo(15);

        double doubleVal = service.getDouble(SystemParameter.FINANCE_MAX_PETTY_CASH_EXPENSE, branchId, tenantId);
        assertThat(doubleVal).isEqualTo(100.0);

        TestMode enumVal = service.getEnum(SystemParameter.INVENTORY_DEDUCTION_MODE, branchId, tenantId, TestMode.class);
        assertThat(enumVal).isEqualTo(TestMode.AUTOMATIC_PERMISSIVE);
    }

    @Test
    @DisplayName("setOverride persiste en BD, invalida la caché y emite ParameterUpdatedEvent")
    void setOverrideSuccess() {
        when(repository.findOverridesHierarchical(any(), any(), any())).thenReturn(List.of());
        when(repository.findByParameterKeyAndScopeAndBranchId("pos.display_dish_images", ParameterScope.BRANCH, branchId))
            .thenReturn(Optional.empty());

        service.setOverride(
            SystemParameter.POS_DISPLAY_DISH_IMAGES,
            ParameterScope.BRANCH,
            tenantId,
            branchId,
            "false",
            userId,
            "Optimizar fila en mostrador"
        );

        ArgumentCaptor<SystemParameterValue> entityCaptor = ArgumentCaptor.forClass(SystemParameterValue.class);
        verify(repository).save(entityCaptor.capture());
        SystemParameterValue saved = entityCaptor.getValue();
        assertThat(saved.getParameterKey()).isEqualTo("pos.display_dish_images");
        assertThat(saved.getParameterValue()).isEqualTo("false");
        assertThat(saved.getScope()).isEqualTo(ParameterScope.BRANCH);
        assertThat(saved.getUpdatedBy()).isEqualTo(userId);

        ArgumentCaptor<ParameterUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ParameterUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ParameterUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.parameterKey()).isEqualTo("pos.display_dish_images");
        assertThat(event.newValue()).isEqualTo("false");
        assertThat(event.scope()).isEqualTo(ParameterScope.BRANCH);
    }

    @Test
    @DisplayName("resetOverride elimina fila en BD, invalida caché y emite evento con valor restaurado")
    void resetOverrideSuccess() {
        when(repository.findOverridesHierarchical(any(), any(), any())).thenReturn(List.of());

        service.resetOverride(
            SystemParameter.POS_DISPLAY_DISH_IMAGES,
            ParameterScope.BRANCH,
            tenantId,
            branchId
        );

        verify(repository).deleteByBranchScope("pos.display_dish_images", ParameterScope.BRANCH, branchId);

        ArgumentCaptor<ParameterUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(ParameterUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ParameterUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.parameterKey()).isEqualTo("pos.display_dish_images");
        assertThat(event.newValue()).isEqualTo("true");
    }

    @Test
    @DisplayName("Modificación de parámetro crítico exige contraseña y motivo >= 10 caracteres")
    void criticalParameterRequiresPasswordAndReason() {
        UpdateParameterRequest requestNoPwd = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            null,
            "Cierre de inventario mensual"
        );

        assertThatThrownBy(() -> service.updateParameterFromRequest(
            "inventory.deduction_mode",
            requestNoPwd,
            tenantId,
            userId,
            "admin",
            "slug-demo"
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("reautenticación con contraseña es obligatoria");

        UpdateParameterRequest requestShortReason = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            "mypassword",
            "Corto"
        );

        assertThatThrownBy(() -> service.updateParameterFromRequest(
            "inventory.deduction_mode",
            requestShortReason,
            tenantId,
            userId,
            "admin",
            "slug-demo"
        )).isInstanceOf(ValidationException.class)
            .hasMessageContaining("al menos 10 caracteres");
    }

    @Test
    @DisplayName("Modificación de parámetro crítico falla si contraseña de confirmación es incorrecta")
    void criticalParameterFailsWithBadPassword() {
        UpdateParameterRequest request = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            "wrong_pwd",
            "Cierre de inventario mensual completado"
        );

        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThatThrownBy(() -> service.updateParameterFromRequest(
            "inventory.deduction_mode",
            request,
            tenantId,
            userId,
            "admin",
            "slug-demo"
        )).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Contraseña de confirmación incorrecta");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Modificación de parámetro crítico exitosa reautentica y aplica cambio")
    void criticalParameterSuccessWithValidPassword() {
        UpdateParameterRequest request = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            "correct_pwd",
            "Cierre de inventario mensual completado"
        );

        SystemParameterValue savedEntity = new SystemParameterValue(
            "inventory.deduction_mode",
            ParameterScope.TENANT,
            tenantId,
            null,
            "AUTOMATIC_STRICT",
            userId,
            "Cierre mensual de inventario completado"
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(LoginResponse.bearer("token", null));
        when(repository.findOverridesHierarchical(any(), any(), any())).thenReturn(List.of(savedEntity));

        SystemParameterDto result = service.updateParameterFromRequest(
            "inventory.deduction_mode",
            request,
            tenantId,
            userId,
            "admin",
            "slug-demo"
        );

        assertThat(result.effectiveValue()).isEqualTo("AUTOMATIC_STRICT");
        verify(repository).save(any(SystemParameterValue.class));
        verify(eventPublisher).publishEvent(any(ParameterUpdatedEvent.class));
    }
}
