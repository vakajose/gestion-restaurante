package com.restaurant.app.modules.catalog.internal;

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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CatalogIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Login as demo admin to obtain JWT
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
        this.jwtToken = responseNode.get("token").asText();
    }

    @Test
    @DisplayName("Debe rechazar peticiones no autenticadas con 401")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe listar las categorías demo inicializadas")
    void shouldListDemoCategories() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItems("Hamburguesas", "Bebidas", "Acompañamientos")));
    }

    @Test
    @DisplayName("Debe listar los ingredientes demo inicializados")
    void shouldListDemoIngredients() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/ingredients")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItems(
                "Carne de Res molida",
                "Pan de Hamburguesa",
                "Queso Cheddar",
                "Papas",
                "Coca Cola 500ml"
            )));
    }

    @Test
    @DisplayName("Debe listar los platos demo inicializados y verificar la receta de la hamburguesa clásica")
    void shouldListDemoDishesAndVerifyBurgerRecipe() throws Exception {
        MvcResult dishesResult = mockMvc.perform(get("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItems(
                "Hamburguesa Clásica",
                "Papas Fritas",
                "Coca Cola 500ml"
            )))
            .andReturn();

        JsonNode dishesNode = objectMapper.readTree(dishesResult.getResponse().getContentAsString());
        UUID burgerDishId = null;
        for (JsonNode dish : dishesNode) {
            if ("Hamburguesa Clásica".equals(dish.get("name").asText())) {
                burgerDishId = UUID.fromString(dish.get("id").asText());
                break;
            }
        }
        assertThat(burgerDishId).isNotNull();

        // Verificar la receta (BOM): carne, pan, queso
        mockMvc.perform(get("/api/v1/catalog/dishes/" + burgerDishId + "/recipe")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dishName").value("Hamburguesa Clásica"))
            .andExpect(jsonPath("$.items", hasSize(3)))
            .andExpect(jsonPath("$.items[*].ingredientName").value(org.hamcrest.Matchers.hasItems(
                "Carne de Res molida", "Pan de Hamburguesa", "Queso Cheddar"
            )));
    }

    @Test
    @DisplayName("Flujo completo CRUD de plato, sobreescritura por sucursal y clonación vía REST")
    void fullCatalogRestWorkflow() throws Exception {
        // 1. Obtener una categoría
        MvcResult catResult = mockMvc.perform(get("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode cats = objectMapper.readTree(catResult.getResponse().getContentAsString());
        UUID categoryId = UUID.fromString(cats.get(0).get("id").asText());

        // 2. Crear un plato nuevo
        String newDishCode = "DISH-TACO-" + UUID.randomUUID().toString().substring(0, 8);
        String createDishJson = String.format("""
            {
                "categoryId": "%s",
                "code": "%s",
                "name": "Tacos al Pastor",
                "description": "Tres tacos tradicionales con piña",
                "salePrice": 28.00,
                "isActive": true
            }
            """, categoryId, newDishCode);

        MvcResult createResult = mockMvc.perform(post("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDishJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(newDishCode))
            .andExpect(jsonPath("$.salePrice").value(28.00))
            .andReturn();

        JsonNode createdDish = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID dishId = UUID.fromString(createdDish.get("id").asText());

        // 3. Consultar por ID
        mockMvc.perform(get("/api/v1/catalog/dishes/" + dishId)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Tacos al Pastor"));

        // 4. Modificar plato (PUT)
        String updateDishJson = String.format("""
            {
                "categoryId": "%s",
                "code": "%s",
                "name": "Tacos al Pastor Especiales",
                "description": "Cuatro tacos con piña y guacamole",
                "salePrice": 32.50,
                "isActive": true
            }
            """, categoryId, newDishCode);

        mockMvc.perform(put("/api/v1/catalog/dishes/" + dishId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateDishJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Tacos al Pastor Especiales"))
            .andExpect(jsonPath("$.salePrice").value(32.50));

        // 5. Asignar receta (PUT recipe)
        MvcResult ingsResult = mockMvc.perform(get("/api/v1/catalog/ingredients")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode ings = objectMapper.readTree(ingsResult.getResponse().getContentAsString());
        UUID meatIngId = UUID.fromString(ings.get(0).get("id").asText());

        String recipeJson = String.format("""
            [
                {
                    "ingredientId": "%s",
                    "quantity": 0.2000
                }
            ]
            """, meatIngId);

        mockMvc.perform(put("/api/v1/catalog/dishes/" + dishId + "/recipe")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(recipeJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].quantity").value(0.2000));

        // 6. Sobreescritura por sucursal
        UUID branchId = UUID.randomUUID();
        String overrideJson = String.format("""
            {
                "branchId": "%s",
                "isAvailable": true,
                "priceOverride": 36.00
            }
            """, branchId);

        mockMvc.perform(post("/api/v1/catalog/dishes/" + dishId + "/branch-override")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(overrideJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectivePrice").value(36.00))
            .andExpect(jsonPath("$.hasOverride").value(true));

        // 7. Clonación con trazabilidad
        String cloneCode = "CLONE-" + UUID.randomUUID().toString().substring(0, 8);
        String cloneJson = String.format("""
            {
                "newCode": "%s",
                "newName": "Tacos al Pastor Clonados",
                "newSalePrice": 30.00
            }
            """, cloneCode);

        mockMvc.perform(post("/api/v1/catalog/dishes/" + dishId + "/clone")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cloneJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(cloneCode))
            .andExpect(jsonPath("$.clonedFromId").value(dishId.toString()))
            .andExpect(jsonPath("$.salePrice").value(30.00));
    }
}
