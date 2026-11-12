package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PlatformAuthAndBootstrapIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformAdminRepository adminRepository;

    @Autowired
    private PlatformRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @Autowired
    private JwtService tenantJwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Bootstrap inicial debe crear al superadmin con must_change_password=true y nunca sobrescribirlo en reinicio normal")
    void bootstrapShouldCreateInitialAdminOnceAndNeverOverwriteOnNormalRestart() {
        PlatformAdmin admin = adminRepository.findByUsernameNormalized("superadmin").orElseThrow();
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isMustChangePassword()).isTrue();
        String originalHash = admin.getPasswordHash();

        PlatformBootstrapInitializer normalRestart = new PlatformBootstrapInitializer(
            adminRepository,
            refreshTokenRepository,
            passwordEncoder,
            environment,
            null,
            "superadmin",
            "superadmin@platform.local",
            "OtraClaveMuySegura123!",
            "Platform Root",
            false,
            false
        );
        normalRestart.executeBootstrap(true, false);

        PlatformAdmin afterRestart = adminRepository.findByUsernameNormalized("superadmin").orElseThrow();
        assertThat(afterRestart.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    @DisplayName("Bootstrap estricto debe fallar rápido si faltan variables o clave < 12 caracteres cuando platform_admins está vacía")
    void strictBootstrapShouldFailFastWhenConfigMissingOnEmptyTable() {
        PlatformAdminRepository emptyRepo = org.mockito.Mockito.mock(PlatformAdminRepository.class);
        org.mockito.Mockito.when(emptyRepo.count()).thenReturn(0L);

        PlatformBootstrapInitializer missingEnv = new PlatformBootstrapInitializer(
            emptyRepo,
            refreshTokenRepository,
            passwordEncoder,
            environment,
            null,
            "",
            "",
            "",
            "Platform Root",
            false,
            false
        );

        assertThatThrownBy(() -> missingEnv.executeBootstrap(true, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fail-fast");

        PlatformBootstrapInitializer shortPassword = new PlatformBootstrapInitializer(
            emptyRepo,
            refreshTokenRepository,
            passwordEncoder,
            environment,
            null,
            "superadmin",
            "admin@platform.local",
            "short123",
            "Platform Root",
            false,
            false
        );

        assertThatThrownBy(() -> shortPassword.executeBootstrap(true, false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("12");
    }

    @Test
    @DisplayName("Ciclo completo Backoffice Auth: login, rotación refresh, detección reúso, change-password >=12 chars, aislamiento ERP y Break-Glass")
    void fullPlatformAuthIsolationAndBreakGlassLifecycle() throws Exception {
        // Asegurar estado conocido inicial del superadmin dev
        PlatformBootstrapInitializer resetToKnown = new PlatformBootstrapInitializer(
            adminRepository,
            refreshTokenRepository,
            passwordEncoder,
            environment,
            null,
            "superadmin",
            "superadmin@platform.local",
            "SuperAdminDev123!",
            "Platform Superadmin",
            true,
            false
        );
        resetToKnown.executeBootstrap(false, true);

        // 1. Login Backoffice exitoso con mustChangePassword=true
        MvcResult loginResult = mockMvc.perform(post("/api/v1/backoffice/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login": "superadmin", "password": "SuperAdminDev123!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.admin.username").value("superadmin"))
            .andExpect(jsonPath("$.admin.mustChangePassword").value(true))
            .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.path("accessToken").asText();
        String refreshToken1 = loginJson.path("refreshToken").asText();

        // 2. Acceso autorizado a /api/v1/backoffice/auth/me
        mockMvc.perform(get("/api/v1/backoffice/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("superadmin"))
            .andExpect(jsonPath("$.mustChangePassword").value(true));

        // 3. Aislamiento estricto: token PLATFORM rechazado en ruta ERP tenant-scoped
        mockMvc.perform(get("/api/v1/finance/shifts")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());

        // 4. Aislamiento estricto: token TENANT ADMIN_TENANT rechazado en Backoffice
        String tenantAdminToken = tenantJwtService.generateToken(
            UUID.randomUUID(),
            "admin",
            "ADMIN_TENANT",
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        mockMvc.perform(get("/api/v1/backoffice/auth/me")
                .header("Authorization", "Bearer " + tenantAdminToken))
            .andExpect(status().isUnauthorized());

        // 5. Refresh rotativo válido + detección de reúso
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/backoffice/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(refreshToken1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        String refreshToken2 = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
            .path("refreshToken").asText();
        assertThat(refreshToken2).isNotEqualTo(refreshToken1);

        // Reusar refreshToken1 debe disparar revocación de toda la familia (incluyendo refreshToken2)
        mockMvc.perform(post("/api/v1/backoffice/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(refreshToken1)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/backoffice/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken": "%s"}
                    """.formatted(refreshToken2)))
            .andExpect(status().isUnauthorized());

        // 6. Change-password rechaza < 12 caracteres y acepta >= 12 limpiando mustChangePassword
        mockMvc.perform(post("/api/v1/backoffice/auth/change-password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword": "SuperAdminDev123!", "newPassword": "corta123"}
                    """))
            .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/v1/backoffice/auth/change-password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"currentPassword": "SuperAdminDev123!", "newPassword": "NuevaClaveSuperSegura2026!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(false));

        // Login con clave nueva muestra mustChangePassword=false
        mockMvc.perform(post("/api/v1/backoffice/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login": "superadmin", "password": "NuevaClaveSuperSegura2026!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.admin.mustChangePassword").value(false));

        // 7. Break-Glass Reset vuelve a forzar must_change_password=true y cambia la clave
        PlatformBootstrapInitializer breakGlass = new PlatformBootstrapInitializer(
            adminRepository,
            refreshTokenRepository,
            passwordEncoder,
            environment,
            null,
            "superadmin",
            "superadmin@platform.local",
            "BreakGlassTemp2026!",
            "Platform Superadmin",
            true,
            false
        );
        breakGlass.executeBootstrap(true, true);

        mockMvc.perform(post("/api/v1/backoffice/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"login": "superadmin", "password": "BreakGlassTemp2026!"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.admin.mustChangePassword").value(true));

        // Restaurar clave dev para no afectar otros tests del contexto H2 compartido
        resetToKnown.executeBootstrap(false, true);
    }
}
