package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.configuration.api.UpdateParameterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ConfigurationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConfigurationPublicApi configurationPublicApi;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

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
        this.adminToken = "Bearer " + responseNode.get("token").asText();
    }

    @Test
    @DisplayName("Petición sin autenticación es rechazada con 401")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/v1/parameters"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/parameters retorna lista de parámetros y cabecera ETag")
    void getParametersReturnsListAndEtag() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/parameters")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.ETAG))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.key == 'pos.display_dish_images')].defaultValue").value("true"))
            .andReturn();

        String eTag = result.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(eTag).isNotNull();

        // Si se envía el mismo ETag en If-None-Match, debe retornar 304 Not Modified
        mockMvc.perform(get("/api/v1/parameters")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .header(HttpHeaders.IF_NONE_MATCH, eTag))
            .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("GET /api/v1/parameters?category=POS retorna únicamente parámetros de la categoría POS")
    void getParametersFilteredByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/parameters?category=POS")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].category", everyItem(is("POS"))));
    }

    @Test
    @DisplayName("Flujo completo de mutación atómica PATCH, hot-reload y reset DELETE")
    void patchAndResetParameterCycle() throws Exception {
        // 1. Mutar pos.display_dish_images a "false" para la sucursal actual
        UpdateParameterRequest patchRequest = new UpdateParameterRequest(
            ParameterScope.BRANCH,
            null,
            "false",
            null,
            "Optimizar comanda"
        );

        mockMvc.perform(patch("/api/v1/parameters/pos.display_dish_images")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("pos.display_dish_images"))
            .andExpect(jsonPath("$.effectiveValue").value("false"))
            .andExpect(jsonPath("$.isOverridden").value(true));

        // 2. Comprobar que GET subsecuente refleja el cambio (Hot-Reload)
        mockMvc.perform(get("/api/v1/parameters?category=POS")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.key == 'pos.display_dish_images')].effectiveValue").value("false"))
            .andExpect(jsonPath("$[?(@.key == 'pos.display_dish_images')].isOverridden").value(true));

        // 3. Restablecer mediante DELETE (Reset to Default)
        mockMvc.perform(delete("/api/v1/parameters/pos.display_dish_images")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isNoContent());

        // 4. Comprobar que volvió al valor por defecto corporativo
        mockMvc.perform(get("/api/v1/parameters?category=POS")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.key == 'pos.display_dish_images')].effectiveValue").value("true"))
            .andExpect(jsonPath("$[?(@.key == 'pos.display_dish_images')].isOverridden").value(false));
    }

    @Test
    @DisplayName("Mutación de parámetro crítico (HIGH) exige justificación y reautenticación")
    void patchCriticalParameterSecurityFlow() throws Exception {
        // A. Intento sin contraseña de confirmación -> Rechazado 400
        UpdateParameterRequest reqNoPwd = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            null,
            "Cierre mensual"
        );

        mockMvc.perform(patch("/api/v1/parameters/inventory.deduction_mode")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqNoPwd)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reautenticación con contraseña es obligatoria")));

        // B. Intento con contraseña errónea -> Rechazado
        UpdateParameterRequest reqWrongPwd = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            "wrong_password_123",
            "Cierre mensual de inventario estricto"
        );

        mockMvc.perform(patch("/api/v1/parameters/inventory.deduction_mode")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqWrongPwd)))
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Contraseña de confirmación incorrecta")));

        // C. Intento con credenciales correctas -> 200 OK
        UpdateParameterRequest reqValid = new UpdateParameterRequest(
            ParameterScope.TENANT,
            null,
            "AUTOMATIC_STRICT",
            "admin123",
            "Cierre mensual de inventario estricto completado"
        );

        mockMvc.perform(patch("/api/v1/parameters/inventory.deduction_mode")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqValid)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectiveValue").value("AUTOMATIC_STRICT"))
            .andExpect(jsonPath("$.isOverridden").value(true));

        // Limpieza: restablecer para no afectar otros tests
        mockMvc.perform(delete("/api/v1/parameters/inventory.deduction_mode?scope=TENANT")
                .header(HttpHeaders.AUTHORIZATION, adminToken))
            .andExpect(status().isNoContent());
    }
}
