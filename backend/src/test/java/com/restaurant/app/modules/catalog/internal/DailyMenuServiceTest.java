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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyMenuServiceTest {

    @Autowired
    private DailyMenuPublicApi dailyMenuPublicApi;

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
    private UUID dishDessertId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Menus Test", "menus-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Menus", "America/La_Paz"));
        branchId = branch.getId();

        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            UUID.randomUUID(),
            "admin_menus",
            "ADMIN_TENANT"
        ));

        var cat = catalogService.createCategory(new CreateCategoryRequest("General", "utensils", 1));

        DishDto starter = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "D-STARTER", "Sopa de Verduras", "Sopa ligera", new BigDecimal("15.00"), true, List.of()
        ));
        dishStarterId = starter.id();

        DishDto main = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "D-MAIN", "Milanesa de Pollo", "Con papas", new BigDecimal("35.00"), true, List.of()
        ));
        dishMainId = main.id();

        DishDto dessert = catalogService.createDish(new CreateDishRequest(
            cat.id(), branchId, "D-DESSERT", "Gelatina con Crema", "Postre", new BigDecimal("10.00"), true, List.of()
        ));
        dishDessertId = dessert.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("TC-MENU-001: Creación de Plantilla de Menú Diario Reutilizable (PB-034)")
    void testCreateDailyMenuTemplate() {
        CreateDailyMenuRequest request = new CreateDailyMenuRequest(
            branchId,
            "Lunes Ejecutivo",
            "Menú ejecutivo balanceado para iniciar la semana",
            "MONDAY",
            PriceMode.INDIVIDUAL_ITEMS,
            null,
            true,
            List.of(
                new CreateDailyMenuItemRequest(dishStarterId, CourseType.STARTER, 30, null, false, 1),
                new CreateDailyMenuItemRequest(dishMainId, CourseType.MAIN, 25, null, false, 2),
                new CreateDailyMenuItemRequest(dishDessertId, CourseType.DESSERT, 20, null, false, 3)
            )
        );

        DailyMenuDto created = dailyMenuPublicApi.createMenu(tenantId, request);

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Lunes Ejecutivo");
        assertThat(created.dayOfWeek()).isEqualTo("MONDAY");
        assertThat(created.isTemplate()).isTrue();
        assertThat(created.items()).hasSize(3);

        List<DailyMenuDto> templates = dailyMenuPublicApi.findMenus(tenantId, branchId, true);
        assertThat(templates).anyMatch(m -> "Lunes Ejecutivo".equals(m.name()) && m.items().size() == 3);
    }
}
