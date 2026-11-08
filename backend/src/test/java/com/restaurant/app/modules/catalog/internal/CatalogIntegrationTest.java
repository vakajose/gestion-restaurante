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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    @DisplayName("Debe subir exitosamente una foto WebP a un plato y persistir su URL")
    void shouldUploadDishImageSuccessfully() throws Exception {
        MvcResult dishesResult = mockMvc.perform(get("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode dishesNode = objectMapper.readTree(dishesResult.getResponse().getContentAsString());
        UUID dishId = UUID.fromString(dishesNode.get(0).get("id").asText());

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
            "file",
            "tacos.webp",
            "image/webp",
            "mock-webp-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/catalog/dishes/" + dishId + "/image")
                .file(mockFile)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(dishId.toString()))
            .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/uploads/dishes/dish_" + dishId + "_")))
            .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.endsWith(".webp")));
    }

    @Test
    @DisplayName("Debe rechazar subida de foto mayor a 1 MB con 413 Payload Too Large")
    void shouldRejectDishImageLargerThanOneMegabyte() throws Exception {
        MvcResult dishesResult = mockMvc.perform(get("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode dishesNode = objectMapper.readTree(dishesResult.getResponse().getContentAsString());
        UUID dishId = UUID.fromString(dishesNode.get(0).get("id").asText());

        byte[] giantPayload = new byte[1048576 + 50];
        org.springframework.mock.web.MockMultipartFile giantFile = new org.springframework.mock.web.MockMultipartFile(
            "file",
            "giant.webp",
            "image/webp",
            giantPayload
        );

        mockMvc.perform(multipart("/api/v1/catalog/dishes/" + dishId + "/image")
                .file(giantFile)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("Debe rechazar subida de foto con extensión no permitida con 400 Bad Request")
    void shouldRejectDishImageWithInvalidExtension() throws Exception {
        MvcResult dishesResult = mockMvc.perform(get("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode dishesNode = objectMapper.readTree(dishesResult.getResponse().getContentAsString());
        UUID dishId = UUID.fromString(dishesNode.get(0).get("id").asText());

        org.springframework.mock.web.MockMultipartFile invalidFile = new org.springframework.mock.web.MockMultipartFile(
            "file",
            "script.sh",
            "application/x-sh",
            "echo hello".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/catalog/dishes/" + dishId + "/image")
                .file(invalidFile)
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe validar unicidad de code_prefix al crear categorías y rechazar duplicados con 409")
    void shouldValidateUniqueCodePrefixWhenCreatingCategory() throws Exception {
        String uniquePrefix = "PIZ";
        String uniqueName = "Pizzas Especiales " + UUID.randomUUID().toString().substring(0, 5);
        String categoryJson = String.format("""
            {
                "name": "%s",
                "icon": "🍕",
                "sortOrder": 10,
                "codePrefix": "%s"
            }
            """, uniqueName, uniquePrefix);

        mockMvc.perform(post("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codePrefix").value("PIZ"));

        // Intentar crear otra categoría con el mismo prefijo
        String duplicateJson = String.format("""
            {
                "name": "Otra Categoria %s",
                "icon": "🍽️",
                "sortOrder": 11,
                "codePrefix": "%s"
            }
            """, UUID.randomUUID().toString().substring(0, 5), uniquePrefix);

        mockMvc.perform(post("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateJson))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("prefijo 'PIZ'")));
    }

    @Test
    @DisplayName("Debe sugerir el siguiente SKU secuencial con política MAX + 1 y formato simple")
    void shouldSuggestNextDishCodeWithMaxPlusOne() throws Exception {
        // 1. Obtener una categoría existente (ej. Bebidas)
        MvcResult catsResult = mockMvc.perform(get("/api/v1/catalog/categories")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode catsNode = objectMapper.readTree(catsResult.getResponse().getContentAsString());
        UUID categoryId = UUID.fromString(catsNode.get(0).get("id").asText());
        String expectedPrefix = catsNode.get(0).get("codePrefix").asText();

        // 2. Invocar endpoint /dishes/next-code
        MvcResult nextCodeResult = mockMvc.perform(get("/api/v1/catalog/dishes/next-code")
                .param("categoryId", categoryId.toString())
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prefix").value(expectedPrefix))
            .andExpect(jsonPath("$.suggestedSequence").isNumber())
            .andExpect(jsonPath("$.nextCode").isString())
            .andReturn();

        JsonNode responseNode = objectMapper.readTree(nextCodeResult.getResponse().getContentAsString());
        int currentSeq = responseNode.get("suggestedSequence").asInt();
        String currentNextCode = responseNode.get("nextCode").asText();
        assertThat(currentNextCode).isEqualTo(expectedPrefix + "-" + currentSeq);

        // 3. Crear un plato con un correlativo mayor (ej. 99)
        int customSeq = 99;
        String customCode = expectedPrefix + "-" + customSeq;
        String dishJson = String.format("""
            {
                "code": "%s",
                "name": "Plato Test Secuencia %s",
                "categoryId": "%s",
                "salePrice": 25.50,
                "isActive": true
            }
            """, customCode, UUID.randomUUID().toString().substring(0, 5), categoryId);

        mockMvc.perform(post("/api/v1/catalog/dishes")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(dishJson))
            .andExpect(status().isCreated());

        // 4. Volver a consultar y verificar que sugiere 100
        mockMvc.perform(get("/api/v1/catalog/dishes/next-code")
                .param("categoryId", categoryId.toString())
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prefix").value(expectedPrefix))
            .andExpect(jsonPath("$.suggestedSequence").value(100))
            .andExpect(jsonPath("$.nextCode").value(expectedPrefix + "-100"));
    }
}
