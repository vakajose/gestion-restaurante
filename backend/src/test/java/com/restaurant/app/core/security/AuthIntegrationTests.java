package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Login exitoso con tenantIdentifier por slug 'demo-restaurant' incluye authorizedBranchIds")
    void loginWithTenantSlugSuccess() throws Exception {
        String requestJson = """
            {
                "tenantIdentifier": "demo-restaurant",
                "login": "admin",
                "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.role").value("ADMIN_TENANT"))
            .andExpect(jsonPath("$.user.tenantSlug").value("demo-restaurant"))
            .andExpect(jsonPath("$.user.permissions").isArray())
            .andExpect(jsonPath("$.user.permissions").isNotEmpty())
            .andExpect(jsonPath("$.user.authorizedBranchIds").isArray())
            .andExpect(jsonPath("$.user.authorizedBranchIds").isNotEmpty());
    }

    @Test
    @DisplayName("Login exitoso con tenantIdentifier por NIT '12345678'")
    void loginWithTenantNitSuccess() throws Exception {
        String requestJson = """
            {
                "tenantIdentifier": "12345678",
                "login": "admin",
                "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.tenantSlug").value("demo-restaurant"));
    }

    @Test
    @DisplayName("Login fallido con tenantIdentifier inexistente retorna 401")
    void loginWithNonExistentTenantFails() throws Exception {
        String requestJson = """
            {
                "tenantIdentifier": "restaurante-fantasma",
                "login": "admin",
                "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Login exitoso con username 'admin' y contraseña 'admin123'")
    void loginWithUsernameSuccess() throws Exception {
        String requestJson = """
            {
                "login": "admin",
                "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.role").value("ADMIN_TENANT"))
            .andExpect(jsonPath("$.user.tenantId").isNotEmpty())
            .andExpect(jsonPath("$.user.branchId").isNotEmpty());
    }

    @Test
    @DisplayName("Login exitoso con email 'admin@restaurant.com' y contraseña 'admin123'")
    void loginWithEmailSuccess() throws Exception {
        String requestJson = """
            {
                "login": "admin@restaurant.com",
                "password": "admin123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.email").value("admin@restaurant.com"));
    }

    @Test
    @DisplayName("Login fallido con contraseña incorrecta retorna 401 Unauthorized y ProblemDetail")
    void loginWithWrongPasswordFails() throws Exception {
        String requestJson = """
            {
                "login": "admin",
                "password": "wrong-password"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Login fallido con usuario inexistente retorna 401 Unauthorized y ProblemDetail")
    void loginWithNonExistentUserFails() throws Exception {
        String requestJson = """
            {
                "login": "nonexistent_user",
                "password": "password"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("Llamada protegida a /api/v1/auth/me con token JWT Bearer")
    void meEndpointWithBearerToken() throws Exception {
        String token = obtainAdminToken();

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.role").value("ADMIN_TENANT"))
            .andExpect(jsonPath("$.email").value("admin@restaurant.com"))
            .andExpect(jsonPath("$.authorizedBranchIds").isArray());
    }

    @Test
    @DisplayName("Cabecera X-Branch-Id válida cambia dinámicamente el branchId en el contexto")
    void branchHeaderOverridesBranchContext() throws Exception {
        String token = obtainAdminToken();

        Tenant tenant = tenantRepository.findBySlug("demo-restaurant").orElseThrow();
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        assertThat(branches.size()).isGreaterThanOrEqualTo(2);

        Branch targetBranch = branches.get(1);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Branch-Id", targetBranch.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.branchId").value(targetBranch.getId().toString()));
    }

    @Test
    @DisplayName("Cabecera X-Branch-Id con UUID malformado retorna 400 ProblemDetail")
    void branchHeaderWithMalformedUuidFails() throws Exception {
        String token = obtainAdminToken();

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Branch-Id", "not-a-valid-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Formato de cabecera X-Branch-Id inválido"));
    }

    @Test
    @DisplayName("Cabecera X-Branch-Id de sucursal ajena retorna 403 Forbidden ProblemDetail")
    void branchHeaderWithForeignBranchFails() throws Exception {
        String token = obtainAdminToken();

        // Crear otro tenant y branch ajeno
        Tenant otherTenant = tenantRepository.save(new Tenant("Otro Restaurant", "88888888", "otro-restaurant-" + UUID.randomUUID()));
        Branch foreignBranch = branchRepository.save(new Branch(otherTenant.getId(), "Sucursal Ajena", "America/La_Paz"));

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Branch-Id", foreignBranch.getId().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("La sucursal no existe o no pertenece a la empresa actual"));
    }

    @Test
    @DisplayName("Cajero intentando acceder a una sucursal no autorizada vía X-Branch-Id retorna 403")
    void cashierAccessingUnauthorizedBranchFails() throws Exception {
        // 1. Login como cajero
        String cashierLogin = """
            {
                "tenantIdentifier": "demo-restaurant",
                "login": "cajero1",
                "password": "cajero123"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cashierLogin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("CASHIER"))
            .andExpect(jsonPath("$.user.authorizedBranchIds.length()").value(1))
            .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = responseNode.path("token").asText();

        // Obtener la segunda sucursal de la cual el cajero no forma parte
        Tenant tenant = tenantRepository.findBySlug("demo-restaurant").orElseThrow();
        List<Branch> branches = branchRepository.findByTenantId(tenant.getId());
        Branch otherBranch = branches.get(1);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .header("X-Branch-Id", otherBranch.getId().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("Acceso no autorizado a la sucursal solicitada"));
    }

    @Test
    @DisplayName("Llamada no autorizada a /api/v1/auth/me sin token retorna 401 ProblemDetail")
    void meEndpointWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    private String obtainAdminToken() throws Exception {
        String loginJson = """
            {
                "login": "admin",
                "password": "admin123"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return responseNode.path("token").asText();
    }
}
