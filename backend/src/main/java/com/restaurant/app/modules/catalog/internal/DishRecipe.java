package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dish_recipes")
class DishRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dish_id", nullable = false)
    private UUID dishId;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    DishRecipe() {
    }

    DishRecipe(UUID dishId, UUID ingredientId, BigDecimal quantity) {
        this.dishId = dishId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDishId() {
        return dishId;
    }

    public void setDishId(UUID dishId) {
        this.dishId = dishId;
    }

    public UUID getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(UUID ingredientId) {
        this.ingredientId = ingredientId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
