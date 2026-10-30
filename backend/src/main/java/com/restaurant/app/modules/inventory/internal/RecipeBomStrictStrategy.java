package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.exception.InsufficientStockException;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.catalog.api.RecipeIngredientDto;
import com.restaurant.app.modules.pos.api.ModifierItemSummary;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class RecipeBomStrictStrategy implements InventoryDeductionStrategy {

    private static final Logger log = LoggerFactory.getLogger(RecipeBomStrictStrategy.class);

    private final CatalogPublicApi catalogPublicApi;
    private final BranchStockRepository branchStockRepository;
    private final KardexMovementRepository kardexMovementRepository;

    RecipeBomStrictStrategy(
        CatalogPublicApi catalogPublicApi,
        BranchStockRepository branchStockRepository,
        KardexMovementRepository kardexMovementRepository
    ) {
        this.catalogPublicApi = catalogPublicApi;
        this.branchStockRepository = branchStockRepository;
        this.kardexMovementRepository = kardexMovementRepository;
    }

    @Override
    public boolean supports(InventoryStrategy strategy) {
        return strategy == InventoryStrategy.RECIPE_BOM_STRICT;
    }

    @Override
    public void deduct(UUID tenantId, UUID branchId, UUID orderId, OrderItemSummary item, boolean allowNegative) {
        List<UUID> targetDishes = new ArrayList<>();
        if (item.comboId() != null && item.componentDishIds() != null && !item.componentDishIds().isEmpty()) {
            targetDishes.addAll(item.componentDishIds());
        } else {
            targetDishes.add(item.dishId());
        }

        BigDecimal orderQty = BigDecimal.valueOf(item.quantity());

        for (UUID targetDishId : targetDishes) {
            Optional<DishRecipeDto> recipeOpt = catalogPublicApi.getRecipeForDish(tenantId, targetDishId);
            if (recipeOpt.isEmpty() || recipeOpt.get().items() == null || recipeOpt.get().items().isEmpty()) {
                log.warn("Plato {} marcado como RECIPE_BOM_STRICT pero no tiene ingredientes definidos.", targetDishId);
                continue;
            }

            for (RecipeIngredientDto recipeItem : recipeOpt.get().items()) {
                BigDecimal neededQty = recipeItem.quantity().multiply(orderQty);
                deductIngredientStock(tenantId, branchId, orderId, recipeItem.ingredientId(), recipeItem.ingredientName(), neededQty, allowNegative);
            }
        }

        // Deducción de modificadores si aplican insumos
        if (item.modifiers() != null) {
            for (ModifierItemSummary mod : item.modifiers()) {
                if (mod.ingredientId() != null && mod.quantityConsumed() != null && mod.quantityConsumed().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal neededQty = mod.quantityConsumed().multiply(orderQty);
                    deductIngredientStock(tenantId, branchId, orderId, mod.ingredientId(), mod.name(), neededQty, allowNegative);
                }
            }
        }
    }

    private void deductIngredientStock(
        UUID tenantId,
        UUID branchId,
        UUID orderId,
        UUID ingredientId,
        String ingredientName,
        BigDecimal neededQty,
        boolean allowNegative
    ) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientId)
            .orElse(null);

        BigDecimal currentQty = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;

        if (currentQty.compareTo(neededQty) < 0) {
            if (!allowNegative) {
                throw new InsufficientStockException(
                    "Stock insuficiente para el insumo '" + ingredientName +
                    "'. Requerido: " + neededQty + ", disponible: " + currentQty
                );
            }
            log.warn("STOCK_DEFICIT_WARNING: Insumo '{}' ({}) con déficit post-venta. Saldo previo: {}, requerido: {}",
                ingredientName, ingredientId, currentQty, neededQty);
        }

        BigDecimal unitCost = stock != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQty.subtract(neededQty);

        if (stock != null) {
            stock.setCurrentQuantity(newQuantity);
            branchStockRepository.save(stock);
        } else {
            stock = new BranchStock(tenantId, branchId, ingredientId, null, newQuantity, unitCost);
            branchStockRepository.save(stock);
        }

        BigDecimal totalCost = unitCost.multiply(neededQty).setScale(2, RoundingMode.HALF_UP);
        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            ingredientId,
            null,
            "SALE_OUT",
            neededQty,
            unitCost,
            totalCost,
            newQuantity,
            orderId,
            Instant.now(),
            null
        );
        kardexMovementRepository.save(movement);
    }
}
