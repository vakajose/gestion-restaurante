package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.storage.api.MediaStorageService;
import com.restaurant.app.modules.catalog.api.BranchOverrideRequest;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.CategoryDto;
import com.restaurant.app.modules.catalog.api.CloneDishRequest;
import com.restaurant.app.modules.catalog.api.ComboDto;
import com.restaurant.app.modules.catalog.api.ComboItemDto;
import com.restaurant.app.modules.catalog.api.ComboItemOptionDto;
import com.restaurant.app.modules.catalog.api.CreateCategoryRequest;
import com.restaurant.app.modules.catalog.api.CreateComboItemOptionRequest;
import com.restaurant.app.modules.catalog.api.CreateComboItemRequest;
import com.restaurant.app.modules.catalog.api.CreateComboRequest;
import com.restaurant.app.modules.catalog.api.CreateDishRequest;
import com.restaurant.app.modules.catalog.api.CreateIngredientRequest;
import com.restaurant.app.modules.catalog.api.CreateModifierGroupRequest;
import com.restaurant.app.modules.catalog.api.CreateModifierRequest;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.DishRecipeItemRequest;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.catalog.api.ModifierDto;
import com.restaurant.app.modules.catalog.api.ModifierGroupDto;
import com.restaurant.app.modules.catalog.api.NextDishCodeResponse;
import com.restaurant.app.modules.catalog.api.RecipeIngredientDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final ComboRepository comboRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final MediaStorageService mediaStorageService;

    CatalogServiceImpl(
        CategoryRepository categoryRepository,
        IngredientRepository ingredientRepository,
        DishRepository dishRepository,
        DishRecipeRepository dishRecipeRepository,
        BranchDishRepository branchDishRepository,
        ComboRepository comboRepository,
        ModifierGroupRepository modifierGroupRepository,
        ModifierRepository modifierRepository,
        MediaStorageService mediaStorageService
    ) {
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
        this.dishRecipeRepository = dishRecipeRepository;
        this.branchDishRepository = branchDishRepository;
        this.comboRepository = comboRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.mediaStorageService = mediaStorageService;
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedOperationException("Contexto de tenant requerido para esta operación");
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
            throw new ResourceConflictException("Ya existe una categoría con el nombre '" + request.name() + "'");
        }

        String rawPrefix = request.codePrefix();
        if (rawPrefix == null || rawPrefix.isBlank()) {
            rawPrefix = request.name().trim().replaceAll("[^a-zA-Z0-9]", "");
            if (rawPrefix.length() > 3) {
                rawPrefix = rawPrefix.substring(0, 3);
            }
            if (rawPrefix.isBlank()) {
                rawPrefix = "CAT";
            }
        }
        String prefix = rawPrefix.trim().toUpperCase();

        if (categoryRepository.existsByTenantIdAndCodePrefixIgnoreCase(tenantId, prefix)) {
            throw new ResourceConflictException("Ya existe una categoría con el prefijo '" + prefix + "'");
        }

        Category category = new Category(
            tenantId,
            request.name().trim(),
            request.icon(),
            request.resolvedSortOrder(),
            prefix
        );
        Category saved = categoryRepository.save(category);
        return toCategoryDto(saved);
    }

    @Transactional(readOnly = true)
    public NextDishCodeResponse suggestNextDishCode(UUID categoryId) {
        UUID tenantId = currentTenantId();
        Category category = categoryRepository.findByIdAndTenantId(categoryId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));
        String prefix = category.getCodePrefix().toUpperCase();
        List<String> codes = dishRepository.findCodesByTenantAndPrefixPattern(tenantId, prefix + "-%");

        int maxSeq = 0;
        for (String code : codes) {
            String suffix = code.substring(prefix.length() + 1);
            try {
                int seq = Integer.parseInt(suffix);
                if (seq > maxSeq) {
                    maxSeq = seq;
                }
            } catch (NumberFormatException ignored) {}
        }
        int next = maxSeq + 1;
        return new NextDishCodeResponse(prefix + "-" + next, prefix, next);
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
            throw new ResourceConflictException("Ya existe un ingrediente con el nombre '" + request.name() + "'");
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
        dish.setInventoryStrategy(request.resolvedInventoryStrategy());
        dish.setAllowNegativeStockOverride(request.allowNegativeStockOverride());
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            dish.setImageUrl(request.imageUrl().trim());
        }
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
        if (request.imageUrl() != null) {
            dish.setImageUrl(request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        }

        Dish updated = dishRepository.save(dish);
        return toDishDto(updated, category.getName());
    }

    @Transactional
    public DishDto uploadDishImage(UUID dishId, org.springframework.web.multipart.MultipartFile file) {
        UUID tenantId = currentTenantId();
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        String previousImageUrl = dish.getImageUrl();
        String imageUrl = mediaStorageService.storeDishImage(tenantId, dishId, file);
        dish.setImageUrl(imageUrl);
        Dish updated = dishRepository.save(dish);

        if (previousImageUrl != null && !previousImageUrl.isBlank() && !previousImageUrl.equals(imageUrl)) {
            mediaStorageService.deleteByUrl(previousImageUrl);
        }

        Category category = categoryRepository.findByIdAndTenantId(updated.getCategoryId(), tenantId).orElse(null);
        String categoryName = category != null ? category.getName() : "";
        return toDishDto(updated, categoryName);
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
                throw new BusinessRuleException("No se permiten ingredientes duplicados en la receta");
            }
        }

        List<Ingredient> ingredients = ingredientRepository.findByTenantIdAndIdIn(tenantId, ingredientIds);
        if (ingredients.size() != ingredientIds.size()) {
            throw new EntityNotFoundException("Uno o más ingredientes especificados no existen o no pertenecen a la empresa");
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
            branchDish.isAvailable(),
            List.of(),
            dish.getImageUrl()
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
        cloned.setImageUrl(sourceDish.getImageUrl());
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
                    isAvailable,
                    toModifierGroupDtos(dish.getModifierGroups()),
                    dish.getImageUrl()
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
            throw new ResourceConflictException("Ya existe un plato con el código '" + code + "' en el mismo ámbito");
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
            category.getCodePrefix(),
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
            dish.getVersion(),
            dish.getInventoryStrategy(),
            dish.getAllowNegativeStockOverride(),
            dish.getImageUrl()
        );
    }

    // -------------------------------------------------------------------------
    // Combos
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ComboDto> getActiveCombosForBranch(UUID tenantId, UUID branchId) {
        List<Combo> combos = comboRepository.findActiveCombosForBranch(tenantId, branchId);
        Map<UUID, String> dishNames = dishRepository.findByTenantId(tenantId).stream()
            .collect(Collectors.toMap(Dish::getId, Dish::getName, (a, b) -> a));

        return combos.stream()
            .map(c -> toComboDto(c, dishNames))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComboDto> findComboById(UUID comboId) {
        return comboRepository.findById(comboId)
            .map(c -> {
                Map<UUID, String> dishNames = dishRepository.findByTenantId(c.getTenantId()).stream()
                    .collect(Collectors.toMap(Dish::getId, Dish::getName, (a, b) -> a));
                return toComboDto(c, dishNames);
            });
    }

    @Transactional(readOnly = true)
    public List<ComboDto> getCombos(UUID branchId) {
        return getActiveCombosForBranch(currentTenantId(), branchId);
    }

    public ComboDto createCombo(CreateComboRequest request) {
        UUID tenantId = currentTenantId();
        if (comboRepository.findByTenantIdAndCode(tenantId, request.code()).isPresent()) {
            throw new ResourceConflictException("Ya existe un combo con el código: " + request.code());
        }

        Combo combo = new Combo(
            tenantId,
            request.branchId(),
            request.categoryId(),
            request.code(),
            request.name(),
            request.description(),
            request.basePrice()
        );

        if (request.items() != null) {
            for (CreateComboItemRequest itemReq : request.items()) {
                ComboItem item = new ComboItem(
                    itemReq.name(),
                    itemReq.selectionMode(),
                    itemReq.isRequired(),
                    itemReq.defaultDishId(),
                    itemReq.quantity(),
                    itemReq.sortOrder()
                );
                if (itemReq.options() != null) {
                    for (CreateComboItemOptionRequest optReq : itemReq.options()) {
                        item.addOption(new ComboItemOption(
                            optReq.dishId(),
                            optReq.priceAdjustment(),
                            optReq.isDefault()
                        ));
                    }
                }
                combo.addItem(item);
            }
        }

        Combo saved = comboRepository.save(combo);
        Map<UUID, String> dishNames = dishRepository.findByTenantId(tenantId).stream()
            .collect(Collectors.toMap(Dish::getId, Dish::getName, (a, b) -> a));
        return toComboDto(saved, dishNames);
    }

    private ComboDto toComboDto(Combo combo, Map<UUID, String> dishNames) {
        List<ComboItemDto> itemDtos = combo.getItems().stream().map(item -> {
            List<ComboItemOptionDto> optDtos = item.getOptions().stream().map(opt ->
                new ComboItemOptionDto(
                    opt.getId(),
                    opt.getDishId(),
                    dishNames.getOrDefault(opt.getDishId(), "Plato"),
                    opt.getPriceAdjustment(),
                    opt.isDefault()
                )
            ).toList();

            String defaultDishName = item.getDefaultDishId() != null
                ? dishNames.getOrDefault(item.getDefaultDishId(), null)
                : null;

            return new ComboItemDto(
                item.getId(),
                item.getName(),
                item.getSelectionMode(),
                item.isRequired(),
                item.getDefaultDishId(),
                defaultDishName,
                item.getQuantity(),
                item.getSortOrder(),
                optDtos
            );
        }).toList();

        return new ComboDto(
            combo.getId(),
            combo.getTenantId(),
            combo.getBranchId(),
            combo.getCategoryId(),
            combo.getCode(),
            combo.getName(),
            combo.getDescription(),
            combo.getBasePrice(),
            combo.isActive(),
            itemDtos
        );
    }

    // -------------------------------------------------------------------------
    // Modifier Groups (TASK-210)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroupDto> getModifierGroups(UUID tenantId) {
        return modifierGroupRepository.findAllWithModifiersByTenantId(tenantId)
            .stream()
            .map(this::toModifierGroupDto)
            .toList();
    }

    @Override
    public ModifierGroupDto createModifierGroup(UUID tenantId, CreateModifierGroupRequest request) {
        ModifierGroup group = new ModifierGroup(
            tenantId,
            request.name(),
            request.minSelectable(),
            request.maxSelectable(),
            request.isRequired()
        );

        if (request.modifiers() != null) {
            int order = 0;
            for (CreateModifierRequest modReq : request.modifiers()) {
                Modifier mod = new Modifier(
                    tenantId,
                    modReq.name(),
                    modReq.additionalPrice(),
                    modReq.ingredientId(),
                    modReq.quantityConsumed(),
                    modReq.sortOrder() != null ? modReq.sortOrder() : order++
                );
                group.addModifier(mod);
            }
        }

        ModifierGroup saved = modifierGroupRepository.save(group);
        return toModifierGroupDto(saved);
    }

    @Override
    public void assignModifierGroupsToDish(UUID tenantId, UUID dishId, List<UUID> groupIds) {
        Dish dish = dishRepository.findByIdAndTenantId(dishId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Plato no encontrado con ID: " + dishId));

        if (groupIds == null || groupIds.isEmpty()) {
            dish.getModifierGroups().clear();
        } else {
            List<ModifierGroup> groups = modifierGroupRepository.findAllById(groupIds);
            dish.setModifierGroups(new LinkedHashSet<>(groups));
        }
        dishRepository.save(dish);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModifierGroupDto> getModifierGroupsForDish(UUID dishId) {
        return dishRepository.findById(dishId)
            .map(dish -> toModifierGroupDtos(dish.getModifierGroups()))
            .orElse(List.of());
    }

    // -------------------------------------------------------------------------
    // Demo Catalog Seeding (ADR-0009 / TASK-334)
    // -------------------------------------------------------------------------

    @org.springframework.context.event.EventListener
    public void onTenantOnboarded(com.restaurant.app.modules.company.api.TenantOnboardedEvent event) {
        seedDemoCatalog(event.tenantId(), event.branchId(), event.businessTemplate());
    }

    @Override
    @Transactional
    public void seedDemoCatalog(UUID tenantId, UUID branchId, String businessTemplate) {
        if (tenantId == null || businessTemplate == null || businessTemplate.isBlank()) {
            return;
        }

        String template = businessTemplate.trim().toUpperCase();
        if ("EMPTY".equals(template)) {
            return;
        }

        switch (template) {
            case "FAST_FOOD" -> seedFastFoodDemo(tenantId);
            case "TRADITIONAL" -> seedTraditionalDemo(tenantId);
            case "CAFE", "BAR", "CAFE_BAR" -> seedCafeBarDemo(tenantId);
            default -> {
                // Unknown template or custom -> no demo dishes
            }
        }
    }

    private void seedFastFoodDemo(UUID tenantId) {
        Category catFastFood = categoryRepository.save(new Category(tenantId, "Comidas Rápidas", "hamburger", 1));
        Category catDrinks = categoryRepository.save(new Category(tenantId, "Bebidas", "cup-soda", 2));

        Ingredient ingBread = ingredientRepository.save(new Ingredient(tenantId, "Pan de Hamburguesa", "UNIT", new BigDecimal("10")));
        Ingredient ingBeef = ingredientRepository.save(new Ingredient(tenantId, "Carne de Res Molida", "GRAM", new BigDecimal("1000")));
        Ingredient ingDough = ingredientRepository.save(new Ingredient(tenantId, "Masa de Pizza", "UNIT", new BigDecimal("10")));
        Ingredient ingCheese = ingredientRepository.save(new Ingredient(tenantId, "Queso Mozzarella", "GRAM", new BigDecimal("500")));
        Ingredient ingSoda = ingredientRepository.save(new Ingredient(tenantId, "Refresco 500ml", "UNIT", new BigDecimal("12")));

        Dish burger = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catFastFood.getId(),
            "FF-001",
            "Hamburguesa Clásica",
            "Hamburguesa artesanal a la parrilla con queso y vegetales frescos",
            new BigDecimal("35.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(burger.getId(), ingBread.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(burger.getId(), ingBeef.getId(), new BigDecimal("150.0000")));

        Dish pizza = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catFastFood.getId(),
            "FF-002",
            "Pizza Personal",
            "Pizza artesanal personal con salsa pomodoro y mozzarella fundida",
            new BigDecimal("40.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(pizza.getId(), ingDough.getId(), new BigDecimal("1.0000")));
        dishRecipeRepository.save(new DishRecipe(pizza.getId(), ingCheese.getId(), new BigDecimal("120.0000")));

        Dish soda = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catDrinks.getId(),
            "FF-003",
            "Refresco 500ml",
            "Bebida gaseosa fría de 500ml",
            new BigDecimal("10.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(soda.getId(), ingSoda.getId(), new BigDecimal("1.0000")));
    }

    private void seedTraditionalDemo(UUID tenantId) {
        Category catStarters = categoryRepository.save(new Category(tenantId, "Entradas y Sopas", "soup", 1));
        Category catMains = categoryRepository.save(new Category(tenantId, "Platos Fuertes", "utensils", 2));
        Category catDesserts = categoryRepository.save(new Category(tenantId, "Postres", "cake", 3));

        Ingredient ingPeanut = ingredientRepository.save(new Ingredient(tenantId, "Maní Molido", "GRAM", new BigDecimal("500")));
        Ingredient ingSteak = ingredientRepository.save(new Ingredient(tenantId, "Corte Bife Chorizo", "GRAM", new BigDecimal("1500")));
        Ingredient ingDessert = ingredientRepository.save(new Ingredient(tenantId, "Porción Postre Casero", "UNIT", new BigDecimal("5")));

        Dish soup = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catStarters.getId(),
            "TR-001",
            "Sopa de Maní",
            "Sopa tradicional boliviana de maní con papas fritas hilo y perejil",
            new BigDecimal("20.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(soup.getId(), ingPeanut.getId(), new BigDecimal("80.0000")));

        Dish steak = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catMains.getId(),
            "TR-002",
            "Bife Chorizo a la Carta",
            "Corte selecto de res a la parrilla con guarnición de arroz con queso y ensalada",
            new BigDecimal("65.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(steak.getId(), ingSteak.getId(), new BigDecimal("300.0000")));

        Dish dessert = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catDesserts.getId(),
            "TR-003",
            "Postre de la Casa",
            "Postre tradicional elaborado diariamente en cocina",
            new BigDecimal("25.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(dessert.getId(), ingDessert.getId(), new BigDecimal("1.0000")));
    }

    private void seedCafeBarDemo(UUID tenantId) {
        Category catCoffee = categoryRepository.save(new Category(tenantId, "Cafetería", "coffee", 1));
        Category catSnacks = categoryRepository.save(new Category(tenantId, "Snacks y Sándwiches", "sandwich", 2));
        Category catDrinks = categoryRepository.save(new Category(tenantId, "Bebidas Artesanales", "glass-water", 3));

        Ingredient ingCoffee = ingredientRepository.save(new Ingredient(tenantId, "Café en Grano Especial", "GRAM", new BigDecimal("500")));
        Ingredient ingBread = ingredientRepository.save(new Ingredient(tenantId, "Pan Ciabatta", "UNIT", new BigDecimal("10")));
        Ingredient ingJuice = ingredientRepository.save(new Ingredient(tenantId, "Jugo Natural", "ML", new BigDecimal("2000")));

        Dish espresso = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catCoffee.getId(),
            "CF-001",
            "Café Especial Espresso",
            "Café de altura boliviano recién molido",
            new BigDecimal("18.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(espresso.getId(), ingCoffee.getId(), new BigDecimal("18.0000")));

        Dish sandwich = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catSnacks.getId(),
            "CF-002",
            "Sándwich Gourmet",
            "Sándwich en pan ciabatta tostado con jamón serrano y queso",
            new BigDecimal("28.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(sandwich.getId(), ingBread.getId(), new BigDecimal("1.0000")));

        Dish drink = dishRepository.save(new Dish(
            tenantId,
            null,
            null,
            catDrinks.getId(),
            "CF-003",
            "Jugo / Bebida Artesanal",
            "Bebida refrescante de fruta natural de estación",
            new BigDecimal("15.00"),
            true
        ));
        dishRecipeRepository.save(new DishRecipe(drink.getId(), ingJuice.getId(), new BigDecimal("350.0000")));
    }

    private List<ModifierGroupDto> toModifierGroupDtos(Set<ModifierGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        return groups.stream().map(this::toModifierGroupDto).toList();
    }

    private ModifierGroupDto toModifierGroupDto(ModifierGroup mg) {
        List<ModifierDto> modifierDtos = mg.getModifiers().stream()
            .map(m -> new ModifierDto(
                m.getId(),
                m.getName(),
                m.getAdditionalPrice(),
                m.getIngredientId(),
                m.getQuantityConsumed(),
                m.isActive(),
                m.getSortOrder()
            ))
            .toList();

        return new ModifierGroupDto(
            mg.getId(),
            mg.getName(),
            mg.getMinSelectable(),
            mg.getMaxSelectable(),
            mg.isRequired(),
            modifierDtos
        );
    }
}
