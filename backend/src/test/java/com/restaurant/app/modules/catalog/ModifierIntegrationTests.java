package com.restaurant.app.modules.catalog;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.CreateModifierGroupRequest;
import com.restaurant.app.modules.catalog.api.CreateModifierRequest;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.catalog.api.ModifierGroupDto;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import com.restaurant.app.modules.pos.api.SelectedModifierRequest;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ModifierIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private InventoryPublicApi inventoryPublicApi;

    private MockMvc mockMvc;
    private String jwtToken;
    private UUID tenantId;
    private UUID branchId;
    private UUID demoDishId;
    private IngredientDto cheeseIngredient;

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
        this.jwtToken = responseNode.get("token").asText();

        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        this.tenantId = tenant.getId();

        Branch branch = branchRepository.findByTenantId(tenantId).getFirst();
        this.branchId = branch.getId();

        DishPriceDto dish = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId).getFirst();
        this.demoDishId = dish.dishId();

        List<IngredientDto> ings = catalogPublicApi.getAllIngredients(tenantId);
        this.cheeseIngredient = ings.stream()
                .filter(i -> i.name().toLowerCase().contains("queso") || i.name().toLowerCase().contains("carne") || i.name().toLowerCase().contains("pan"))
                .findFirst()
                .orElse(ings.getFirst());

        TenantContextHolder.set(new TenantContext(tenantId, branchId, UUID.randomUUID(), "admin", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe crear grupo de modificadores y asociarlo a un plato vía REST")
    void testCreateModifierGroupAndAssignToDishViaRest() throws Exception {
        String groupJson = String.format("""
            {
                "name": "Extras Hamburguesa",
                "minSelectable": 0,
                "maxSelectable": 3,
                "isRequired": false,
                "modifiers": [
                    {
                        "name": "Extra Queso Cheddar",
                        "additionalPrice": 6.50,
                        "ingredientId": "%s",
                        "quantityConsumed": 0.0500,
                        "sortOrder": 1
                    }
                ]
            }
            """, cheeseIngredient.id());

        MvcResult createResult = mockMvc.perform(post("/api/v1/catalog/modifier-groups")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Extras Hamburguesa"))
                .andExpect(jsonPath("$.modifiers[0].additionalPrice").value(6.50))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID groupId = UUID.fromString(createdJson.get("id").asText());

        // Asociar grupo al plato demo
        mockMvc.perform(post("/api/v1/catalog/dishes/" + demoDishId + "/modifier-groups")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(groupId))))
                .andExpect(status().isOk());

        // Verificar que getActiveMenuForBranch retorne el plato con el grupo de modificadores
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        DishPriceDto demoMenuDish = menu.stream().filter(d -> d.dishId().equals(demoDishId)).findFirst().orElseThrow();
        assertThat(demoMenuDish.modifierGroups()).isNotEmpty();
        assertThat(demoMenuDish.modifierGroups().getFirst().name()).isEqualTo("Extras Hamburguesa");
    }

    @Test
    @DisplayName("Debe vender plato con modificador en POS y deducir atómicamente el ingrediente extra en Kardex")
    void testSellDishWithModifierInPos_deductsExtraIngredientInKardex() throws Exception {
        // 1. Crear grupo de modificador con ingrediente
        CreateModifierGroupRequest groupReq = new CreateModifierGroupRequest(
                "Salsas y Extras",
                0,
                2,
                false,
                List.of(new CreateModifierRequest("Porción Extra", BigDecimal.valueOf(5.00), cheeseIngredient.id(), BigDecimal.valueOf(0.0400), 1))
        );
        ModifierGroupDto group = catalogPublicApi.createModifierGroup(tenantId, groupReq);
        catalogPublicApi.assignModifierGroupsToDish(tenantId, demoDishId, List.of(group.id()));

        // 2. Establecer stock inicial para el ingrediente del modificador
        BigDecimal initialStockQty = BigDecimal.valueOf(2.0000);
        try {
            inventoryPublicApi.registerInitialStock(tenantId, branchId, new InitialStockRequest(
                    cheeseIngredient.id(),
                    initialStockQty,
                    BigDecimal.valueOf(10.00)
            ));
        } catch (IllegalStateException e) {
            // Ya inicializado previamente en demo data
        }

        BigDecimal beforeStock = inventoryPublicApi.getAvailableStock(tenantId, branchId, cheeseIngredient.id()).orElse(BigDecimal.ZERO);

        // 3. Venta en POS de 2 unidades del plato con el modificador extra seleccionado
        UUID clientTxId = UUID.randomUUID();
        SelectedModifierRequest modSelection = new SelectedModifierRequest(
                group.modifiers().getFirst().id(),
                "Porción Extra",
                BigDecimal.valueOf(5.00),
                cheeseIngredient.id(),
                BigDecimal.valueOf(0.0400)
        );

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
                demoDishId,
                2,
                BigDecimal.valueOf(35.00),
                "Con extra",
                null,
                List.of(),
                List.of(modSelection)
        );

        CreateOrderRequest orderReq = new CreateOrderRequest(
                clientTxId,
                branchId,
                null,
                "CASH",
                "Venta con modificador",
                List.of(itemReq)
        );

        OrderDto order = posPublicApi.placeOrder(orderReq);
        // Precio unitario efectivo: 35.00 + 5.00 = 40.00. Subtotal x2 = 80.00
        assertThat(order.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(80.00));

        // 4. Aguardar ejecución asíncrona del listener en Spring Modulith
        for (int i = 0; i < 20; i++) {
            BigDecimal currentStock = inventoryPublicApi.getAvailableStock(tenantId, branchId, cheeseIngredient.id()).orElse(beforeStock);
            if (currentStock.compareTo(beforeStock) < 0) {
                break;
            }
            Thread.sleep(100);
        }

        BigDecimal afterStock = inventoryPublicApi.getAvailableStock(tenantId, branchId, cheeseIngredient.id()).orElse(beforeStock);
        // Cantidad extra consumida: 0.0400 * 2 = 0.0800
        assertThat(afterStock).isLessThan(beforeStock);
    }
}
