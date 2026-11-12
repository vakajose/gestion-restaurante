package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformJwtServiceTest {

    private static final String SHARED_SECRET = "shared-secret-key-for-testing-audience-isolation-32chars!";

    private PlatformJwtService platformJwtService;
    private JwtService tenantJwtService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        platformJwtService = new PlatformJwtService(objectMapper, SHARED_SECRET, 900);
        tenantJwtService = new JwtService(objectMapper, SHARED_SECRET, 900);
    }

    @Test
    @DisplayName("Debe emitir y validar token PLATFORM extrayendo PlatformPrincipal")
    void shouldIssueAndVerifyPlatformToken() {
        PlatformAdmin admin = new PlatformAdmin(
            "superadmin",
            "superadmin@platform.local",
            "Platform Root",
            "hashed",
            true
        );
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        String token = platformJwtService.generateToken(admin);
        Optional<PlatformPrincipal> principalOpt = platformJwtService.extractPrincipal(token);

        assertThat(principalOpt).isPresent();
        PlatformPrincipal principal = principalOpt.get();
        assertThat(principal.adminId()).isEqualTo(admin.getId());
        assertThat(principal.username()).isEqualTo("superadmin");
        assertThat(principal.email()).isEqualTo("superadmin@platform.local");
        assertThat(principal.mustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("Aislamiento de audiencia: tokens ERP y PLATFORM deben rechazarse mutuamente incluso con el mismo secreto")
    void shouldRejectCrossAudienceTokensEvenWithSameSecret() {
        PlatformAdmin admin = new PlatformAdmin(
            "superadmin",
            "superadmin@platform.local",
            "Platform Root",
            "hashed",
            false
        );
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        String platformToken = platformJwtService.generateToken(admin);
        String tenantToken = tenantJwtService.generateToken(
            UUID.randomUUID(),
            "admin",
            "ADMIN_TENANT",
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        assertThat(tenantJwtService.validateToken(platformToken)).isFalse();
        assertThat(tenantJwtService.extractTenantContext(platformToken)).isEmpty();

        assertThat(platformJwtService.extractPrincipal(tenantToken)).isEmpty();
    }
}
