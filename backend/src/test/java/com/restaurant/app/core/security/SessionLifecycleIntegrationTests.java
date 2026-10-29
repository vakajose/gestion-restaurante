package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.api.CreateUserRequest;
import com.restaurant.app.core.security.api.SessionPublicApi.ActiveSessionDto;
import com.restaurant.app.core.security.api.SessionPublicApi.LogoutRequest;
import com.restaurant.app.core.security.api.SessionPublicApi.RefreshRequest;
import com.restaurant.app.core.security.api.SessionPublicApi.RefreshResponse;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.core.security.internal.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SessionLifecycleIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserPublicApi userPublicApi;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Tenant tenant;
    private Branch branch;
    private UserSummaryDto cashierUser;
    private String username;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
        tenant = tenantRepository.save(new Tenant(
            "Empresa Sesiones " + uniqueSuffix,
            "776655" + uniqueSuffix,
            "sesiones-" + uniqueSuffix,
            null
        ));
        branch = branchRepository.save(new Branch(tenant.getId(), "Sucursal Sesiones", "America/La_Paz"));
        userPublicApi.seedDefaultRolesForTenant(tenant.getId());

        username = "cajero_sesion_" + uniqueSuffix;
        cashierUser = userPublicApi.createUser(
            tenant.getId(),
            new CreateUserRequest(
                username,
                "Cajero Sesiones",
                "cajero@" + uniqueSuffix + ".bo",
                "SesionPass123!",
                "CASHIER",
                branch.getId()
            )
        );
    }

    @Test
    @DisplayName("1. Login emite Refresh Token persistido como hash SHA-256 y rotación atómica vincula replaced_by_id")
    void shouldIssueHashedRefreshTokenAndRotateAtomically() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .header("User-Agent", "POS-Tablet-Android/14")
                .header("X-Forwarded-For", "192.168.10.25")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "SesionPass123!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        LoginResponse loginDto = objectMapper.readValue(
            loginRes.getResponse().getContentAsString(),
            LoginResponse.class
        );
        String token1 = loginDto.refreshToken();
        String hash1 = SessionServiceImpl.computeSha256Hex(token1);

        RefreshToken storedToken1 = refreshTokenRepository.findByTokenHash(hash1).orElseThrow();
        assertThat(storedToken1.getUserId()).isEqualTo(cashierUser.id());
        assertThat(storedToken1.getDeviceInfo()).contains("POS-Tablet-Android/14");
        assertThat(storedToken1.getIpAddress()).isEqualTo("192.168.10.25");
        assertThat(storedToken1.getRevokedAt()).isNull();

        // Rotar el refresh token
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                .header("User-Agent", "POS-Tablet-Android/14")
                .header("X-Forwarded-For", "192.168.10.25")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(token1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        RefreshResponse refreshDto = objectMapper.readValue(
            refreshRes.getResponse().getContentAsString(),
            RefreshResponse.class
        );
        String token2 = refreshDto.refreshToken();
        assertThat(token2).isNotEqualTo(token1);

        String hash2 = SessionServiceImpl.computeSha256Hex(token2);
        RefreshToken storedToken2 = refreshTokenRepository.findByTokenHash(hash2).orElseThrow();
        RefreshToken updatedToken1 = refreshTokenRepository.findByTokenHash(hash1).orElseThrow();

        assertThat(updatedToken1.getRevokedAt()).isNotNull();
        assertThat(updatedToken1.getReplacedById()).isEqualTo(storedToken2.getId());
        assertThat(storedToken2.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("2. Detección automática de reúso de Refresh Token revoca toda la familia de sesiones activas del usuario (ADR-0011)")
    void shouldRevokeAllUserSessionsWhenRotatedTokenIsReused() throws Exception {
        // 1. Login inicial -> Token 1
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "SesionPass123!"))))
            .andExpect(status().isOk())
            .andReturn();

        LoginResponse loginDto = objectMapper.readValue(
            loginRes.getResponse().getContentAsString(),
            LoginResponse.class
        );
        String token1 = loginDto.refreshToken();

        // 2. Rotación legítima -> emite Token 2 e invalida Token 1
        MvcResult refreshRes = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(token1))))
            .andExpect(status().isOk())
            .andReturn();

        RefreshResponse refreshDto = objectMapper.readValue(
            refreshRes.getResponse().getContentAsString(),
            RefreshResponse.class
        );
        String token2 = refreshDto.refreshToken();

        // 3. Atacante (o cliente desincronizado) intenta reutilizar el Token 1 ya rotado -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(token1))))
            .andExpect(status().isUnauthorized());

        // 4. Verificar que el Token 2 también fue revocado automáticamente por detección de brecha
        String hash2 = SessionServiceImpl.computeSha256Hex(token2);
        RefreshToken storedToken2 = refreshTokenRepository.findByTokenHash(hash2).orElseThrow();
        assertThat(storedToken2.getRevokedAt()).isNotNull();

        // 5. Intentar usar el Token 2 ahora también devuelve 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(token2))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Listado de sesiones activas, revocación selectiva por ID y cierre de sesión explícito (logout)")
    void shouldListActiveSessionsRevokeSingleSessionAndLogout() throws Exception {
        // Sesión A (Terminal Caja 1)
        MvcResult loginARes = mockMvc.perform(post("/api/v1/auth/login")
                .header("User-Agent", "Chrome-Desktop-Caja1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "SesionPass123!"))))
            .andExpect(status().isOk())
            .andReturn();
        LoginResponse sessionA = objectMapper.readValue(loginARes.getResponse().getContentAsString(), LoginResponse.class);

        // Sesión B (Tablet Mesero)
        MvcResult loginBRes = mockMvc.perform(post("/api/v1/auth/login")
                .header("User-Agent", "Safari-iPad-Mesero")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "SesionPass123!"))))
            .andExpect(status().isOk())
            .andReturn();
        LoginResponse sessionB = objectMapper.readValue(loginBRes.getResponse().getContentAsString(), LoginResponse.class);

        // Listar sesiones activas desde Sesión A identificando la sesión actual
        MvcResult sessionsRes = mockMvc.perform(get("/api/v1/auth/sessions")
                .header("Authorization", "Bearer " + sessionA.accessToken())
                .param("currentRefreshToken", sessionA.refreshToken()))
            .andExpect(status().isOk())
            .andReturn();

        List<ActiveSessionDto> activeSessions = objectMapper.readValue(
            sessionsRes.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );
        assertThat(activeSessions).hasSize(2);
        assertThat(activeSessions.stream().filter(ActiveSessionDto::isCurrent).count()).isEqualTo(1);

        ActiveSessionDto tabletSession = activeSessions.stream()
            .filter(s -> !s.isCurrent())
            .findFirst()
            .orElseThrow();

        // Revocar remotamente la Sesión B (Tablet) desde la Sesión A
        mockMvc.perform(delete("/api/v1/auth/sessions/" + tabletSession.id())
                .header("Authorization", "Bearer " + sessionA.accessToken()))
            .andExpect(status().isNoContent());

        // Verificar que la Sesión B ya no puede refrescar su token
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(sessionB.refreshToken()))))
            .andExpect(status().isUnauthorized());

        // Cerrar explícitamente la Sesión A (Logout)
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LogoutRequest(sessionA.refreshToken()))))
            .andExpect(status().isOk());

        // Verificar que la Sesión A tampoco puede refrescar su token tras el logout
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(sessionA.refreshToken()))))
            .andExpect(status().isUnauthorized());
    }
}
