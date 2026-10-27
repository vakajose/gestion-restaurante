package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogPublicApi {

    Optional<DishRecipeDto> getRecipeByDishId(UUID dishId);

    default Optional<DishRecipeDto> getRecipeForDish(UUID tenantId, UUID dishId) {
        return getRecipeByDishId(dishId);
    }

    BigDecimal getEffectiveDishPrice(UUID dishId, UUID branchId);

    boolean isDishAvailableInBranch(UUID dishId, UUID branchId);

    List<DishPriceDto> getActiveMenuForBranch(UUID tenantId, UUID branchId);

    Optional<DishDto> findDishById(UUID dishId);

    List<IngredientDto> getAllIngredients(UUID tenantId);

    Optional<IngredientDto> findIngredientById(UUID ingredientId);

    List<ComboDto> getActiveCombosForBranch(UUID tenantId, UUID branchId);

    Optional<ComboDto> findComboById(UUID comboId);

    List<ModifierGroupDto> getModifierGroups(UUID tenantId);

    ModifierGroupDto createModifierGroup(UUID tenantId, CreateModifierGroupRequest request);

    void assignModifierGroupsToDish(UUID tenantId, UUID dishId, List<UUID> groupIds);

    List<ModifierGroupDto> getModifierGroupsForDish(UUID dishId);
}
