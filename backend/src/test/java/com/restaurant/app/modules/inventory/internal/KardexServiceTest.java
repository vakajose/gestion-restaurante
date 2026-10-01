package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.config.api.TenantConfigService;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.inventory.api.StockAdjustmentRequest;
import com.restaurant.app.modules.inventory.api.StockBalanceDto;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KardexServiceTest {

    @Autowired
    private KardexService kardexService;

    @Autowired
    private BranchStockRepository branchStockRepository;

    @Autowired
    private KardexMovementRepository kardexMovementRepository;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    private UUID tenantId;
    private UUID branchId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        Branch branch = branchRepository.findByTenantId(tenant.getId()).get(0);
        this.tenantId = tenant.getId();
        this.branchId = branch.getId();
    }

    @Test
    @DisplayName("Debe deducir insumos de receta en Kardex ante OrderPaidEvent (AUTOMATIC_PERMISSIVE)")
    void testProcessOrderDeduction_permissive() {
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        DishPriceDto burger = menu.stream()
            .filter(d -> d.name().toLowerCase().contains("hamburguesa"))
            .findFirst()
            .orElseThrow();

        List<StockBalanceDto> stockBefore = kardexService.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto carneBefore = stockBefore.stream()
            .filter(s -> s.ingredientName().toLowerCase().contains("carne"))
            .findFirst()
            .orElseThrow();

        // 2 Hamburguesas -> requiere 2 * 0.150 = 0.300 KG de carne
        OrderPaidEvent event = new OrderPaidEvent(
            UUID.randomUUID(),
            tenantId,
            branchId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T-TEST-01",
            List.of(new OrderItemSummary(burger.dishId(), burger.name(), 2, burger.effectivePrice(), burger.effectivePrice().multiply(BigDecimal.valueOf(2)))),
            burger.effectivePrice().multiply(BigDecimal.valueOf(2)),
            "CASH",
            Instant.now()
        );

        kardexService.processOrderDeduction(event);

        List<StockBalanceDto> stockAfter = kardexService.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto carneAfter = stockAfter.stream()
            .filter(s -> s.ingredientName().toLowerCase().contains("carne"))
            .findFirst()
            .orElseThrow();

        assertThat(carneAfter.currentQuantity())
            .isEqualByComparingTo(carneBefore.currentQuantity().subtract(new BigDecimal("0.3000")));

        List<KardexMovementDto> movements = kardexService.getKardexMovements(tenantId, branchId, carneBefore.ingredientId());
        assertThat(movements).isNotEmpty();
        KardexMovementDto latest = movements.get(0);
        assertThat(latest.movementType()).isEqualTo("SALE_OUT");
        assertThat(latest.quantity()).isEqualByComparingTo(new BigDecimal("0.3000"));
    }

    @Test
    @DisplayName("Debe omitir deducción cuando INVENTORY_DEDUCTION_MODE es MANUAL_ISOLATED")
    void testProcessOrderDeduction_manualIsolated() {
        tenantConfigService.setSetting(tenantId, "INVENTORY_DEDUCTION_MODE", "MANUAL_ISOLATED", "STRING", "Modo manual");

        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        DishPriceDto burger = menu.stream()
            .filter(d -> d.name().toLowerCase().contains("hamburguesa"))
            .findFirst()
            .orElseThrow();

        List<StockBalanceDto> stockBefore = kardexService.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto panBefore = stockBefore.stream()
            .filter(s -> s.ingredientName().toLowerCase().contains("pan"))
            .findFirst()
            .orElseThrow();

        OrderPaidEvent event = new OrderPaidEvent(
            UUID.randomUUID(),
            tenantId,
            branchId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T-TEST-MANUAL",
            List.of(new OrderItemSummary(burger.dishId(), burger.name(), 3, burger.effectivePrice(), burger.effectivePrice().multiply(BigDecimal.valueOf(3)))),
            burger.effectivePrice().multiply(BigDecimal.valueOf(3)),
            "CASH",
            Instant.now()
        );

        kardexService.processOrderDeduction(event);

        List<StockBalanceDto> stockAfter = kardexService.getBranchStockSummary(tenantId, branchId);
        StockBalanceDto panAfter = stockAfter.stream()
            .filter(s -> s.ingredientName().toLowerCase().contains("pan"))
            .findFirst()
            .orElseThrow();

        assertThat(panAfter.currentQuantity()).isEqualByComparingTo(panBefore.currentQuantity());
    }

    @Test
    @DisplayName("Debe ajustar stock físicamente y registrar movimiento PHYSICAL_COUNT_ADJUSTMENT")
    void testAdjustStock() {
        List<IngredientDto> ingredients = catalogPublicApi.getAllIngredients(tenantId);
        IngredientDto queso = ingredients.stream()
            .filter(i -> i.name().toLowerCase().contains("queso"))
            .findFirst()
            .orElseThrow();

        StockAdjustmentRequest request = new StockAdjustmentRequest(
            queso.id(),
            new BigDecimal("12.5000"),
            new BigDecimal("42.00"),
            "Conteo físico mensual"
        );

        StockBalanceDto adjusted = kardexService.adjustStock(tenantId, branchId, request, UUID.randomUUID());
        assertThat(adjusted.currentQuantity()).isEqualByComparingTo(new BigDecimal("12.5000"));
        assertThat(adjusted.averageUnitCost()).isEqualByComparingTo(new BigDecimal("42.00"));

        List<KardexMovementDto> movements = kardexService.getKardexMovements(tenantId, branchId, queso.id());
        assertThat(movements).isNotEmpty();
        assertThat(movements.get(0).movementType()).isEqualTo("PHYSICAL_COUNT_ADJUSTMENT");
        assertThat(movements.get(0).balanceQuantity()).isEqualByComparingTo(new BigDecimal("12.5000"));
    }
}
