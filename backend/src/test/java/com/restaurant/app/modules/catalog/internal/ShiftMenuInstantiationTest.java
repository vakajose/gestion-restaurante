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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftMenuInstantiationTest {

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
    private UUID dishStarterId;
    private UUID dishMainId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Shift Menu Test", "shift-menu-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Turno", "America/La_Paz"));
        branchId = branch.getId();

        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            UUID.randomUUID(),
            "admin_shift",
            "ADMIN_TENANT"
        ));

        var cat = catalogService.createCategory(new CreateCategoryRequest("General", "utensils", 1));

        DishDto starter = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "S-01", "Sopa de Quinua", "Sopa", new BigDecimal("18.00"), true, List.of()
        ));
        dishStarterId = starter.id();

        DishDto main = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "M-01", "Milanesa de Res", "Milanesa", new BigDecimal("40.00"), true, List.of()
        ));
        dishMainId = main.id();

        DailyMenuDto template = dailyMenuPublicApi.createMenu(tenantId, new CreateDailyMenuRequest(
            branchId,
            "Almuerzo Ejecutivo Martes",
            "Plantilla estándar",
            "TUESDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(
                new CreateDailyMenuItemRequest(dishStarterId, CourseType.STARTER, 30, null, false, 1),
                new CreateDailyMenuItemRequest(dishMainId, CourseType.MAIN, 15, null, false, 2)
            )
        ));
        templateId = template.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TC-MENU-002: Instanciación de Menú al Turno con Cupos Iniciales Personalizados (PB-034)")
    void testInstantiateMenuForShiftWithCustomQuotas() {
        // Abrir turno indicando templateId y cupo personalizado para Milanesa = 25 (en vez de 15)
        OpenShiftRequest openRequest = new OpenShiftRequest(
            branchId,
            new BigDecimal("200.00"),
            templateId,
            Map.of(dishMainId, 25)
        );

        CashShiftDto shift = financePublicApi.openShift(openRequest);
        assertThat(shift).isNotNull();
        assertThat(shift.id()).isNotNull();

        // Consultar el menú instanciado en el turno
        Optional<ShiftMenuDto> shiftMenuOpt = dailyMenuPublicApi.getShiftMenu(shift.id());
        assertThat(shiftMenuOpt).isPresent();
        ShiftMenuDto shiftMenu = shiftMenuOpt.get();
        assertThat(shiftMenu.items()).hasSize(2);

        ShiftMenuItemDto milanesaItem = shiftMenu.items().stream()
            .filter(i -> i.dishId().equals(dishMainId))
            .findFirst()
            .orElseThrow();

        assertThat(milanesaItem.initialQuota()).isEqualTo(25);
        assertThat(milanesaItem.currentQuota()).isEqualTo(25);
        assertThat(milanesaItem.soldCount()).isEqualTo(0);
        assertThat(milanesaItem.isSoldOut()).isFalse();

        ShiftMenuItemDto sopaItem = shiftMenu.items().stream()
            .filter(i -> i.dishId().equals(dishStarterId))
            .findFirst()
            .orElseThrow();

        // Sopa mantuvo el cupo sugerido de la plantilla (30)
        assertThat(sopaItem.initialQuota()).isEqualTo(30);
        assertThat(sopaItem.currentQuota()).isEqualTo(30);

        // Verificar que la plantilla base NO sufrió mutaciones
        DailyMenuDto templateOriginal = dailyMenuPublicApi.getMenuById(templateId);
        var originalMilanesa = templateOriginal.items().stream()
            .filter(i -> i.dishId().equals(dishMainId))
            .findFirst()
            .orElseThrow();
        assertThat(originalMilanesa.suggestedQuota()).isEqualTo(15);
    }
}
