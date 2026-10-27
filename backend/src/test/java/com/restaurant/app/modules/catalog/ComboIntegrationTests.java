package com.restaurant.app.modules.catalog;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.ComboDto;
import com.restaurant.app.modules.catalog.api.CreateComboItemOptionRequest;
import com.restaurant.app.modules.catalog.api.CreateComboItemRequest;
import com.restaurant.app.modules.catalog.api.CreateComboRequest;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.StockBalanceDto;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
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

import java.math.BigDecimal;
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
public class ComboIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private InventoryPublicApi inventoryPublicApi;

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    private MockMvc mockMvc;
    private UUID tenantId;
    private UUID branchId;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        Branch branch = branchRepository.findByTenantId(tenant.getId()).get(0);
        this.tenantId = tenant.getId();
        this.branchId = branch.getId();

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
        this.authToken = root.path("token").asText();

        TenantContextHolder.set(new TenantContext(tenant.getId(), branch.getId(), UUID.randomUUID(), "admin", "ADMIN_TENANT"));
    }

    @Test
    @DisplayName("Debe listar el combo demo seeded en el catálogo de la sucursal")
    void testGetActiveCombos() {
        List<ComboDto> combos = catalogPublicApi.getActiveCombosForBranch(tenantId, branchId);
        assertThat(combos).isNotEmpty();

        ComboDto combo = combos.stream()
            .filter(c -> c.code().equals("COMBO-CLASICO-01"))
            .findFirst()
            .orElseThrow();

        assertThat(combo.name()).isEqualTo("Combo Burger Clásico");
        assertThat(combo.basePrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(combo.items()).hasSize(3);
    }

    @Test
    @DisplayName("Debe crear un nuevo combo flexible via REST API y recuperarlo")
    void testCreateComboViaRest() throws Exception {
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        DishPriceDto burger = menu.stream().filter(d -> d.name().contains("Hamburguesa")).findFirst().orElseThrow();
        DishPriceDto fries = menu.stream().filter(d -> d.name().contains("Papas")).findFirst().orElseThrow();

        CreateComboRequest request = new CreateComboRequest(
            null,
            burger.categoryId(),
            "COMBO-DUO-01",
            "Combo Dúo Burger",
            "2 Hamburguesas + Papas para compartir",
            new BigDecimal("75.00"),
            List.of(
                new CreateComboItemRequest("Burgers", "FIXED", true, burger.dishId(), 2, 1, List.of(
                    new CreateComboItemOptionRequest(burger.dishId(), BigDecimal.ZERO, true)
                )),
                new CreateComboItemRequest("Papas Grandes", "FIXED", true, fries.dishId(), 1, 2, List.of(
                    new CreateComboItemOptionRequest(fries.dishId(), BigDecimal.ZERO, true)
                ))
            )
        );

        mockMvc.perform(post("/api/v1/catalog/combos")
                .header("Authorization", "Bearer " + authToken)
                .header("X-Branch-Id", branchId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("COMBO-DUO-01"))
            .andExpect(jsonPath("$.name").value("Combo Dúo Burger"))
            .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("Venta de combo en POS debe deducir recursivamente las recetas de todos sus componentes en Kardex")
    void testSellComboInPos_deductsComponentsRecursively() {
        List<ComboDto> combos = catalogPublicApi.getActiveCombosForBranch(tenantId, branchId);
        ComboDto combo = combos.stream().filter(c -> c.code().equals("COMBO-CLASICO-01")).findFirst().orElseThrow();

        List<StockBalanceDto> stockBefore = inventoryPublicApi.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto carneBefore = stockBefore.stream().filter(s -> s.ingredientName().toLowerCase().contains("carne")).findFirst().orElseThrow();
        StockBalanceDto papasBefore = stockBefore.stream().filter(s -> s.ingredientName().toLowerCase().contains("papas")).findFirst().orElseThrow();
        StockBalanceDto cokeBefore = stockBefore.stream().filter(s -> s.ingredientName().toLowerCase().contains("coca")).findFirst().orElseThrow();

        // Platos componentes del combo: Hamburguesa (0.150 KG carne) + Papas (0.250 KG papas) + Coca (1 UNIT)
        // Vender 2 combos
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(
            combo.items().get(0).defaultDishId(),
            2,
            combo.basePrice(),
            "2 Combos Burger Clásico",
            combo.id(),
            List.of(
                combo.items().get(0).defaultDishId(),
                combo.items().get(1).defaultDishId(),
                combo.items().get(2).defaultDishId()
            )
        );

        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            null,
            "CASH",
            "Pedido con Combos",
            List.of(itemReq)
        );

        TenantContextHolder.set(new TenantContext(tenantId, branchId, UUID.randomUUID(), "admin", "ADMIN_TENANT"));
        OrderDto order = posPublicApi.placeOrder(orderReq);
        assertThat(order).isNotNull();
        assertThat(order.orderStatus()).isEqualTo("PAID");

        // Esperar a que el listener asíncrono de Spring Modulith complete la deducción
        for (int i = 0; i < 30; i++) {
            List<StockBalanceDto> current = inventoryPublicApi.getBranchStockSummary(tenantId, branchId);
            StockBalanceDto carne = current.stream().filter(s -> s.ingredientName().toLowerCase().contains("carne")).findFirst().orElseThrow();
            if (carne.currentQuantity().compareTo(carneBefore.currentQuantity()) < 0) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        // Verificar deducción en Kardex:
        // 2 combos -> 2 * 0.150 = 0.300 KG carne
        // 2 combos -> 2 * 0.250 = 0.500 KG papas
        // 2 combos -> 2 * 1.0 = 2 UNIT coca
        List<StockBalanceDto> stockAfter = inventoryPublicApi.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto carneAfter = stockAfter.stream().filter(s -> s.ingredientName().toLowerCase().contains("carne")).findFirst().orElseThrow();
        StockBalanceDto papasAfter = stockAfter.stream().filter(s -> s.ingredientName().toLowerCase().contains("papas")).findFirst().orElseThrow();
        StockBalanceDto cokeAfter = stockAfter.stream().filter(s -> s.ingredientName().toLowerCase().contains("coca")).findFirst().orElseThrow();

        assertThat(carneAfter.currentQuantity()).isEqualByComparingTo(carneBefore.currentQuantity().subtract(new BigDecimal("0.3000")));
        assertThat(papasAfter.currentQuantity()).isEqualByComparingTo(papasBefore.currentQuantity().subtract(new BigDecimal("0.5000")));
        assertThat(cokeAfter.currentQuantity()).isEqualByComparingTo(cokeBefore.currentQuantity().subtract(new BigDecimal("2.0000")));
    }
}
