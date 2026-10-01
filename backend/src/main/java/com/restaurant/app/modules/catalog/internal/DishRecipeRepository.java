package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface DishRecipeRepository extends JpaRepository<DishRecipe, UUID> {

    List<DishRecipe> findByDishId(UUID dishId);

    @Modifying
    @Query("DELETE FROM DishRecipe r WHERE r.dishId = :dishId")
    void deleteByDishId(@Param("dishId") UUID dishId);

    boolean existsByDishIdAndIngredientId(UUID dishId, UUID ingredientId);
}
