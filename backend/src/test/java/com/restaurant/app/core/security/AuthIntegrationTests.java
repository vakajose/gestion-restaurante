package com.restaurant.app.core.security;

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
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
        // 1. Iniciar sesión para obtener token
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
        String token = responseNode.path("token").asText();
        assertThat(token).isNotBlank();

        // 2. Invocar /api/v1/auth/me con Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.role").value("ADMIN_TENANT"))
            .andExpect(jsonPath("$.email").value("admin@restaurant.com"));
    }

    @Test
    @DisplayName("Llamada no autorizada a /api/v1/auth/me sin token retorna 401 ProblemDetail")
    void meEndpointWithoutTokenFails() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }
}
