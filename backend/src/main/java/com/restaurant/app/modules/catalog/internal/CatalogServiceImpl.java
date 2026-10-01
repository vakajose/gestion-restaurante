package com.restaurant.app.modules.catalog.internal;

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
import com.restaurant.app.modules.catalog.api.RecipeIngredientDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
class CatalogServiceImpl implements CatalogPublicApi {

    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final DishRepository dishRepository;
    private final DishRecipeRepository dishRecipeRepository;
    private final BranchDishRepository branchDishRepository;

    CatalogServiceImpl(
        CategoryRepository categoryRepository,
        IngredientRepository ingredientRepository,
        DishRepository dishRepository,
        DishRecipeRepository dishRecipeRepository,
        BranchDishRepository branchDishRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
        this.dishRecipeRepository = dishRecipeRepository;
        this.branchDishRepository = branchDishRepository;
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Contexto de tenant requerido para esta operación");
        }
        return tenantId;
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        UUID tenantId = currentTenantId();
        return categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
            .stream()
            .map(this::toCategoryDto)
            .toList();
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        UUID tenantId = currentTenantId();
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name().trim())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre '" + request.name() + "'");
        }

        Category category = new Category(
            tenantId,
            request.name().trim(),
            request.icon(),
            request.resolvedSortOrder()
        );
        Category saved = categoryRepository.save(category);
        return toCategoryDto(saved);
    }

    // -------------------------------------------------------------------------
    // Ingredients
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<IngredientDto> getIngredients() {
        UUID tenantId = currentTenantId();
        return ingredientRepository.findByTenantIdOrderByNameAsc(tenantId)
            .stream()
            .map(this::toIngredientDto)
            .toList();
    }

    public IngredientDto createIngredient(CreateIngredientRequest request) {
        UUID tenantId = currentTenantId();
        if (ingredientRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name().trim())) {
            throw new IllegalArgumentException("Ya existe un ingrediente con el nombre '" + request.name() + "'");
        }

        Ingredient ingredient = new Ingredient(
            tenantId,
            request.name().trim(),
            request.unitOfMeasure().toUpperCase().trim(),
            request.minStockAlert()
        );
        Ingredient saved = ingredientRepository.save(ingredient);
        return toIngredientDto(saved);
    }

    // -------------------------------------------------------------------------
    // Dishes
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DishDto> getDishes() {
        UUID tenantId = currentTenantId();
        Map<UUID, String> categoryMap = getCategoryMap(tenantId);

        return dishRepository.findByTenantIdOrderByNameAsc(tenantId)
            .stream()
            .map(dish -> toDishDto(dish, categoryMap.getOrDefault(dish.getCategoryId(), "")))
            .toList();
    }

    @Transactional(readOnly = true)
    public DishDto getDishById(UUID dishId) {
        UUID tenantId = currentTenantId();
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        Category category = categoryRepository.findByIdAndTenantId(dish.getCategoryId(), tenantId).orElse(null);
        String categoryName = category != null ? category.getName() : "";

        return toDishDto(dish, categoryName);
    }

    public DishDto createDish(CreateDishRequest request) {
        UUID tenantId = currentTenantId();

        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + request.categoryId()));

        validateDishCodeUniqueness(tenantId, request.branchId(), request.code().trim(), null);

        Dish dish = new Dish(
            tenantId,
            request.branchId(),
            null,
            request.categoryId(),
            request.code().trim(),
            request.name().trim(),
            request.description(),
            request.salePrice(),
            request.resolvedIsActive()
        );
        Dish saved = dishRepository.save(dish);

        if (request.recipeItems() != null && !request.recipeItems().isEmpty()) {
            setDishRecipeInternal(saved.getId(), tenantId, request.recipeItems());
        }

        return toDishDto(saved, category.getName());
    }

    public DishDto updateDish(UUID dishId, CreateDishRequest request) {
        UUID tenantId = currentTenantId();

        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + request.categoryId()));

        if (!dish.getCode().equalsIgnoreCase(request.code().trim())) {
            validateDishCodeUniqueness(tenantId, dish.getBranchId(), request.code().trim(), dishId);
        }

        dish.setCategoryId(request.categoryId());
        dish.setCode(request.code().trim());
        dish.setName(request.name().trim());
        dish.setDescription(request.description());
        dish.setSalePrice(request.salePrice());
        if (request.isActive() != null) {
            dish.setActive(request.isActive());
        }

        Dish updated = dishRepository.save(dish);
        return toDishDto(updated, category.getName());
    }

    // -------------------------------------------------------------------------
    // Recipes (BOM)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DishRecipeDto getDishRecipe(UUID dishId) {
        UUID tenantId = currentTenantId();
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        return buildDishRecipeDto(dish);
    }

    public DishRecipeDto setDishRecipe(UUID dishId, List<DishRecipeItemRequest> items) {
        UUID tenantId = currentTenantId();
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        setDishRecipeInternal(dishId, tenantId, items);
        return buildDishRecipeDto(dish);
    }

    private void setDishRecipeInternal(UUID dishId, UUID tenantId, List<DishRecipeItemRequest> items) {
        if (items == null || items.isEmpty()) {
            dishRecipeRepository.deleteByDishId(dishId);
            return;
        }

        Set<UUID> ingredientIds = new HashSet<>();
        for (DishRecipeItemRequest item : items) {
            if (!ingredientIds.add(item.ingredientId())) {
                throw new IllegalArgumentException("No se permiten ingredientes duplicados en la receta");
            }
        }

        List<Ingredient> ingredients = ingredientRepository.findByTenantIdAndIdIn(tenantId, ingredientIds);
        if (ingredients.size() != ingredientIds.size()) {
            throw new IllegalArgumentException("Uno o más ingredientes especificados no existen o no pertenecen a la empresa");
        }

        dishRecipeRepository.deleteByDishId(dishId);

        for (DishRecipeItemRequest item : items) {
            DishRecipe recipe = new DishRecipe(dishId, item.ingredientId(), item.quantity());
            dishRecipeRepository.save(recipe);
        }
    }

    // -------------------------------------------------------------------------
    // Branch Overrides
    // -------------------------------------------------------------------------

    public DishPriceDto setBranchOverride(UUID dishId, BranchOverrideRequest request) {
        UUID tenantId = currentTenantId();
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        BranchDish branchDish = branchDishRepository.findByBranchIdAndDishId(request.branchId(), dishId)
            .orElseGet(() -> new BranchDish(tenantId, request.branchId(), dishId, true, null));

        if (request.isAvailable() != null) {
            branchDish.setAvailable(request.isAvailable());
        }
        branchDish.setPriceOverride(request.priceOverride());

        branchDishRepository.save(branchDish);

        Category category = categoryRepository.findByIdAndTenantId(dish.getCategoryId(), tenantId).orElse(null);
        String categoryName = category != null ? category.getName() : "";

        boolean hasOverride = branchDish.getPriceOverride() != null;
        BigDecimal effectivePrice = hasOverride ? branchDish.getPriceOverride() : dish.getSalePrice();

        return new DishPriceDto(
            dish.getId(),
            dish.getCode(),
            dish.getName(),
            dish.getDescription(),
            dish.getCategoryId(),
            categoryName,
            dish.getSalePrice(),
            effectivePrice,
            hasOverride,
            branchDish.isAvailable()
        );
    }

    // -------------------------------------------------------------------------
    // Dish Cloning
    // -------------------------------------------------------------------------

    public DishDto cloneDish(UUID dishId, CloneDishRequest request) {
        UUID tenantId = currentTenantId();
        Dish sourceDish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato de origen no encontrado con ID: " + dishId));

        validateDishCodeUniqueness(tenantId, request.targetBranchId(), request.newCode().trim(), null);

        BigDecimal salePrice = request.newSalePrice() != null ? request.newSalePrice() : sourceDish.getSalePrice();

        Dish cloned = new Dish(
            tenantId,
            request.targetBranchId(),
            sourceDish.getId(),
            sourceDish.getCategoryId(),
            request.newCode().trim(),
            request.newName().trim(),
            sourceDish.getDescription(),
            salePrice,
            true
        );
        Dish savedCloned = dishRepository.save(cloned);

        if (request.customRecipe() != null) {
            setDishRecipeInternal(savedCloned.getId(), tenantId, request.customRecipe());
        } else {
            List<DishRecipe> sourceRecipes = dishRecipeRepository.findByDishId(sourceDish.getId());
            for (DishRecipe sr : sourceRecipes) {
                dishRecipeRepository.save(new DishRecipe(savedCloned.getId(), sr.getIngredientId(), sr.getQuantity()));
            }
        }

        Category category = categoryRepository.findByIdAndTenantId(savedCloned.getCategoryId(), tenantId).orElse(null);
        String categoryName = category != null ? category.getName() : "";

        return toDishDto(savedCloned, categoryName);
    }

    // -------------------------------------------------------------------------
    // CatalogPublicApi Implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<DishRecipeDto> getRecipeByDishId(UUID dishId) {
        return dishRepository.findById(dishId).map(this::buildDishRecipeDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getEffectiveDishPrice(UUID dishId, UUID branchId) {
        Dish dish = dishRepository.findById(dishId)
            .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado con ID: " + dishId));

        if (branchId != null) {
            Optional<BranchDish> override = branchDishRepository.findByBranchIdAndDishId(branchId, dishId);
            if (override.isPresent() && override.get().getPriceOverride() != null) {
                return override.get().getPriceOverride();
            }
        }
        return dish.getSalePrice();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDishAvailableInBranch(UUID dishId, UUID branchId) {
        Optional<Dish> dishOpt = dishRepository.findById(dishId);
        if (dishOpt.isEmpty()) {
            return false;
        }

        Dish dish = dishOpt.get();
        if (!dish.isActive()) {
            return false;
        }

        if (dish.getBranchId() != null && !dish.getBranchId().equals(branchId)) {
            return false;
        }

        if (branchId != null) {
            Optional<BranchDish> override = branchDishRepository.findByBranchIdAndDishId(branchId, dishId);
            if (override.isPresent()) {
                return override.get().isAvailable();
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishPriceDto> getActiveMenuForBranch(UUID tenantId, UUID branchId) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(branchId, "branchId cannot be null");

        List<Dish> activeDishes = dishRepository.findActiveDishesForBranch(tenantId, branchId);
        if (activeDishes.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> categoryMap = getCategoryMap(tenantId);

        Map<UUID, BranchDish> overrideMap = branchDishRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .stream()
            .collect(Collectors.toMap(BranchDish::getDishId, b -> b, (a, b) -> a));

        return activeDishes.stream()
            .map(dish -> {
                BranchDish override = overrideMap.get(dish.getId());
                boolean hasOverride = override != null && override.getPriceOverride() != null;
                BigDecimal effectivePrice = hasOverride ? override.getPriceOverride() : dish.getSalePrice();
                boolean isAvailable = override != null ? override.isAvailable() : true;
                String categoryName = categoryMap.getOrDefault(dish.getCategoryId(), "");

                return new DishPriceDto(
                    dish.getId(),
                    dish.getCode(),
                    dish.getName(),
                    dish.getDescription(),
                    dish.getCategoryId(),
                    categoryName,
                    dish.getSalePrice(),
                    effectivePrice,
                    hasOverride,
                    isAvailable
                );
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DishDto> findDishById(UUID dishId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return dishRepository.findById(dishId)
                .map(d -> {
                    Category category = categoryRepository.findById(d.getCategoryId()).orElse(null);
                    return toDishDto(d, category != null ? category.getName() : "");
                });
        }
        return dishRepository.findByIdAndTenantId(dishId, tenantId)
            .map(d -> {
                Category category = categoryRepository.findByIdAndTenantId(d.getCategoryId(), tenantId).orElse(null);
                return toDishDto(d, category != null ? category.getName() : "");
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientDto> getAllIngredients(UUID tenantId) {
        return ingredientRepository.findByTenantIdOrderByNameAsc(tenantId)
            .stream()
            .map(this::toIngredientDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IngredientDto> findIngredientById(UUID ingredientId) {
        return ingredientRepository.findById(ingredientId)
            .map(this::toIngredientDto);
    }

    // -------------------------------------------------------------------------
    // Helper Methods & Mappings
    // -------------------------------------------------------------------------

    private void validateDishCodeUniqueness(UUID tenantId, UUID branchId, String code, UUID excludeDishId) {
        boolean exists;
        if (branchId == null) {
            exists = dishRepository.existsByTenantIdAndBranchIdIsNullAndCode(tenantId, code);
        } else {
            exists = dishRepository.existsByTenantIdAndBranchIdAndCode(tenantId, branchId, code);
        }

        if (exists) {
            if (excludeDishId != null) {
                Dish existing = dishRepository.findByIdAndTenantId(excludeDishId, tenantId).orElse(null);
                if (existing != null && existing.getCode().equalsIgnoreCase(code)) {
                    return;
                }
            }
            throw new IllegalArgumentException("Ya existe un plato con el código '" + code + "' en el mismo ámbito");
        }
    }

    private DishRecipeDto buildDishRecipeDto(Dish dish) {
        List<DishRecipe> recipes = dishRecipeRepository.findByDishId(dish.getId());
        if (recipes.isEmpty()) {
            return new DishRecipeDto(dish.getId(), dish.getName(), List.of());
        }

        Set<UUID> ingredientIds = recipes.stream().map(DishRecipe::getIngredientId).collect(Collectors.toSet());
        Map<UUID, Ingredient> ingredientMap = ingredientRepository.findByTenantIdAndIdIn(dish.getTenantId(), ingredientIds)
            .stream()
            .collect(Collectors.toMap(Ingredient::getId, i -> i, (a, b) -> a));

        List<RecipeIngredientDto> items = recipes.stream()
            .map(r -> {
                Ingredient ing = ingredientMap.get(r.getIngredientId());
                String ingName = ing != null ? ing.getName() : "Desconocido";
                String uom = ing != null ? ing.getUnitOfMeasure() : "UNIT";
                return new RecipeIngredientDto(r.getIngredientId(), ingName, uom, r.getQuantity());
            })
            .toList();

        return new DishRecipeDto(dish.getId(), dish.getName(), items);
    }

    private Map<UUID, String> getCategoryMap(UUID tenantId) {
        return categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
            .stream()
            .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getTenantId(),
            category.getName(),
            category.getIcon(),
            category.getSortOrder(),
            category.getCreatedAt()
        );
    }

    private IngredientDto toIngredientDto(Ingredient ingredient) {
        return new IngredientDto(
            ingredient.getId(),
            ingredient.getTenantId(),
            ingredient.getName(),
            ingredient.getUnitOfMeasure(),
            ingredient.getMinStockAlert(),
            ingredient.getCreatedAt()
        );
    }

    private DishDto toDishDto(Dish dish, String categoryName) {
        return new DishDto(
            dish.getId(),
            dish.getTenantId(),
            dish.getBranchId(),
            dish.getClonedFromId(),
            dish.getCategoryId(),
            categoryName,
            dish.getCode(),
            dish.getName(),
            dish.getDescription(),
            dish.getSalePrice(),
            dish.isActive(),
            dish.getCreatedAt(),
            dish.getVersion()
        );
    }
}
