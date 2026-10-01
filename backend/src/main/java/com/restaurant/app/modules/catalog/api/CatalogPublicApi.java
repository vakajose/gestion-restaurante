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
}
