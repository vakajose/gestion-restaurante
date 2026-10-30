package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.CreateCategoryRequest;
import com.restaurant.app.modules.catalog.api.CreateDishRequest;
import com.restaurant.app.modules.catalog.api.CreateIngredientRequest;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.DishRecipeItemRequest;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryStrategyFactoryTest {

    @Autowired
    private InventoryPublicApi inventoryPublicApi;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private BranchStockRepository branchStockRepository;

    @Autowired
    private KardexMovementRepository kardexMovementRepository;

    @Autowired
    private KardexService kardexService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID branchId;
    private UUID dishBomId;
    private UUID dishUnitId;
    private UUID dishCommercialId;
    private UUID ingredientCarneId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Strategy Test", "strategy-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Inventario", "America/La_Paz"));
        branchId = branch.getId();

        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            UUID.randomUUID(),
            "admin_strat",
            "ADMIN_TENANT"
        ));

        // Insumo Carne
        ingredientCarneId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO ingredients (id, tenant_id, name, unit_of_measure, min_stock_alert, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            ingredientCarneId, tenantId, "Carne Molida", "KG", new BigDecimal("5.0000"), Instant.now()
        );

        // Stock inicial del insumo carne (10.0000 KG)
        inventoryPublicApi.registerInitialStock(
            tenantId,
            branchId,
            new InitialStockRequest(ingredientCarneId, new BigDecimal("10.0000"), new BigDecimal("40.00"))
        );

        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO categories (id, tenant_id, name, icon, sort_order, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            categoryId, tenantId, "General", "utensils", 1, Instant.now()
        );

        // 1. Plato RECIPE_BOM_STRICT: Hamburguesa con receta de 0.2000 KG de carne
        dishBomId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO dishes (id, tenant_id, branch_id, category_id, code, name, description, sale_price, is_active, created_at, version, inventory_strategy, allow_negative_stock_override) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            dishBomId, tenantId, branchId, categoryId, "BOM-01", "Hamburguesa BOM", "Con receta", new BigDecimal("35.00"), true, Instant.now(), 0L, "RECIPE_BOM_STRICT", false
        );
        jdbcTemplate.update(
            "INSERT INTO dish_recipes (id, dish_id, ingredient_id, quantity) VALUES (?, ?, ?, ?)",
            UUID.randomUUID(), dishBomId, ingredientCarneId, new BigDecimal("0.2000")
        );

        // 2. Plato DISH_AS_INVENTORY_UNIT: Empanada (unidad física directa en almacén)
        dishUnitId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO dishes (id, tenant_id, branch_id, category_id, code, name, description, sale_price, is_active, created_at, version, inventory_strategy, allow_negative_stock_override) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            dishUnitId, tenantId, branchId, categoryId, "UNIT-01", "Empanada de Queso", "Unidad física", new BigDecimal("8.00"), true, Instant.now(), 0L, "DISH_AS_INVENTORY_UNIT", false
        );

        // Stock del plato unitario en branch_stock (20 unidades)
        branchStockRepository.save(new BranchStock(
            tenantId,
            branchId,
            null,
            dishUnitId,
            new BigDecimal("20.0000"),
            new BigDecimal("4.50")
        ));

        // 3. Plato COMMERCIAL_NO_INVENTORY: Bebida comercial sin control de stock
        dishCommercialId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO dishes (id, tenant_id, branch_id, category_id, code, name, description, sale_price, is_active, created_at, version, inventory_strategy, allow_negative_stock_override) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            dishCommercialId, tenantId, branchId, categoryId, "COM-01", "Bebida Comercial Libre", "Sin stock", new BigDecimal("12.00"), true, Instant.now(), 0L, "COMMERCIAL_NO_INVENTORY", true
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TC-INV-005: Deducción Polimórfica según Estrategia de Inventario (PB-044)")
    void testPolymorphicInventoryDeduction() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderPaidEvent event = new OrderPaidEvent(
            eventId,
            tenantId,
            branchId,
            orderId,
            UUID.randomUUID(),
            "T-STRATEGY-01",
            List.of(
                new OrderItemSummary(dishBomId, "Hamburguesa BOM", 1, new BigDecimal("35.00"), new BigDecimal("35.00")),
                new OrderItemSummary(dishUnitId, "Empanada de Queso", 1, new BigDecimal("8.00"), new BigDecimal("8.00")),
                new OrderItemSummary(dishCommercialId, "Bebida Comercial Libre", 1, new BigDecimal("12.00"), new BigDecimal("12.00"))
            ),
            new BigDecimal("55.00"),
            "CASH",
            Instant.now()
        );

        // Procesar deducción polimórfica
        kardexService.processOrderDeduction(event);

        // 1. Verificación RECIPE_BOM_STRICT:
        // El insumo carne debe haber bajado de 10.0000 a 9.8000
        var carneStock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientCarneId).orElseThrow();
        assertThat(carneStock.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("9.8000"));

        var carneMovements = kardexMovementRepository.findByTenantIdAndBranchIdAndIngredientIdOrderByMovementDateDesc(tenantId, branchId, ingredientCarneId);
        assertThat(carneMovements).anyMatch(m -> "SALE_OUT".equals(m.getMovementType()) &&
            m.getQuantity().compareTo(new BigDecimal("0.2000")) == 0 &&
            orderId.equals(m.getReferenceId()));

        // 2. Verificación DISH_AS_INVENTORY_UNIT:
        // El plato terminado debe haber bajado de 20.0000 a 19.0000
        var dishStock = branchStockRepository.findByTenantIdAndBranchIdAndDishId(tenantId, branchId, dishUnitId).orElseThrow();
        assertThat(dishStock.getCurrentQuantity()).isEqualByComparingTo(new BigDecimal("19.0000"));

        var dishMovements = kardexMovementRepository.findByTenantIdAndBranchIdAndDishIdOrderByMovementDateDesc(tenantId, branchId, dishUnitId);
        assertThat(dishMovements).hasSize(1);
        assertThat(dishMovements.get(0).getMovementType()).isEqualTo("SALE_OUT");
        assertThat(dishMovements.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(dishMovements.get(0).getReferenceId()).isEqualTo(orderId);

        // 3. Verificación COMMERCIAL_NO_INVENTORY:
        // Ningún movimiento en branch_stock ni en kardex_movements para el plato comercial
        var comStock = branchStockRepository.findByTenantIdAndBranchIdAndDishId(tenantId, branchId, dishCommercialId);
        assertThat(comStock).isEmpty();

        var comMovements = kardexMovementRepository.findByTenantIdAndBranchIdAndDishIdOrderByMovementDateDesc(tenantId, branchId, dishCommercialId);
        assertThat(comMovements).isEmpty();
    }
}
