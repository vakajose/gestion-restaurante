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
    private final ComboRepository comboRepository;
    private final ModifierGroupRepository modifierGroupRepository;

    CatalogDataInitializer(
        TenantRepository tenantRepository,
        CategoryRepository categoryRepository,
        IngredientRepository ingredientRepository,
        DishRepository dishRepository,
        DishRecipeRepository dishRecipeRepository,
        ComboRepository comboRepository,
        ModifierGroupRepository modifierGroupRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
        this.dishRecipeRepository = dishRecipeRepository;
        this.comboRepository = comboRepository;
        this.modifierGroupRepository = modifierGroupRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedDefaultDemoCatalog();
        seedBurgerStationCatalog();
        seedBellaItaliaCatalog();
    }

    private void seedDefaultDemoCatalog() {
        Optional<Tenant> demoTenantOpt = tenantRepository.findByNitOrTaxId("12345678");
        if (demoTenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = demoTenantOpt.get();
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenant.getId(), "Hamburguesas")) {
            return;
        }

        log.info("Initializing catalog demo data for default tenant {}...", tenant.getId());

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

        // 4. Combo Demo: "Combo Burger Clásico" ($50.00)
        Combo comboClasico = new Combo(
            tenant.getId(),
            null,
            catBurgers.getId(),
            "COMBO-CLASICO-01",
            "Combo Burger Clásico",
            "Hamburguesa Clásica + Papas Fritas + Gaseosa personal",
            new BigDecimal("50.00")
        );

        ComboItem itemBurger = new ComboItem("Hamburguesa", "FIXED", true, dishBurger.getId(), 1, 1);
        itemBurger.addOption(new ComboItemOption(dishBurger.getId(), BigDecimal.ZERO, true));
        comboClasico.addItem(itemBurger);

        ComboItem itemFries = new ComboItem("Guarnición", "FIXED", true, dishFries.getId(), 1, 2);
        itemFries.addOption(new ComboItemOption(dishFries.getId(), BigDecimal.ZERO, true));
        comboClasico.addItem(itemFries);

        ComboItem itemDrink = new ComboItem("Bebida", "CHOICE", true, dishCoke.getId(), 1, 3);
        itemDrink.addOption(new ComboItemOption(dishCoke.getId(), BigDecimal.ZERO, true));
        comboClasico.addItem(itemDrink);

        comboRepository.save(comboClasico);
    }

    private void seedBurgerStationCatalog() {
        Optional<Tenant> tenantOpt = tenantRepository.findByNitOrTaxId("87654321");
        if (tenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = tenantOpt.get();
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenant.getId(), "Hamburguesas Gourmet")) {
            return;
        }

        log.info("Initializing Burger Station catalog data (tenant: {})...", tenant.getId());

        // 1. Categorías
        Category catBurgers = categoryRepository.save(new Category(tenant.getId(), "Hamburguesas Gourmet", "burger", 1));
        Category catSmash = categoryRepository.save(new Category(tenant.getId(), "Smash Burgers", "burger", 2));
        Category catSides = categoryRepository.save(new Category(tenant.getId(), "Sides & Acompañamientos", "fries", 3));
        Category catDrinks = categoryRepository.save(new Category(tenant.getId(), "Bebidas & Cervezas", "cup", 4));
        Category catDesserts = categoryRepository.save(new Category(tenant.getId(), "Postres & Shakes", "cake", 5));

        // 2. Ingredientes
        Ingredient ingBeefAngus = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Medallón de Res Angus 180g", "UNIT", new BigDecimal("15.0000"))
        );
        Ingredient ingBeefSmash = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Medallón Smash Angus 90g", "UNIT", new BigDecimal("8.0000"))
        );
        Ingredient ingBrioche = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Pan Brioche Artesanal", "UNIT", new BigDecimal("3.0000"))
        );
        Ingredient ingBacon = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Tocino Ahumado Crujiente", "KG", new BigDecimal("55.0000"))
        );
        Ingredient ingCheddar = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Queso Cheddar Inglés", "KG", new BigDecimal("48.0000"))
        );
        Ingredient ingGouda = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Queso Gouda Suizo", "KG", new BigDecimal("52.0000"))
        );
        Ingredient ingTruffleSauce = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Salsa Trufada Especial", "KG", new BigDecimal("60.0000"))
        );
        Ingredient ingCaramelOnion = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Cebolla Caramelizada", "KG", new BigDecimal("20.0000"))
        );
        Ingredient ingRusticPotatoes = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Papas Rústicas con Romero", "KG", new BigDecimal("12.0000"))
        );
        Ingredient ingOnionRings = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Aros de Cebolla Rebozados", "KG", new BigDecimal("18.0000"))
        );
        Ingredient ingCokeZero = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Coca Cola Zero 500ml", "UNIT", new BigDecimal("4.5000"))
        );
        Ingredient ingIpaBeer = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Cerveza IPA Artesanal 330ml", "UNIT", new BigDecimal("11.0000"))
        );
        Ingredient ingShake = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Milkshake Frutilla & Vainilla", "UNIT", new BigDecimal("7.0000"))
        );

        // 3. Modificadores
        ModifierGroup mgCooking = new ModifierGroup(tenant.getId(), "Término de la Carne", 1, 1, true);
        mgCooking.addModifier(new Modifier(tenant.getId(), "Término Medio (Jugosa)", BigDecimal.ZERO, null, BigDecimal.ZERO, 1));
        mgCooking.addModifier(new Modifier(tenant.getId(), "Tres Cuartos (Punto)", BigDecimal.ZERO, null, BigDecimal.ZERO, 2));
        mgCooking.addModifier(new Modifier(tenant.getId(), "Bien Cocido", BigDecimal.ZERO, null, BigDecimal.ZERO, 3));
        mgCooking = modifierGroupRepository.save(mgCooking);

        ModifierGroup mgExtras = new ModifierGroup(tenant.getId(), "Extras y Toppings", 0, 4, false);
        mgExtras.addModifier(new Modifier(tenant.getId(), "Extra Tocino Crujiente (+30g)", new BigDecimal("8.00"), ingBacon.getId(), new BigDecimal("0.0300"), 1));
        mgExtras.addModifier(new Modifier(tenant.getId(), "Extra Queso Cheddar (+40g)", new BigDecimal("6.00"), ingCheddar.getId(), new BigDecimal("0.0400"), 2));
        mgExtras.addModifier(new Modifier(tenant.getId(), "Doble Salsa Trufada", new BigDecimal("7.00"), ingTruffleSauce.getId(), new BigDecimal("0.0300"), 3));
        mgExtras.addModifier(new Modifier(tenant.getId(), "Cebolla Caramelizada Extra", new BigDecimal("5.00"), ingCaramelOnion.getId(), new BigDecimal("0.0500"), 4));
        mgExtras = modifierGroupRepository.save(mgExtras);

        // 4. Platos y Recetas
        // a) Burger Doble Bacon Trufada
        Dish dishBurger1 = new Dish(
            tenant.getId(), null, null, catBurgers.getId(), "BS-BURG-01",
            "Burger Doble Bacon Trufada",
            "180g carne Angus, pan brioche, doble tocino ahumado crujiente, cheddar inglés y salsa trufada",
            new BigDecimal("58.00"), true
        );
        dishBurger1.getModifierGroups().add(mgCooking);
        dishBurger1.getModifierGroups().add(mgExtras);
        dishBurger1 = dishRepository.save(dishBurger1);
        dishRecipeRepository.save(new DishRecipe(dishBurger1.getId(), ingBeefAngus.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishBurger1.getId(), ingBrioche.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishBurger1.getId(), ingBacon.getId(), new BigDecimal("0.0500")));
        dishRecipeRepository.save(new DishRecipe(dishBurger1.getId(), ingCheddar.getId(), new BigDecimal("0.0400")));
        dishRecipeRepository.save(new DishRecipe(dishBurger1.getId(), ingTruffleSauce.getId(), new BigDecimal("0.0300")));

        // b) Crispy BBQ Bacon Burger
        Dish dishBurger2 = new Dish(
            tenant.getId(), null, null, catBurgers.getId(), "BS-BURG-02",
            "Crispy BBQ Bacon Burger",
            "180g Angus a la parrilla, salsa BBQ ahumada, tocino crocante, queso Gouda y cebolla caramelizada",
            new BigDecimal("52.00"), true
        );
        dishBurger2.getModifierGroups().add(mgCooking);
        dishBurger2.getModifierGroups().add(mgExtras);
        dishBurger2 = dishRepository.save(dishBurger2);
        dishRecipeRepository.save(new DishRecipe(dishBurger2.getId(), ingBeefAngus.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishBurger2.getId(), ingBrioche.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishBurger2.getId(), ingBacon.getId(), new BigDecimal("0.0400")));
        dishRecipeRepository.save(new DishRecipe(dishBurger2.getId(), ingGouda.getId(), new BigDecimal("0.0400")));
        dishRecipeRepository.save(new DishRecipe(dishBurger2.getId(), ingCaramelOnion.getId(), new BigDecimal("0.0300")));

        // c) Smash Bacon Cheeseburger
        Dish dishSmash1 = new Dish(
            tenant.getId(), null, null, catSmash.getId(), "BS-SMASH-01",
            "Smash Bacon Cheeseburger",
            "Doble smash de res Angus costrado a la plancha, doble queso cheddar fundido y trocitos de tocino",
            new BigDecimal("42.00"), true
        );
        dishSmash1.getModifierGroups().add(mgExtras);
        dishSmash1 = dishRepository.save(dishSmash1);
        dishRecipeRepository.save(new DishRecipe(dishSmash1.getId(), ingBeefSmash.getId(), new BigDecimal("2.0000")));
        dishRecipeRepository.save(new DishRecipe(dishSmash1.getId(), ingBrioche.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishSmash1.getId(), ingBacon.getId(), new BigDecimal("0.0300")));
        dishRecipeRepository.save(new DishRecipe(dishSmash1.getId(), ingCheddar.getId(), new BigDecimal("0.0400")));

        // d) Smash Burger Simple
        Dish dishSmash2 = new Dish(
            tenant.getId(), null, null, catSmash.getId(), "BS-SMASH-02",
            "Smash Burger Clásica",
            "Smash simple Angus, queso cheddar americano en pan brioche suave",
            new BigDecimal("34.00"), true
        );
        dishSmash2.getModifierGroups().add(mgExtras);
        dishSmash2 = dishRepository.save(dishSmash2);
        dishRecipeRepository.save(new DishRecipe(dishSmash2.getId(), ingBeefSmash.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishSmash2.getId(), ingBrioche.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishSmash2.getId(), ingCheddar.getId(), new BigDecimal("0.0200")));

        // e) Papas Rústicas con Romero
        Dish dishFries = dishRepository.save(new Dish(
            tenant.getId(), null, null, catSides.getId(), "BS-SIDE-01",
            "Papas Rústicas con Romero",
            "Papas corte rústico fritas con piel, sal de mar y romero fresco",
            new BigDecimal("20.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishFries.getId(), ingRusticPotatoes.getId(), new BigDecimal("0.3000")));

        // f) Aros de Cebolla Crujientes
        Dish dishOnion = dishRepository.save(new Dish(
            tenant.getId(), null, null, catSides.getId(), "BS-SIDE-02",
            "Aros de Cebolla Crujientes",
            "Aros de cebolla rebozados y dorados servidos con salsa de la casa",
            new BigDecimal("22.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishOnion.getId(), ingOnionRings.getId(), new BigDecimal("0.2500")));

        // g) Coca Cola Zero 500ml
        Dish dishCoke = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDrinks.getId(), "BS-DRK-01",
            "Coca Cola Zero 500ml",
            "Gaseosa Coca Cola Zero azúcar 500ml",
            new BigDecimal("12.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishCoke.getId(), ingCokeZero.getId(), new BigDecimal("1.0000")));

        // h) Cerveza IPA Artesanal 330ml
        Dish dishBeer = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDrinks.getId(), "BS-DRK-02",
            "Cerveza IPA Artesanal 330ml",
            "Cerveza artesanal rubia con notas cítricas e intenso lúpulo",
            new BigDecimal("26.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishBeer.getId(), ingIpaBeer.getId(), new BigDecimal("1.0000")));

        // i) Milkshake Frutilla & Vainilla
        Dish dishShake = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDesserts.getId(), "BS-DES-01",
            "Milkshake Frutilla & Vainilla",
            "Batido espeso de helado artesanal con frutillas naturales y crema chantilly",
            new BigDecimal("24.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishShake.getId(), ingShake.getId(), new BigDecimal("1.0000")));

        // 5. Combos
        // Combo Station Doble Bacon
        Combo combo1 = new Combo(
            tenant.getId(), null, catBurgers.getId(), "BS-CMB-01",
            "Combo Station Doble Bacon",
            "Burger Doble Bacon Trufada + Guarnición + Bebida a elección",
            new BigDecimal("75.00")
        );
        ComboItem itemMain = new ComboItem("Hamburguesa Principal", "FIXED", true, dishBurger1.getId(), 1, 1);
        itemMain.addOption(new ComboItemOption(dishBurger1.getId(), BigDecimal.ZERO, true));
        combo1.addItem(itemMain);

        ComboItem itemSide = new ComboItem("Guarnición a Elección", "CHOICE", true, dishFries.getId(), 1, 2);
        itemSide.addOption(new ComboItemOption(dishFries.getId(), BigDecimal.ZERO, true));
        itemSide.addOption(new ComboItemOption(dishOnion.getId(), new BigDecimal("3.00"), false));
        combo1.addItem(itemSide);

        ComboItem itemDrink = new ComboItem("Bebida a Elección", "CHOICE", true, dishCoke.getId(), 1, 3);
        itemDrink.addOption(new ComboItemOption(dishCoke.getId(), BigDecimal.ZERO, true));
        itemDrink.addOption(new ComboItemOption(dishBeer.getId(), new BigDecimal("14.00"), false));
        combo1.addItem(itemDrink);
        comboRepository.save(combo1);

        // Combo Smash Lunch
        Combo combo2 = new Combo(
            tenant.getId(), null, catSmash.getId(), "BS-CMB-02",
            "Combo Smash Lunch",
            "Smash Bacon Cheeseburger + Papas Rústicas + Coca Cola Zero",
            new BigDecimal("55.00")
        );
        ComboItem smashMain = new ComboItem("Hamburguesa Smash", "FIXED", true, dishSmash1.getId(), 1, 1);
        smashMain.addOption(new ComboItemOption(dishSmash1.getId(), BigDecimal.ZERO, true));
        combo2.addItem(smashMain);

        ComboItem smashSide = new ComboItem("Acompañamiento", "FIXED", true, dishFries.getId(), 1, 2);
        smashSide.addOption(new ComboItemOption(dishFries.getId(), BigDecimal.ZERO, true));
        combo2.addItem(smashSide);

        ComboItem smashDrink = new ComboItem("Bebida", "FIXED", true, dishCoke.getId(), 1, 3);
        smashDrink.addOption(new ComboItemOption(dishCoke.getId(), BigDecimal.ZERO, true));
        combo2.addItem(smashDrink);
        comboRepository.save(combo2);

        log.info("Burger Station catalog seeded successfully with {} dishes and {} combos.", 9, 2);
    }

    private void seedBellaItaliaCatalog() {
        Optional<Tenant> tenantOpt = tenantRepository.findByNitOrTaxId("98765432");
        if (tenantOpt.isEmpty()) {
            return;
        }

        Tenant tenant = tenantOpt.get();
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenant.getId(), "Pizzas Tradicionales")) {
            return;
        }

        log.info("Initializing Bella Italia catalog data (tenant: {})...", tenant.getId());

        // 1. Categorías
        Category catPizzasTrad = categoryRepository.save(new Category(tenant.getId(), "Pizzas Tradicionales", "pizza", 1));
        Category catPizzasGourmet = categoryRepository.save(new Category(tenant.getId(), "Pizzas Gourmet", "pizza", 2));
        Category catPastas = categoryRepository.save(new Category(tenant.getId(), "Pastas Frescas", "bowl", 3));
        Category catDesserts = categoryRepository.save(new Category(tenant.getId(), "Postres Italianos", "cake", 4));
        Category catDrinks = categoryRepository.save(new Category(tenant.getId(), "Bebidas & Vinos", "wine", 5));

        // 2. Ingredientes
        Ingredient ingPizzaDough = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Masa Madre Artesanal Napolitana", "UNIT", new BigDecimal("5.0000"))
        );
        Ingredient ingPomodoro = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Salsa Pomodoro San Marzano", "KG", new BigDecimal("22.0000"))
        );
        Ingredient ingMozzarella = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Queso Mozzarella Fior di Latte", "KG", new BigDecimal("42.0000"))
        );
        Ingredient ingPepperoni = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Pepperoni Premium Curado", "KG", new BigDecimal("65.0000"))
        );
        Ingredient ingProsciutto = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Prosciutto di Parma D.O.P.", "KG", new BigDecimal("95.0000"))
        );
        Ingredient ingGorgonzola = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Queso Gorgonzola D.O.P.", "KG", new BigDecimal("70.0000"))
        );
        Ingredient ingBasil = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Albahaca Fresca Genovesa", "KG", new BigDecimal("18.0000"))
        );
        Ingredient ingMushrooms = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Champiñones Portobello Frescos", "KG", new BigDecimal("25.0000"))
        );
        Ingredient ingFettuccine = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Pasta Fettuccine al Huevo", "KG", new BigDecimal("20.0000"))
        );
        Ingredient ingCreamParm = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Salsa Crema y Parmigiano Reggiano", "KG", new BigDecimal("38.0000"))
        );
        Ingredient ingTiramisu = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Porción Tiramisú Artesanal", "UNIT", new BigDecimal("12.0000"))
        );
        Ingredient ingCannoli = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Cannoli Siciliani con Ricotta (par)", "UNIT", new BigDecimal("10.0000"))
        );
        Ingredient ingChiantiWine = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Copa Vino Chianti Reserva", "UNIT", new BigDecimal("14.0000"))
        );
        Ingredient ingPellegrino = ingredientRepository.save(
            new Ingredient(tenant.getId(), "Agua Mineral San Pellegrino 500ml", "UNIT", new BigDecimal("8.0000"))
        );

        // 3. Modificadores
        ModifierGroup mgCrust = new ModifierGroup(tenant.getId(), "Tipo de Masa y Borde", 1, 1, true);
        mgCrust.addModifier(new Modifier(tenant.getId(), "Masa Napolitana al Horno (Clásica)", BigDecimal.ZERO, null, BigDecimal.ZERO, 1));
        mgCrust.addModifier(new Modifier(tenant.getId(), "Masa Fina y Crujiente Romana", BigDecimal.ZERO, null, BigDecimal.ZERO, 2));
        mgCrust.addModifier(new Modifier(tenant.getId(), "Borde Relleno de Mozzarella Fior di Latte", new BigDecimal("15.00"), ingMozzarella.getId(), new BigDecimal("0.0800"), 3));
        mgCrust = modifierGroupRepository.save(mgCrust);

        ModifierGroup mgPizzaToppings = new ModifierGroup(tenant.getId(), "Ingredientes Adicionales", 0, 3, false);
        mgPizzaToppings.addModifier(new Modifier(tenant.getId(), "Extra Mozzarella (+80g)", new BigDecimal("10.00"), ingMozzarella.getId(), new BigDecimal("0.0800"), 1));
        mgPizzaToppings.addModifier(new Modifier(tenant.getId(), "Extra Prosciutto di Parma (+50g)", new BigDecimal("14.00"), ingProsciutto.getId(), new BigDecimal("0.0500"), 2));
        mgPizzaToppings.addModifier(new Modifier(tenant.getId(), "Extra Portobellos Salteados (+60g)", new BigDecimal("8.00"), ingMushrooms.getId(), new BigDecimal("0.0600"), 3));
        mgPizzaToppings = modifierGroupRepository.save(mgPizzaToppings);

        // 4. Platos y Recetas
        // a) Pizza Margherita D.O.P.
        Dish dishMargherita = new Dish(
            tenant.getId(), null, null, catPizzasTrad.getId(), "BI-PIZ-01",
            "Pizza Margherita D.O.P.",
            "Pomodoro San Marzano, mozzarella fior di latte fundida, albahaca genovesa y aceite de oliva virgen extra",
            new BigDecimal("55.00"), true
        );
        dishMargherita.getModifierGroups().add(mgCrust);
        dishMargherita.getModifierGroups().add(mgPizzaToppings);
        dishMargherita = dishRepository.save(dishMargherita);
        dishRecipeRepository.save(new DishRecipe(dishMargherita.getId(), ingPizzaDough.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishMargherita.getId(), ingPomodoro.getId(), new BigDecimal("0.1200")));
        dishRecipeRepository.save(new DishRecipe(dishMargherita.getId(), ingMozzarella.getId(), new BigDecimal("0.1800")));
        dishRecipeRepository.save(new DishRecipe(dishMargherita.getId(), ingBasil.getId(), new BigDecimal("0.0150")));

        // b) Pizza Pepperoni Supremo
        Dish dishPepperoni = new Dish(
            tenant.getId(), null, null, catPizzasTrad.getId(), "BI-PIZ-02",
            "Pizza Pepperoni Supremo",
            "Salsa pomodoro San Marzano, doble mozzarella fior di latte y abundantes rodajas de pepperoni curado italiano",
            new BigDecimal("68.00"), true
        );
        dishPepperoni.getModifierGroups().add(mgCrust);
        dishPepperoni.getModifierGroups().add(mgPizzaToppings);
        dishPepperoni = dishRepository.save(dishPepperoni);
        dishRecipeRepository.save(new DishRecipe(dishPepperoni.getId(), ingPizzaDough.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishPepperoni.getId(), ingPomodoro.getId(), new BigDecimal("0.1200")));
        dishRecipeRepository.save(new DishRecipe(dishPepperoni.getId(), ingMozzarella.getId(), new BigDecimal("0.1800")));
        dishRecipeRepository.save(new DishRecipe(dishPepperoni.getId(), ingPepperoni.getId(), new BigDecimal("0.0800")));

        // c) Pizza Cuatro Quesos & Miel
        Dish dish4Quesos = new Dish(
            tenant.getId(), null, null, catPizzasGourmet.getId(), "BI-PIZ-03",
            "Pizza Cuatro Quesos & Miel",
            "Base bianca con mozzarella fior di latte, gorgonzola D.O.P., parmigiano reggiano y un toque de miel orgánica",
            new BigDecimal("72.00"), true
        );
        dish4Quesos.getModifierGroups().add(mgCrust);
        dish4Quesos.getModifierGroups().add(mgPizzaToppings);
        dish4Quesos = dishRepository.save(dish4Quesos);
        dishRecipeRepository.save(new DishRecipe(dish4Quesos.getId(), ingPizzaDough.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dish4Quesos.getId(), ingMozzarella.getId(), new BigDecimal("0.1500")));
        dishRecipeRepository.save(new DishRecipe(dish4Quesos.getId(), ingGorgonzola.getId(), new BigDecimal("0.0600")));

        // d) Pizza Prosciutto & Funghi
        Dish dishProsciutto = new Dish(
            tenant.getId(), null, null, catPizzasGourmet.getId(), "BI-PIZ-04",
            "Pizza Prosciutto & Funghi",
            "Pomodoro, mozzarella fresca, finas láminas de jamón de Parma curado y champiñones portobello salteados",
            new BigDecimal("78.00"), true
        );
        dishProsciutto.getModifierGroups().add(mgCrust);
        dishProsciutto.getModifierGroups().add(mgPizzaToppings);
        dishProsciutto = dishRepository.save(dishProsciutto);
        dishRecipeRepository.save(new DishRecipe(dishProsciutto.getId(), ingPizzaDough.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(dishProsciutto.getId(), ingPomodoro.getId(), new BigDecimal("0.1200")));
        dishRecipeRepository.save(new DishRecipe(dishProsciutto.getId(), ingMozzarella.getId(), new BigDecimal("0.1500")));
        dishRecipeRepository.save(new DishRecipe(dishProsciutto.getId(), ingProsciutto.getId(), new BigDecimal("0.0600")));
        dishRecipeRepository.save(new DishRecipe(dishProsciutto.getId(), ingMushrooms.getId(), new BigDecimal("0.0600")));

        // e) Fettuccine Alfredo Clásico
        Dish dishPasta = dishRepository.save(new Dish(
            tenant.getId(), null, null, catPastas.getId(), "BI-PAS-01",
            "Fettuccine Alfredo Clásico",
            "Pasta fresca al huevo con cremosa emulsión de mantequilla y Parmigiano Reggiano añejo",
            new BigDecimal("52.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishPasta.getId(), ingFettuccine.getId(), new BigDecimal("0.2000")));
        dishRecipeRepository.save(new DishRecipe(dishPasta.getId(), ingCreamParm.getId(), new BigDecimal("0.1500")));

        // f) Tiramisú Tradicional
        Dish dishTiramisu = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDesserts.getId(), "BI-DES-01",
            "Tiramisú Tradicional",
            "Receta original de Treviso: bizcochos savoiardi café espresso, queso mascarpone y cacao amargo",
            new BigDecimal("30.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishTiramisu.getId(), ingTiramisu.getId(), new BigDecimal("1.0000")));

        // g) Cannoli Siciliani (2 un)
        Dish dishCannoli = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDesserts.getId(), "BI-DES-02",
            "Cannoli Siciliani (2 un)",
            "Masas fritas crujientes rellenas de crema de ricotta dulce, chips de chocolate y pistacho",
            new BigDecimal("28.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishCannoli.getId(), ingCannoli.getId(), new BigDecimal("1.0000")));

        // h) Copa Chianti Reserva
        Dish dishWine = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDrinks.getId(), "BI-DRK-01",
            "Copa Chianti Reserva",
            "Vino tinto italiano D.O.C.G. notas de cereza y roble",
            new BigDecimal("35.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishWine.getId(), ingChiantiWine.getId(), new BigDecimal("1.0000")));

        // i) Agua San Pellegrino 500ml
        Dish dishWater = dishRepository.save(new Dish(
            tenant.getId(), null, null, catDrinks.getId(), "BI-DRK-02",
            "Agua San Pellegrino 500ml",
            "Agua mineral con gas natural importada de los Alpes italianos",
            new BigDecimal("18.00"), true
        ));
        dishRecipeRepository.save(new DishRecipe(dishWater.getId(), ingPellegrino.getId(), new BigDecimal("1.0000")));

        // 5. Combos
        // Combo Serata Italiana (Para 2)
        Combo combo1 = new Combo(
            tenant.getId(), null, catPizzasTrad.getId(), "BI-CMB-01",
            "Combo Serata Italiana (Para 2)",
            "1 Pizza Grande a elección + Tiramisú para compartir + Bebida",
            new BigDecimal("125.00")
        );
        ComboItem itemPizza = new ComboItem("Pizza Grande al Gusto", "CHOICE", true, dishPepperoni.getId(), 1, 1);
        itemPizza.addOption(new ComboItemOption(dishPepperoni.getId(), BigDecimal.ZERO, true));
        itemPizza.addOption(new ComboItemOption(dishMargherita.getId(), BigDecimal.ZERO, false));
        itemPizza.addOption(new ComboItemOption(dishProsciutto.getId(), new BigDecimal("8.00"), false));
        combo1.addItem(itemPizza);

        ComboItem itemDessert = new ComboItem("Postre para Compartir", "FIXED", true, dishTiramisu.getId(), 1, 2);
        itemDessert.addOption(new ComboItemOption(dishTiramisu.getId(), BigDecimal.ZERO, true));
        combo1.addItem(itemDessert);

        ComboItem itemDrink = new ComboItem("Bebida de Acompañamiento", "CHOICE", true, dishWine.getId(), 1, 3);
        itemDrink.addOption(new ComboItemOption(dishWine.getId(), BigDecimal.ZERO, true));
        itemDrink.addOption(new ComboItemOption(dishWater.getId(), BigDecimal.ZERO, false));
        combo1.addItem(itemDrink);
        comboRepository.save(combo1);

        // Combo Pranzo Express
        Combo combo2 = new Combo(
            tenant.getId(), null, catPastas.getId(), "BI-CMB-02",
            "Combo Pranzo Express",
            "Fettuccine Alfredo + Agua San Pellegrino + Cannoli Siciliano",
            new BigDecimal("65.00")
        );
        ComboItem pastaMain = new ComboItem("Plato Principal", "FIXED", true, dishPasta.getId(), 1, 1);
        pastaMain.addOption(new ComboItemOption(dishPasta.getId(), BigDecimal.ZERO, true));
        combo2.addItem(pastaMain);

        ComboItem pastaDrink = new ComboItem("Bebida", "FIXED", true, dishWater.getId(), 1, 2);
        pastaDrink.addOption(new ComboItemOption(dishWater.getId(), BigDecimal.ZERO, true));
        combo2.addItem(pastaDrink);

        ComboItem pastaDessert = new ComboItem("Postre", "FIXED", true, dishCannoli.getId(), 1, 3);
        pastaDessert.addOption(new ComboItemOption(dishCannoli.getId(), BigDecimal.ZERO, true));
        combo2.addItem(pastaDessert);
        comboRepository.save(combo2);

        log.info("Bella Italia catalog seeded successfully with {} dishes and {} combos.", 9, 2);
    }
}
