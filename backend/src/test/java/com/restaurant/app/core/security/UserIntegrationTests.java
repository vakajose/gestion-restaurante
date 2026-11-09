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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private MockMvc mockMvc;
    private String adminToken;
    private Tenant testTenant;
    private Branch testBranch;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Obtener o crear tenant y branch de prueba
        testTenant = tenantRepository.findBySlug("demo-restaurant").orElseThrow();
        testBranch = branchRepository.findByTenantId(testTenant.getId()).getFirst();

        // Obtener token admin
        String loginJson = """
            {
                "tenantIdentifier": "demo-restaurant",
                "login": "admin",
                "password": "admin123"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        this.adminToken = root.path("token").asText();
    }

    @Test
    @DisplayName("GET /api/v1/users lista los colaboradores del tenant actual")
    void listUsersSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("POST /api/v1/users crea un nuevo usuario colaborador con contraseña encriptada")
    void createUserSuccess() throws Exception {
        String uniqueUser = "mesero_" + UUID.randomUUID().toString().substring(0, 8);
        String createJson = String.format("""
            {
                "username": "%s",
                "fullName": "Juan Carlos Mesero",
                "phoneNumber": "+591 71234567",
                "email": "%s@restaurant.com",
                "password": "password123",
                "role": "CASHIER",
                "branchId": "%s"
            }
            """, uniqueUser, uniqueUser, testBranch.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value(uniqueUser))
            .andExpect(jsonPath("$.fullName").value("Juan Carlos Mesero"))
            .andExpect(jsonPath("$.phoneNumber").value("+591 71234567"))
            .andExpect(jsonPath("$.role").value("CASHIER"))
            .andExpect(jsonPath("$.branchId").value(testBranch.getId().toString()))
            .andExpect(jsonPath("$.isActive").value(true))
            .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdId = UUID.fromString(node.path("id").asText());

        AppUser savedUser = appUserRepository.findById(createdId).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123");
        assertThat(savedUser.getPasswordHash()).startsWith("$argon2");
        assertThat(savedUser.getFullName()).isEqualTo("Juan Carlos Mesero");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("+591 71234567");
    }

    @Test
    @DisplayName("POST /api/v1/users con nombre de usuario duplicado en el mismo tenant retorna 400 ProblemDetail")
    void createUserWithDuplicateUsernameFails() throws Exception {
        String createJson = String.format("""
            {
                "username": "admin",
                "email": "admin_duplicado@restaurant.com",
                "password": "password123",
                "role": "CASHIER",
                "branchId": "%s"
            }
            """, testBranch.getId());

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ya existe")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} actualiza datos del colaborador")
    void updateUserSuccess() throws Exception {
        // Crear usuario inicial
        String username = "colab_" + UUID.randomUUID().toString().substring(0, 8);
        String createJson = String.format("""
            {
                "username": "%s",
                "email": "%s@test.com",
                "password": "password123",
                "role": "KITCHEN",
                "branchId": "%s"
            }
            """, username, username, testBranch.getId());

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn();

        UUID userId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asText());

        // Actualizar rol a BRANCH_MANAGER, fullName y phoneNumber
        String updateJson = String.format("""
            {
                "username": "%s",
                "fullName": "Juan Carlos Actualizado",
                "phoneNumber": "78888888",
                "email": "%s_actualizado@test.com",
                "role": "BRANCH_MANAGER",
                "branchId": "%s",
                "isActive": true
            }
            """, username, username, testBranch.getId());

        mockMvc.perform(put("/api/v1/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("BRANCH_MANAGER"))
            .andExpect(jsonPath("$.fullName").value("Juan Carlos Actualizado"))
            .andExpect(jsonPath("$.phoneNumber").value("78888888"))
            .andExpect(jsonPath("$.email").value(username + "_actualizado@test.com"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status conmuta estado de activación del colaborador")
    void toggleUserStatusSuccess() throws Exception {
        String username = "toggle_" + UUID.randomUUID().toString().substring(0, 8);
        String createJson = String.format("""
            {
                "username": "%s",
                "email": "%s@test.com",
                "password": "password123",
                "role": "CASHIER",
                "branchId": "%s"
            }
            """, username, username, testBranch.getId());

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated())
            .andReturn();

        UUID userId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asText());

        // Desactivar
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status?isActive=false")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(false));

        // Reactivar
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status?isActive=true")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/profile retorna datos del usuario autenticado")
    void getProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/profile")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.role").value("ADMIN_TENANT"))
            .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/profile/password actualiza contraseña propia con éxito")
    void changePasswordSuccess() throws Exception {
        // Crear usuario dedicado para probar cambio de clave
        String username = "pwduser_" + UUID.randomUUID().toString().substring(0, 8);
        String createJson = String.format("""
            {
                "username": "%s",
                "email": "%s@test.com",
                "password": "initialPassword123",
                "role": "CASHIER",
                "branchId": "%s"
            }
            """, username, username, testBranch.getId());

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
            .andExpect(status().isCreated());

        // Login con usuario dedicado
        String userLoginJson = String.format("""
            {
                "tenantIdentifier": "demo-restaurant",
                "login": "%s",
                "password": "initialPassword123"
            }
            """, username);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userLoginJson))
            .andExpect(status().isOk())
            .andReturn();

        String userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("token").asText();

        // 1. Fallo con contraseña actual errónea
        String wrongPwdJson = """
            {
                "currentPassword": "wrongInitialPassword",
                "newPassword": "newSecretPassword123"
            }
            """;
        mockMvc.perform(put("/api/v1/profile/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongPwdJson))
            .andExpect(status().isUnauthorized());

        // 2. Éxito con contraseña actual correcta
        String correctPwdJson = """
            {
                "currentPassword": "initialPassword123",
                "newPassword": "newSecretPassword123"
            }
            """;
        mockMvc.perform(put("/api/v1/profile/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctPwdJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Contraseña actualizada exitosamente"));

        // 3. Verificar que puede iniciar sesión con la nueva contraseña
        String newLoginJson = String.format("""
            {
                "tenantIdentifier": "demo-restaurant",
                "login": "%s",
                "password": "newSecretPassword123"
            }
            """, username);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newLoginJson))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/profile actualiza nombre completo, teléfono y correo del usuario actual")
    void updateProfileSuccess() throws Exception {
        String updateJson = """
            {
                "fullName": "Carlos Administrador Real",
                "phoneNumber": "70012345",
                "email": "carlos_admin@test.com"
            }
            """;

        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Carlos Administrador Real"))
            .andExpect(jsonPath("$.phoneNumber").value("70012345"))
            .andExpect(jsonPath("$.email").value("carlos_admin@test.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/profile/pin actualiza el PIN de supervisor exitosamente")
    void updateProfilePinSuccess() throws Exception {
        String pinJson = """
            {
                "pin": "4321"
            }
            """;

        mockMvc.perform(put("/api/v1/profile/pin")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pinJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("PIN de supervisor actualizado exitosamente"));
    }
}
