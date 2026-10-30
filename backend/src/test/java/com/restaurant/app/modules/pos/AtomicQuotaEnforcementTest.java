package com.restaurant.app.modules.pos;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.ItemSoldOutException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.CourseType;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuItemRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.PriceMode;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AtomicQuotaEnforcementTest {

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private FinancePublicApi financePublicApi;

    @Autowired
    private DailyMenuPublicApi dailyMenuPublicApi;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    private UUID tenantId;
    private UUID branchId;
    private UUID cashShiftId;
    private UUID limitedDishId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Quota Test", "quota-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Quota", "America/La_Paz"));
        branchId = branch.getId();

        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            UUID.randomUUID(),
            "cajero_pos",
            "CASHIER"
        ));

        // Sembrar catálogo demo
        catalogPublicApi.seedDemoCatalog(tenantId, branchId, "FAST_FOOD");
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        DishPriceDto limitedDish = menu.get(0);
        limitedDishId = limitedDish.dishId();

        DailyMenuDto template = dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            branchId,
            "Almuerzo Limitado",
            "Prueba de cupos finitos",
            "WEDNESDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(
                new CreateDailyMenuItemRequest(limitedDishId, CourseType.MAIN, 1, null, false, 1)
            )
        ));

        // Abrir turno con cupo de exactamente 1 ración
        CashShiftDto shift = financePublicApi.openShift(new OpenShiftRequest(
            branchId,
            new BigDecimal("100.00"),
            template.id(),
            Map.of(limitedDishId, 1)
        ));
        cashShiftId = shift.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TC-MENU-003: Decremento Atómico Concurrente y Bloqueo de Agotado en POS (PB-034)")
    void testAtomicQuotaEnforcementAndSoldOutBlocking() {
        // 1. Primera venta: solicita 1 ración -> Debe ser exitosa
        CreateOrderRequest firstOrderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Primera comanda",
            List.of(new CreateOrderItemRequest(limitedDishId, 1, new BigDecimal("35.00"), null)),
            null
        );

        OrderDto firstOrder = posPublicApi.placeOrder(firstOrderReq);
        assertThat(firstOrder).isNotNull();
        assertThat(firstOrder.id()).isNotNull();

        // Verificar que el cupo decrementó a 0 y se marcó is_sold_out = true
        ShiftMenuItemDto itemAfterFirstSale = dailyMenuPublicApi.findShiftMenuItem(cashShiftId, limitedDishId).orElseThrow();
        assertThat(itemAfterFirstSale.currentQuota()).isEqualTo(0);
        assertThat(itemAfterFirstSale.soldCount()).isEqualTo(1);
        assertThat(itemAfterFirstSale.isSoldOut()).isTrue();

        // 2. Segunda venta inmediatamente después: solicita 1 ración -> Debe arrojar ItemSoldOutException (409)
        CreateOrderRequest secondOrderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Segunda comanda que debe fallar",
            List.of(new CreateOrderItemRequest(limitedDishId, 1, new BigDecimal("35.00"), null)),
            null
        );

        assertThatThrownBy(() -> posPublicApi.placeOrder(secondOrderReq))
            .isInstanceOf(ItemSoldOutException.class)
            .hasMessageContaining("solo dispone de 0 raciones");
    }
}
