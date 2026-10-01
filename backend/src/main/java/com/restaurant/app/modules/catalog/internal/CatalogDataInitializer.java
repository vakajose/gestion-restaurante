package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(10)
class CatalogDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataInitializer.class);

    private final TenantRepository tenantRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final DishRepository dishRepository;
    private final DishRecipeRepository dishRecipeRepository;

    CatalogDataInitializer(
        TenantRepository tenantRepository,
        CategoryRepository categoryRepository,
        IngredientRepository ingredientRepository,
        DishRepository dishRepository,
        DishRecipeRepository dishRecipeRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
        this.dishRecipeRepository = dishRecipeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Optional<Tenant> demoTenantOpt = tenantRepository.findByNitOrTaxId("12345678");
        if (demoTenantOpt.isEmpty()) {
            log.info("Demo tenant not found, skipping catalog demo initialization.");
            return;
        }

        Tenant tenant = demoTenantOpt.get();

        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenant.getId(), "Hamburguesas")) {
            log.info("Catalog demo data already initialized for tenant {}", tenant.getId());
            return;
        }

        log.info("Initializing catalog demo data for tenant {}...", tenant.getId());

        // 1. Categorías demo
        Category catBurgers = categoryRepository.save(new Category(tenant.getId(), "Hamburguesas", "burger", 1));
        Category catDrinks = categoryRepository.save(new Category(tenant.getId(), "Bebidas", "cup", 2));
        Category catSides = categoryRepository.save(new Category(tenant.getId(), "Acompañamientos", "fries", 3));

        // 2. Ingredientes demo
        Ingredient ingMeat = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Carne de Res molida", "KG", new BigDecimal("5.0000"))
        );
        Ingredient ingBread = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Pan de Hamburguesa", "UNIT", new BigDecimal("20.0000"))
        );
        Ingredient ingCheese = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Queso Cheddar", "KG", new BigDecimal("2.0000"))
        );
        Ingredient ingPotatoes = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Papas", "KG", new BigDecimal("10.0000"))
        );
        Ingredient ingCoke = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Coca Cola 500ml", "UNIT", new BigDecimal("24.0000"))
        );

        // 3. Platos demo con sus recetas (BOM)
        // a) Hamburguesa Clásica ($35.00)
        Dish dishBurger = dishRepository.save(new Dish(
            tenant.getId(),
            null,
            null,
            catBurgers.getId(),
            "DISH-BURGER-01",
            "Hamburguesa Clásica",
            "Deliciosa hamburguesa artesanal con carne molida y queso cheddar",
            new BigDecimal("35.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(dishBurger.getId(), ingMeat.getId(), new BigDecimal("0.1500")));
        dishRecipeRepository.save(new DishRecipe(dishBurger.getId(), ingBread.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishBurger.getId(), ingCheese.getId(), new BigDecimal("0.0300")));

        // b) Papas Fritas ($15.00)
        Dish dishFries = dishRepository.save(new Dish(
            tenant.getId(),
            null,
            null,
            catSides.getId(),
            "DISH-FRIES-01",
            "Papas Fritas",
            "Porción de papas fritas crocantes y sazonadas",
            new BigDecimal("15.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(dishFries.getId(), ingPotatoes.getId(), new BigDecimal("0.2500")));

        // c) Coca Cola 500ml ($10.00)
        Dish dishCoke = dishRepository.save(new Dish(
            tenant.getId(),
            null,
            null,
            catDrinks.getId(),
            "DISH-COCA-01",
            "Coca Cola 500ml",
            "Gaseosa Coca Cola personal de 500ml",
            new BigDecimal("10.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(dishCoke.getId(), ingCoke.getId(), new BigDecimal("1.0000")));

        log.info("Catalog demo data initialized successfully with categories, ingredients, and dishes with recipes.");
    }
}
