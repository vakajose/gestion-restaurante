package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.BranchOverrideRequest;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.CategoryDto;
import com.restaurant.app.modules.catalog.api.CloneDishRequest;
import com.restaurant.app.modules.catalog.api.CreateCategoryRequest;
import com.restaurant.app.modules.catalog.api.CreateDishRequest;
import com.restaurant.app.modules.catalog.api.CreateIngredientRequest;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.DishRecipeItemRequest;
import com.restaurant.app.modules.catalog.api.IngredientDto;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogPublicApiTest {

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private CatalogServiceImpl catalogService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private DishRecipeRepository dishRecipeRepository;

    @Autowired
    private BranchDishRepository branchDishRepository;

    private Tenant testTenant;
    private Branch testBranch1;
    private Branch testBranch2;

    @BeforeEach
    void setUp() {
        testTenant = tenantRepository.save(new Tenant("Tenant Test " + UUID.randomUUID(), "NIT-" + UUID.randomUUID()));
        testBranch1 = branchRepository.save(new Branch(testTenant.getId(), "Sucursal Norte", "America/La_Paz"));
        testBranch2 = branchRepository.save(new Branch(testTenant.getId(), "Sucursal Sur", "America/La_Paz"));

        TenantContextHolder.set(new TenantContext(
            testTenant.getId(),
            testBranch1.getId(),
            UUID.randomUUID(),
            "admin_test",
            "ADMIN_TENANT"
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe crear categorías y listar ordenadas por sortOrder y nombre")
    void shouldCreateAndListCategories() {
        CategoryDto cat2 = catalogService.createCategory(new CreateCategoryRequest("Postres", "cake", 2));
        CategoryDto cat1 = catalogService.createCategory(new CreateCategoryRequest("Entradas", "soup", 1));

        List<CategoryDto> categories = catalogService.getCategories();
        assertThat(categories).extracting(CategoryDto::name).containsSubsequence("Entradas", "Postres");

        assertThatThrownBy(() -> catalogService.createCategory(new CreateCategoryRequest("Postres", "cake", 3)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe una categoría");
    }

    @Test
    @DisplayName("Debe crear ingredientes y validar unicidad")
    void shouldCreateAndListIngredients() {
        IngredientDto ing = catalogService.createIngredient(
            new CreateIngredientRequest("Tomate", "KG", new BigDecimal("1.5000"))
        );

        assertThat(ing.id()).isNotNull();
        assertThat(ing.name()).isEqualTo("Tomate");
        assertThat(ing.unitOfMeasure()).isEqualTo("KG");

        assertThatThrownBy(() -> catalogService.createIngredient(
            new CreateIngredientRequest("Tomate", "KG", BigDecimal.ZERO)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Debe crear plato maestro con su receta (BOM) y recuperarla")
    void shouldCreateDishWithRecipe() {
        CategoryDto cat = catalogService.createCategory(new CreateCategoryRequest("Pizzas", "pizza", 1));
        IngredientDto ingHarina = catalogService.createIngredient(new CreateIngredientRequest("Harina", "KG", BigDecimal.ONE));
        IngredientDto ingQueso = catalogService.createIngredient(new CreateIngredientRequest("Queso Mozzarella", "KG", BigDecimal.ONE));

        DishDto dish = catalogService.createDish(new CreateDishRequest(
            cat.id(),
            null,
            "PIZZA-MARG-01",
            "Pizza Margherita",
            "Pizza clásica con mozzarella y albahaca",
            new BigDecimal("45.00"),
            true,
            List.of(
                new DishRecipeItemRequest(ingHarina.id(), new BigDecimal("0.2500")),
                new DishRecipeItemRequest(ingQueso.id(), new BigDecimal("0.1800"))
            )
        ));

        assertThat(dish.id()).isNotNull();
        assertThat(dish.code()).isEqualTo("PIZZA-MARG-01");
        assertThat(dish.categoryName()).isEqualTo("Pizzas");

        // Validar CatalogPublicApi.getRecipeByDishId
        Optional<DishRecipeDto> recipeOpt = catalogPublicApi.getRecipeByDishId(dish.id());
        assertThat(recipeOpt).isPresent();
        assertThat(recipeOpt.get().items()).hasSize(2);
        assertThat(recipeOpt.get().items()).extracting("ingredientName").containsExactlyInAnyOrder("Harina", "Queso Mozzarella");
    }

    @Test
    @DisplayName("Debe validar actualización de receta (BOM) rechazando ingredientes duplicados o inexistentes")
    void shouldUpdateRecipeAndValidateIntegrity() {
        CategoryDto cat = catalogService.createCategory(new CreateCategoryRequest("Pastas", "pasta", 1));
        IngredientDto ingPasta = catalogService.createIngredient(new CreateIngredientRequest("Pasta", "KG", BigDecimal.ONE));

        DishDto dish = catalogService.createDish(new CreateDishRequest(
            cat.id(), null, "PASTA-01", "Pasta Carbonara", "Con panceta y huevo", new BigDecimal("38.00"), true, null
        ));

        // Duplicados
        assertThatThrownBy(() -> catalogService.setDishRecipe(dish.id(), List.of(
            new DishRecipeItemRequest(ingPasta.id(), new BigDecimal("0.2000")),
            new DishRecipeItemRequest(ingPasta.id(), new BigDecimal("0.1000"))
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicados");

        // Inexistente
        assertThatThrownBy(() -> catalogService.setDishRecipe(dish.id(), List.of(
            new DishRecipeItemRequest(UUID.randomUUID(), new BigDecimal("0.2000"))
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no existen");

        // Actualización válida
        DishRecipeDto updatedRecipe = catalogService.setDishRecipe(dish.id(), List.of(
            new DishRecipeItemRequest(ingPasta.id(), new BigDecimal("0.2200"))
        ));
        assertThat(updatedRecipe.items()).hasSize(1);
        assertThat(updatedRecipe.items().getFirst().quantity()).isEqualByComparingTo("0.2200");
    }

    @Test
    @DisplayName("Debe manejar sobreescrituras por sucursal de precio y disponibilidad")
    void shouldHandleBranchOverrides() {
        CategoryDto cat = catalogService.createCategory(new CreateCategoryRequest("Bebidas", "cup", 1));
        DishDto dish = catalogService.createDish(new CreateDishRequest(
            cat.id(), null, "JUICE-01", "Jugo de Naranja", "Natural", new BigDecimal("12.00"), true, null
        ));

        // 1. Sin override en Branch 1 ni Branch 2: precio base $12.00, disponible = true
        assertThat(catalogPublicApi.getEffectiveDishPrice(dish.id(), testBranch1.getId()))
            .isEqualByComparingTo("12.00");
        assertThat(catalogPublicApi.isDishAvailableInBranch(dish.id(), testBranch1.getId()))
            .isTrue();

        // 2. Establecer override en Branch 1: precio $14.50 y disponible = true
        catalogService.setBranchOverride(dish.id(), new BranchOverrideRequest(
            testBranch1.getId(), true, new BigDecimal("14.50")
        ));

        assertThat(catalogPublicApi.getEffectiveDishPrice(dish.id(), testBranch1.getId()))
            .isEqualByComparingTo("14.50");
        // Branch 2 sigue con precio base
        assertThat(catalogPublicApi.getEffectiveDishPrice(dish.id(), testBranch2.getId()))
            .isEqualByComparingTo("12.00");

        // 3. Desactivar disponibilidad en Branch 2
        catalogService.setBranchOverride(dish.id(), new BranchOverrideRequest(
            testBranch2.getId(), false, null
        ));

        assertThat(catalogPublicApi.isDishAvailableInBranch(dish.id(), testBranch1.getId())).isTrue();
        assertThat(catalogPublicApi.isDishAvailableInBranch(dish.id(), testBranch2.getId())).isFalse();

        // 4. Menú activo para Branch 1
        List<DishPriceDto> menuBranch1 = catalogPublicApi.getActiveMenuForBranch(testTenant.getId(), testBranch1.getId());
        assertThat(menuBranch1).hasSize(1);
        DishPriceDto itemBranch1 = menuBranch1.getFirst();
        assertThat(itemBranch1.basePrice()).isEqualByComparingTo("12.00");
        assertThat(itemBranch1.effectivePrice()).isEqualByComparingTo("14.50");
        assertThat(itemBranch1.hasOverride()).isTrue();
        assertThat(itemBranch1.isAvailable()).isTrue();

        // Menú activo para Branch 2
        List<DishPriceDto> menuBranch2 = catalogPublicApi.getActiveMenuForBranch(testTenant.getId(), testBranch2.getId());
        assertThat(menuBranch2).hasSize(1);
        DishPriceDto itemBranch2 = menuBranch2.getFirst();
        assertThat(itemBranch2.basePrice()).isEqualByComparingTo("12.00");
        assertThat(itemBranch2.effectivePrice()).isEqualByComparingTo("12.00");
        assertThat(itemBranch2.hasOverride()).isFalse();
        assertThat(itemBranch2.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Debe clonar plato con trazabilidad (cloned_from_id) y duplicar su receta")
    void shouldCloneDishWithTraceabilityAndRecipe() {
        CategoryDto cat = catalogService.createCategory(new CreateCategoryRequest("Carnes", "meat", 1));
        IngredientDto ingCarne = catalogService.createIngredient(new CreateIngredientRequest("Bife de Chorizo", "KG", BigDecimal.ONE));

        DishDto original = catalogService.createDish(new CreateDishRequest(
            cat.id(),
            null,
            "MEAT-01",
            "Bife de Chorizo 400g",
            "Corte premium a la parrilla",
            new BigDecimal("60.00"),
            true,
            List.of(new DishRecipeItemRequest(ingCarne.id(), new BigDecimal("0.4000")))
        ));

        // Clonar plato para Sucursal Sur con nuevo código y precio ajustado
        DishDto cloned = catalogService.cloneDish(original.id(), new CloneDishRequest(
            testBranch2.getId(),
            "MEAT-01-SUR",
            "Bife de Chorizo Especial Sur",
            new BigDecimal("65.00"),
            null // hereda la receta original
        ));

        assertThat(cloned.id()).isNotNull().isNotEqualTo(original.id());
        assertThat(cloned.clonedFromId()).isEqualTo(original.id());
        assertThat(cloned.branchId()).isEqualTo(testBranch2.getId());
        assertThat(cloned.code()).isEqualTo("MEAT-01-SUR");
        assertThat(cloned.salePrice()).isEqualByComparingTo("65.00");

        // Verificar que la receta fue clonada automáticamente
        Optional<DishRecipeDto> clonedRecipe = catalogPublicApi.getRecipeByDishId(cloned.id());
        assertThat(clonedRecipe).isPresent();
        assertThat(clonedRecipe.get().items()).hasSize(1);
        assertThat(clonedRecipe.get().items().getFirst().ingredientId()).isEqualTo(ingCarne.id());
        assertThat(clonedRecipe.get().items().getFirst().quantity()).isEqualByComparingTo("0.4000");
    }

    @Test
    @DisplayName("Debe clonar plato con receta personalizada diferenciada")
    void shouldCloneDishWithCustomRecipe() {
        CategoryDto cat = catalogService.createCategory(new CreateCategoryRequest("Sándwiches", "sandwich", 1));
        IngredientDto ingPan = catalogService.createIngredient(new CreateIngredientRequest("Pan Baguette", "UNIT", BigDecimal.ONE));
        IngredientDto ingJamon = catalogService.createIngredient(new CreateIngredientRequest("Jamón Serrano", "KG", BigDecimal.ONE));
        IngredientDto ingQueso = catalogService.createIngredient(new CreateIngredientRequest("Queso Brie", "KG", BigDecimal.ONE));

        DishDto original = catalogService.createDish(new CreateDishRequest(
            cat.id(),
            null,
            "SAND-01",
            "Sándwich Serrano",
            "Baguette con jamón",
            new BigDecimal("25.00"),
            true,
            List.of(
                new DishRecipeItemRequest(ingPan.id(), BigDecimal.ONE),
                new DishRecipeItemRequest(ingJamon.id(), new BigDecimal("0.1000"))
            )
        ));

        // Clonar con queso brie agregado en la receta
        DishDto cloned = catalogService.cloneDish(original.id(), new CloneDishRequest(
            null,
            "SAND-02-BRIE",
            "Sándwich Serrano con Brie",
            new BigDecimal("30.00"),
            List.of(
                new DishRecipeItemRequest(ingPan.id(), BigDecimal.ONE),
                new DishRecipeItemRequest(ingJamon.id(), new BigDecimal("0.1000")),
                new DishRecipeItemRequest(ingQueso.id(), new BigDecimal("0.0500"))
            )
        ));

        assertThat(cloned.clonedFromId()).isEqualTo(original.id());
        Optional<DishRecipeDto> clonedRecipe = catalogPublicApi.getRecipeByDishId(cloned.id());
        assertThat(clonedRecipe).isPresent();
        assertThat(clonedRecipe.get().items()).hasSize(3);
    }
}
