package com.restaurant.app.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtService = new JwtService(
            objectMapper,
            "secret-key-for-unit-testing-at-least-32-chars-long!",
            3600
        );
    }

    @Test
    @DisplayName("Generar token y validar que los claims coincidan con TenantContext")
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "admin", "ADMIN_TENANT", tenantId, branchId);
        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();

        Optional<TenantContext> contextOpt = jwtService.extractTenantContext(token);
        assertThat(contextOpt).isPresent();

        TenantContext context = contextOpt.get();
        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.username()).isEqualTo("admin");
        assertThat(context.role()).isEqualTo("ADMIN_TENANT");
        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.branchId()).isEqualTo(branchId);
    }

    @Test
    @DisplayName("Token expirado debe ser inválido")
    void expiredTokenIsInvalid() {
        // JwtService con tiempo de expiración negativo (-10s)
        JwtService expiredService = new JwtService(
            objectMapper,
            "secret-key-for-unit-testing-at-least-32-chars-long!",
            -10
        );

        String token = expiredService.generateToken(UUID.randomUUID(), "test", "CASHIER", UUID.randomUUID(), null);
        assertThat(expiredService.validateToken(token)).isFalse();
        assertThat(expiredService.extractTenantContext(token)).isEmpty();
    }

    @Test
    @DisplayName("Token alterado o con firma incorrecta debe ser rechazado")
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(UUID.randomUUID(), "admin", "ADMIN_TENANT", UUID.randomUUID(), null);
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidSignature";

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThat(jwtService.extractTenantContext(tamperedToken)).isEmpty();
    }
}
