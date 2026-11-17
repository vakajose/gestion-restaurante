package com.restaurant.app.modules.catalog;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.BranchServiceSessionDto;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.ComboDto;
import com.restaurant.app.modules.catalog.api.ConfigureServiceSessionRequest;
import com.restaurant.app.modules.catalog.api.CourseType;
import com.restaurant.app.modules.catalog.api.CreateComboItemOptionRequest;
import com.restaurant.app.modules.catalog.api.CreateComboItemRequest;
import com.restaurant.app.modules.catalog.api.CreateComboRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuItemRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.CreateModifierGroupRequest;
import com.restaurant.app.modules.catalog.api.CreateModifierRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.ModifierGroupDto;
import com.restaurant.app.modules.catalog.api.PriceMode;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.StockAdjustmentRequest;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.OrderItemSelectionRequest;
import com.restaurant.app.modules.pos.api.PosPublicApi;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DailyMenuAndServiceSessionIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DailyMenuPublicApi dailyMenuPublicApi;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private FinancePublicApi financePublicApi;

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private InventoryPublicApi inventoryPublicApi;

    @Autowired
    private ConfigurationPublicApi configurationPublicApi;

    private MockMvc mockMvc;
    private String authToken;
    private UUID tenantId;
    private UUID baseBranchId;
    private UUID isolatedBranchId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        this.tenantId = tenant.getId();
        this.baseBranchId = branchRepository.findByTenantId(tenantId).getFirst().getId();

        Branch isolated = branchRepository.save(new Branch(tenantId, "Sucursal Jornada " + UUID.randomUUID().toString().substring(0, 8), "America/La_Paz"));
        this.isolatedBranchId = isolated.getId();

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
        this.adminUserId = UUID.fromString(root.path("user").path("id").asText());

        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, adminUserId, "admin", "ADMIN_TENANT"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TASK-634: Jornada de Sucursal bloquea si auto_open=false, auto-abre FULL_CATALOG si auto_open=true, y comparte pool entre 2 cajeros")
    void testBranchServiceSessionLifecycleAndSharedQuotaPool() {
        // 1. Con auto_open=false y sin sesión configurada -> bloquea apertura de caja
        configurationPublicApi.setOverride(
            SystemParameter.FINANCE_AUTO_OPEN_FULL_CATALOG_SESSION,
            ParameterScope.BRANCH,
            tenantId,
            isolatedBranchId,
            "false",
            adminUserId,
            "Test bloqueo sin sesión"
        );

        assertThatThrownBy(() -> financePublicApi.openShift(new OpenShiftRequest(isolatedBranchId, new BigDecimal("100.00"))))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("no ha configurado la jornada");

        // 2. En otra sucursal sin sesión con auto_open=true (default) -> crea sesión FULL_CATALOG automáticamente
        Branch autoBranch = branchRepository.save(new Branch(tenantId, "Sucursal Auto Full", "America/La_Paz"));
        CashShiftDto autoShift = financePublicApi.openShift(new OpenShiftRequest(autoBranch.getId(), new BigDecimal("50.00")));
        assertThat(autoShift.serviceSessionId()).isNotNull();
        BranchServiceSessionDto autoSession = dailyMenuPublicApi.getActiveServiceSession(tenantId, autoBranch.getId()).orElseThrow();
        assertThat(autoSession.menuMode()).isEqualTo("FULL_CATALOG");
        assertThat(autoSession.status()).isEqualTo("ACTIVE");

        // 3. Configurar modo INDIVIDUAL_PER_CASHIER + Jornada DAILY_MENU con 1 plato limitado (cupo=5) y 1 ilimitado
        configurationPublicApi.setOverride(
            SystemParameter.FINANCE_SHIFT_CONCURRENCY_MODE,
            ParameterScope.BRANCH,
            tenantId,
            isolatedBranchId,
            "INDIVIDUAL_PER_CASHIER",
            adminUserId,
            "Dos cajeros simultáneos"
        );

        List<DishPriceDto> dishes = catalogPublicApi.getActiveMenuForBranch(tenantId, isolatedBranchId);
        assertThat(dishes.size()).isGreaterThanOrEqualTo(2);
        UUID limitedDishId = dishes.get(0).dishId();
        UUID unlimitedDishId = dishes.get(1).dishId();

        DailyMenuDto menu = dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            isolatedBranchId,
            "Almuerzo Compartido",
            "Pool único por sucursal",
            "WEDNESDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(
                new CreateDailyMenuItemRequest(limitedDishId, CourseType.MAIN, 5, null, false, 1, true, false),
                new CreateDailyMenuItemRequest(unlimitedDishId, CourseType.BEVERAGE, 0, null, false, 2, true, true)
            ),
            List.of()
        ));

        BranchServiceSessionDto configuredSession = dailyMenuPublicApi.configureServiceSession(
            tenantId,
            adminUserId,
            new ConfigureServiceSessionRequest(
                isolatedBranchId,
                "DAILY_MENU",
                menu.id(),
                null,
                null,
                "Jornada preparada por admin"
            )
        );
        assertThat(configuredSession.status()).isEqualTo("CONFIGURED");

        // 4. Cajero 1 y Cajero 2 abren turno -> ambos quedan vinculados a la MISMA jornada
        UUID cashier1 = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, cashier1, "cajero1", "CASHIER"));
        CashShiftDto shift1 = financePublicApi.openShift(new OpenShiftRequest(isolatedBranchId, new BigDecimal("100.00")));

        UUID cashier2 = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, cashier2, "cajero2", "CASHIER"));
        CashShiftDto shift2 = financePublicApi.openShift(new OpenShiftRequest(isolatedBranchId, new BigDecimal("100.00")));

        assertThat(shift1.serviceSessionId()).isEqualTo(configuredSession.id());
        assertThat(shift2.serviceSessionId()).isEqualTo(configuredSession.id());

        // Cajero 1 vende 3 raciones del limitado y 12 del ilimitado
        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, cashier1, "cajero1", "CASHIER"));
        posPublicApi.placeOrder(new CreateOrderRequest(
            UUID.randomUUID(),
            isolatedBranchId,
            shift1.id(),
            "CASH",
            "Venta cajero 1",
            List.of(
                new CreateOrderItemRequest(limitedDishId, 3, new BigDecimal("25.00"), null),
                new CreateOrderItemRequest(unlimitedDishId, 12, new BigDecimal("10.00"), null)
            )
        ));

        // Cajero 2 ve cupo restante = 2 en el limitado, e ilimitado nunca se agota
        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, cashier2, "cajero2", "CASHIER"));
        ShiftMenuDto menuForCashier2 = dailyMenuPublicApi.getShiftMenu(shift2.id()).orElseThrow();
        ShiftMenuItemDto limitedItemAfterC1 = menuForCashier2.items().stream()
            .filter(i -> i.dishId().equals(limitedDishId))
            .findFirst()
            .orElseThrow();
        ShiftMenuItemDto unlimitedItemAfterC1 = menuForCashier2.items().stream()
            .filter(i -> i.dishId().equals(unlimitedDishId))
            .findFirst()
            .orElseThrow();

        assertThat(limitedItemAfterC1.currentQuota()).isEqualTo(2);
        assertThat(limitedItemAfterC1.soldCount()).isEqualTo(3);
        assertThat(unlimitedItemAfterC1.isUnlimited()).isTrue();
        assertThat(unlimitedItemAfterC1.isSoldOut()).isFalse();
        assertThat(unlimitedItemAfterC1.soldCount()).isEqualTo(12);

        // Cajero 2 vende los 2 restantes del limitado -> queda SOLD OUT
        posPublicApi.placeOrder(new CreateOrderRequest(
            UUID.randomUUID(),
            isolatedBranchId,
            shift2.id(),
            "CARD",
            "Venta cajero 2",
            List.of(new CreateOrderItemRequest(limitedDishId, 2, new BigDecimal("25.00"), null))
        ));

        ShiftMenuItemDto limitedSoldOut = dailyMenuPublicApi.getShiftMenu(shift1.id()).orElseThrow()
            .items().stream().filter(i -> i.dishId().equals(limitedDishId)).findFirst().orElseThrow();
        assertThat(limitedSoldOut.currentQuota()).isEqualTo(0);
        assertThat(limitedSoldOut.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("TASK-634: Combo dinámico MENU_COURSE valida tiempos al vincular a menú, descuenta cupo y registra order_item_selections y Kardex")
    void testDynamicMenuCourseComboValidationAndOrderSelections() throws Exception {
        List<DishPriceDto> dishes = catalogPublicApi.getActiveMenuForBranch(tenantId, baseBranchId);
        // Encontrar platos con receta existente (demo: Hamburguesa Clásica y Refresco o Papas)
        DishPriceDto dish1 = dishes.get(0);
        DishPriceDto dish2 = dishes.get(1);

        // 1. Crear Combo Dinámico por tiempo de menú (STARTER + MAIN)
        CreateComboRequest dynamicComboReq = new CreateComboRequest(
            isolatedBranchId,
            dish1.categoryId(),
            "COMBO-ALM-DIN-" + UUID.randomUUID().toString().substring(0, 6),
            "Almuerzo Dinámico",
            "Sopa del menú + Segundo del menú",
            new BigDecimal("30.00"),
            List.of(
                new CreateComboItemRequest("Sopa a elección", "CHOICE", true, null, 1, 1, List.of(), "MENU_COURSE", CourseType.STARTER),
                new CreateComboItemRequest("Segundo a elección", "CHOICE", true, null, 1, 2, List.of(), "MENU_COURSE", CourseType.MAIN)
            )
        );
        MvcResult createComboRes = mockMvc.perform(post("/api/v1/catalog/combos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dynamicComboReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].selectionSource").value("MENU_COURSE"))
            .andExpect(jsonPath("$.items[0].courseType").value("STARTER"))
            .andReturn();

        ComboDto createdCombo = objectMapper.readValue(createComboRes.getResponse().getContentAsString(), ComboDto.class);
        TenantContextHolder.set(new TenantContext(tenantId, isolatedBranchId, adminUserId, "admin", "ADMIN_TENANT"));

        // 2. Intentar crear Menú del Día sin platos STARTER pero con el combo -> DEBE FALLAR validación
        assertThatThrownBy(() -> dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            isolatedBranchId,
            "Menú Inválido",
            null,
            "THURSDAY",
            PriceMode.FIXED_PACKAGE,
            new BigDecimal("30.00"),
            true,
            List.of(new CreateDailyMenuItemRequest(dish2.dishId(), CourseType.MAIN, 10, null, false, 1, false, false)),
            List.of(createdCombo.id())
        )))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("STARTER");

        // 3. Crear Menú del Día completo (STARTER + MAIN) con el combo vinculado y activar jornada
        DailyMenuDto validMenu = dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            isolatedBranchId,
            "Menú Almuerzo Completo",
            null,
            "THURSDAY",
            PriceMode.FIXED_PACKAGE,
            new BigDecimal("30.00"),
            true,
            List.of(
                new CreateDailyMenuItemRequest(dish1.dishId(), CourseType.STARTER, 6, null, false, 1, false, false),
                new CreateDailyMenuItemRequest(dish2.dishId(), CourseType.MAIN, 8, null, false, 2, true, false)
            ),
            List.of(createdCombo.id())
        ));

        assertThat(validMenu.comboIds()).containsExactly(createdCombo.id());

        dailyMenuPublicApi.configureServiceSession(
            tenantId,
            adminUserId,
            new ConfigureServiceSessionRequest(isolatedBranchId, "DAILY_MENU", validMenu.id(), null, null, null)
        );
        CashShiftDto shift = financePublicApi.openShift(new OpenShiftRequest(isolatedBranchId, new BigDecimal("100.00")));

        // Verificar que ShiftMenu trae el combo habilitado y allowIndividualSale correctamente
        ShiftMenuDto shiftMenu = dailyMenuPublicApi.getShiftMenu(shift.id()).orElseThrow();
        assertThat(shiftMenu.combos()).extracting(ComboDto::id).contains(createdCombo.id());

        // Registrar stock inicial para los ingredientes de dish1 y dish2 en la sucursal aislada
        var recipe1 = catalogPublicApi.getRecipeByDishId(dish1.dishId());
        var recipe2 = catalogPublicApi.getRecipeByDishId(dish2.dishId());
        BigDecimal initialIngredientStock = null;
        UUID trackedIngredientId = null;
        if (recipe1.isPresent() && !recipe1.get().items().isEmpty()) {
            for (var ri : recipe1.get().items()) {
                inventoryPublicApi.adjustStock(
                    tenantId,
                    isolatedBranchId,
                    new StockAdjustmentRequest(ri.ingredientId(), new BigDecimal("500.00"), new BigDecimal("10.00"), "Stock inicial recipe1"),
                    adminUserId
                );
            }
            trackedIngredientId = recipe1.get().items().getFirst().ingredientId();
            initialIngredientStock = inventoryPublicApi.getAvailableStock(tenantId, isolatedBranchId, trackedIngredientId)
                .orElse(BigDecimal.ZERO);
        }
        if (recipe2.isPresent() && !recipe2.get().items().isEmpty()) {
            for (var ri : recipe2.get().items()) {
                inventoryPublicApi.adjustStock(
                    tenantId,
                    isolatedBranchId,
                    new StockAdjustmentRequest(ri.ingredientId(), new BigDecimal("500.00"), new BigDecimal("10.00"), "Stock inicial recipe2"),
                    adminUserId
                );
            }
        }

        // 4. Vender 2 Almuerzos Dinámicos enviando dishId=null y selections reales
        OrderDto order = posPublicApi.placeOrder(new CreateOrderRequest(
            UUID.randomUUID(),
            isolatedBranchId,
            shift.id(),
            "CASH",
            "Mesa 3",
            List.of(new CreateOrderItemRequest(
                null,
                2,
                new BigDecimal("30.00"),
                null,
                createdCombo.id(),
                List.of(),
                List.of(),
                validMenu.id(),
                List.of(
                    new OrderItemSelectionRequest("Sopa", dish1.dishId(), 1, BigDecimal.ZERO),
                    new OrderItemSelectionRequest("Segundo", dish2.dishId(), 1, new BigDecimal("5.00"))
                )
            ))
        ));

        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().selections()).hasSize(2);
        assertThat(order.items().getFirst().dishName()).isEqualTo("Almuerzo Dinámico");

        // Verificar descuento de cupos reales en ambos platos elegidos (6-2=4, 8-2=6)
        ShiftMenuDto afterOrder = dailyMenuPublicApi.getShiftMenu(shift.id()).orElseThrow();
        ShiftMenuItemDto sItem = afterOrder.items().stream().filter(i -> i.dishId().equals(dish1.dishId())).findFirst().orElseThrow();
        ShiftMenuItemDto mItem = afterOrder.items().stream().filter(i -> i.dishId().equals(dish2.dishId())).findFirst().orElseThrow();
        assertThat(sItem.currentQuota()).isEqualTo(4);
        assertThat(mItem.currentQuota()).isEqualTo(6);

        // Verificar deducción real en Kardex por los ingredientes de dish1 elegido en selection
        if (trackedIngredientId != null && initialIngredientStock != null) {
            BigDecimal finalStock = initialIngredientStock;
            for (int i = 0; i < 30; i++) {
                finalStock = inventoryPublicApi.getAvailableStock(tenantId, isolatedBranchId, trackedIngredientId)
                    .orElse(BigDecimal.ZERO);
                if (finalStock.compareTo(initialIngredientStock) < 0) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            assertThat(finalStock).isLessThan(initialIngredientStock);
        }
    }

    @Test
    @DisplayName("TASK-634: CRUD completo REST para Combos, ModifierGroups y asignación PUT a platos + Menús PUT/DELETE/Clone")
    void testComboModifierGroupAndMenuRestCrudEndpoints() throws Exception {
        List<DishPriceDto> dishes = catalogPublicApi.getActiveMenuForBranch(tenantId, baseBranchId);
        DishPriceDto dish = dishes.get(0);

        // 1. ModifierGroup POST -> PUT -> Asignar PUT a plato -> GET plato modifiers -> DELETE
        CreateModifierGroupRequest modReq = new CreateModifierGroupRequest(
            "Guarniciones Extra",
            0,
            2,
            false,
            List.of(new CreateModifierRequest("Huevo frito", new BigDecimal("4.00"), null, BigDecimal.ZERO, 1))
        );
        MvcResult modRes = mockMvc.perform(post("/api/v1/catalog/modifier-groups")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modReq)))
            .andExpect(status().isCreated())
            .andReturn();
        ModifierGroupDto modGroup = objectMapper.readValue(modRes.getResponse().getContentAsString(), ModifierGroupDto.class);

        CreateModifierGroupRequest modUpdate = new CreateModifierGroupRequest(
            "Guarniciones Premium",
            1,
            2,
            true,
            List.of(new CreateModifierRequest("Tocino crispy", new BigDecimal("7.00"), null, BigDecimal.ZERO, 1))
        );
        mockMvc.perform(put("/api/v1/catalog/modifier-groups/{id}", modGroup.id())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Guarniciones Premium"))
            .andExpect(jsonPath("$.modifiers[0].name").value("Tocino crispy"));

        mockMvc.perform(put("/api/v1/catalog/dishes/{dishId}/modifier-groups", dish.dishId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(modGroup.id()))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/catalog/dishes/{dishId}/modifier-groups", dish.dishId())
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(modGroup.id().toString()));

        mockMvc.perform(delete("/api/v1/catalog/modifier-groups/{id}", modGroup.id())
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isNoContent());

        // 2. Combo POST -> PUT -> DELETE
        String uniqueComboCode = "COMBO-CRUD-" + UUID.randomUUID().toString().substring(0, 6);
        CreateComboRequest comboReq = new CreateComboRequest(
            isolatedBranchId,
            dish.categoryId(),
            uniqueComboCode,
            "Combo Crud",
            "Inicial",
            new BigDecimal("42.00"),
            List.of(new CreateComboItemRequest("Plato fijo", "FIXED", true, dish.dishId(), 1, 1, List.of(), "FIXED", null))
        );
        MvcResult comboRes = mockMvc.perform(post("/api/v1/catalog/combos")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comboReq)))
            .andExpect(status().isCreated())
            .andReturn();
        ComboDto combo = objectMapper.readValue(comboRes.getResponse().getContentAsString(), ComboDto.class);

        CreateComboRequest comboUpdate = new CreateComboRequest(
            isolatedBranchId,
            dish.categoryId(),
            uniqueComboCode,
            "Combo Crud Editado",
            "Actualizado",
            new BigDecimal("45.00"),
            List.of(new CreateComboItemRequest("Opciones", "CHOICE", true, dish.dishId(), 1, 1,
                List.of(new CreateComboItemOptionRequest(dish.dishId(), BigDecimal.ZERO, true)), "SPECIFIC_OPTIONS", null))
        );
        mockMvc.perform(put("/api/v1/catalog/combos/{id}", combo.id())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(comboUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Combo Crud Editado"))
            .andExpect(jsonPath("$.items[0].selectionSource").value("SPECIFIC_OPTIONS"));

        mockMvc.perform(delete("/api/v1/catalog/combos/{id}", combo.id())
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isNoContent());

        // 3. DailyMenu PUT -> Clone -> DELETE (con múltiples días concatenados > 15 chars)
        CreateDailyMenuRequest menuReq = new CreateDailyMenuRequest(
            isolatedBranchId,
            "Menú CRUD",
            "Test",
            "LUNES,MARTES,MIERCOLES,JUEVES,VIERNES",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(new CreateDailyMenuItemRequest(dish.dishId(), CourseType.MAIN, 10, null, false, 1, true, false)),
            List.of()
        );
        MvcResult menuRes = mockMvc.perform(post("/api/v1/catalog/menus")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menuReq)))
            .andExpect(status().isCreated())
            .andReturn();
        DailyMenuDto menuDto = objectMapper.readValue(menuRes.getResponse().getContentAsString(), DailyMenuDto.class);

        CreateDailyMenuRequest menuUpdate = new CreateDailyMenuRequest(
            isolatedBranchId,
            "Menú CRUD Editado",
            "Test edit",
            "FRIDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(new CreateDailyMenuItemRequest(dish.dishId(), CourseType.MAIN, 15, null, false, 1, false, true)),
            List.of()
        );
        mockMvc.perform(put("/api/v1/catalog/menus/{id}", menuDto.id())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menuUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Menú CRUD Editado"))
            .andExpect(jsonPath("$.items[0].isUnlimited").value(true));

        MvcResult cloneRes = mockMvc.perform(post("/api/v1/catalog/menus/{id}/clone", menuDto.id())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newName\": \"Menú Clonado\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Menú Clonado"))
            .andReturn();
        DailyMenuDto cloned = objectMapper.readValue(cloneRes.getResponse().getContentAsString(), DailyMenuDto.class);

        mockMvc.perform(delete("/api/v1/catalog/menus/{id}", cloned.id())
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isNoContent());

        Optional<DailyMenuDto> remaining = Optional.ofNullable(dailyMenuPublicApi.getMenuById(menuDto.id()));
        assertThat(remaining).isPresent();
    }
}
