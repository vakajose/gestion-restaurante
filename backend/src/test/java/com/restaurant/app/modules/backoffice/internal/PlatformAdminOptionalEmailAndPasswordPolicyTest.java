package com.restaurant.app.modules.backoffice.internal;

import tools.jackson.databind.ObjectMapper;
import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminOptionalEmailAndPasswordPolicyTest {

    @Mock
    private PlatformAdminRepository adminRepository;
    @Mock
    private PlatformRefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PlatformAuditService auditService;
    @Mock
    private PlatformSettingsService settingsService;

    @Test
    @DisplayName("Permite crear platform_admin sin email cuando la política mínima es 8 caracteres")
    void shouldCreateAdminWithoutEmailAndRespectDynamicMinLength() {
        when(settingsService.getLong(PlatformSettingsService.KEY_PLATFORM_PASSWORD_MIN_LENGTH, 12L)).thenReturn(8L);
        when(settingsService.getBoolean(PlatformSettingsService.KEY_PLATFORM_PASSWORD_REQUIRE_COMPLEXITY, false))
            .thenReturn(false);

        PlatformPasswordPolicyService policyService = new PlatformPasswordPolicyService(settingsService);
        PlatformAdminManagementService service = new PlatformAdminManagementService(
            adminRepository, refreshTokenRepository, passwordEncoder, auditService, policyService
        );

        when(adminRepository.findByUsernameNormalized("operador_sin_mail")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Clave123")).thenReturn("$2a$encoded");
        when(adminRepository.save(any(PlatformAdmin.class))).thenAnswer(inv -> {
            PlatformAdmin a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
            return a;
        });

        PlatformPrincipal actor = new PlatformPrincipal(
            UUID.randomUUID(), "superadmin", null, "Super Admin", PlatformRole.SUPERADMIN, false
        );

        var dto = service.create(
            new PlatformAdminManagementService.CreatePlatformAdminRequest(
                "operador_sin_mail",
                "   ",
                "Operador Sin Correo",
                "Clave123",
                PlatformRole.PLATFORM_OPERATOR
            ),
            actor,
            "127.0.0.1"
        );

        assertThat(dto.username()).isEqualTo("operador_sin_mail");
        assertThat(dto.email()).isNull();
    }

    @Test
    @DisplayName("Aplica complejidad obligatoria cuando security.platform_password_require_complexity=true")
    void shouldEnforceComplexityWhenEnabled() {
        when(settingsService.getLong(PlatformSettingsService.KEY_PLATFORM_PASSWORD_MIN_LENGTH, 12L)).thenReturn(8L);
        when(settingsService.getBoolean(PlatformSettingsService.KEY_PLATFORM_PASSWORD_REQUIRE_COMPLEXITY, false))
            .thenReturn(true);

        PlatformPasswordPolicyService policyService = new PlatformPasswordPolicyService(settingsService);

        assertThatThrownBy(() -> policyService.validate("solo8car"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("mayúscula");

        policyService.validate("ClaveSegura1!");
    }

    @Test
    @DisplayName("PlatformJwtService preserva email=null al emitir y decodificar JWT de plataforma")
    void shouldPreserveNullEmailInPlatformJwt() {
        PlatformJwtService jwtService = new PlatformJwtService(
            new ObjectMapper(),
            "test-secret-key-with-at-least-32-characters-long!",
            900L
        );
        PlatformAdmin admin = new PlatformAdmin("op", null, "Operador", "hash", false, PlatformRole.PLATFORM_OPERATOR);
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        String token = jwtService.generateToken(admin);
        var principal = jwtService.extractPrincipal(token).orElseThrow();

        assertThat(principal.username()).isEqualTo("op");
        assertThat(principal.email()).isNull();
    }
}
