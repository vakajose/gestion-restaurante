package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CourseType;
import com.restaurant.app.modules.catalog.api.CreateCategoryRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuItemRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.CreateDishRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.PriceMode;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuotaAdjustmentTest {

    @Autowired
    private DailyMenuPublicApi dailyMenuPublicApi;

    @Autowired
    private FinancePublicApi financePublicApi;

    @Autowired
    private CatalogServiceImpl catalogService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    private UUID tenantId;
    private UUID branchId;
    private UUID supervisorId;
    private UUID cashShiftId;
    private UUID shiftItemId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Adjust Test", "adjust-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Adjust", "America/La_Paz"));
        branchId = branch.getId();

        supervisorId = UUID.randomUUID();
        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            supervisorId,
            "supervisor_juan",
            "BRANCH_MANAGER"
        ));

        var cat = catalogService.createCategory(new CreateCategoryRequest("General", "utensils", 1));

        DishDto dish = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "ADJ-01", "Pastel de Choclo", "Raciones limitadas", new BigDecimal("25.00"), true, List.of()
        ));

        var template = dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            branchId,
            "Menú Jueves",
            "Menú con platos horneados",
            "THURSDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(
                new CreateDailyMenuItemRequest(dish.id(), CourseType.MAIN, 0, null, false, 1)
            )
        ));

        // Abrir turno con cupo inicial 0 (Agotado desde el inicio hasta que horneen)
        CashShiftDto shift = financePublicApi.openShift(new OpenShiftRequest(
            branchId,
            new BigDecimal("150.00"),
            template.id(),
            Map.of(dish.id(), 0)
        ));
        cashShiftId = shift.id();

        ShiftMenuDto shiftMenu = dailyMenuPublicApi.getShiftMenu(cashShiftId).orElseThrow();
        ShiftMenuItemDto item = shiftMenu.items().get(0);
        shiftItemId = item.id();
        assertThat(item.currentQuota()).isEqualTo(0);
        assertThat(item.isSoldOut()).isTrue();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TC-MENU-004: Ampliación Supervisada de Cupos en Vivo con Trazabilidad (PB-034)")
    void testSupervisorLiveQuotaExpansion() {
        // El supervisor amplía +10 raciones tras salir una nueva bandeja del horno
        ShiftMenuItemDto adjusted = dailyMenuPublicApi.adjustQuota(
            cashShiftId,
            shiftItemId,
            10,
            "Tanda horneada extra en cocina",
            supervisorId
        );

        assertThat(adjusted).isNotNull();
        assertThat(adjusted.currentQuota()).isEqualTo(10);
        assertThat(adjusted.isSoldOut()).isFalse();

        // Verificar persistencia y consulta directa
        ShiftMenuDto shiftMenu = dailyMenuPublicApi.getShiftMenu(cashShiftId).orElseThrow();
        ShiftMenuItemDto itemUpdated = shiftMenu.items().stream()
            .filter(i -> i.id().equals(shiftItemId))
            .findFirst()
            .orElseThrow();

        assertThat(itemUpdated.currentQuota()).isEqualTo(10);
        assertThat(itemUpdated.isSoldOut()).isFalse();
    }
}
