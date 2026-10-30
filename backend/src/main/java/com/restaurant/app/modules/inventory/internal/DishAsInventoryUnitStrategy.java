package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.exception.InsufficientStockException;
import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Component
class DishAsInventoryUnitStrategy implements InventoryDeductionStrategy {

    private static final Logger log = LoggerFactory.getLogger(DishAsInventoryUnitStrategy.class);

    private final BranchStockRepository branchStockRepository;
    private final KardexMovementRepository kardexMovementRepository;

    DishAsInventoryUnitStrategy(
        BranchStockRepository branchStockRepository,
        KardexMovementRepository kardexMovementRepository
    ) {
        this.branchStockRepository = branchStockRepository;
        this.kardexMovementRepository = kardexMovementRepository;
    }

    @Override
    public boolean supports(InventoryStrategy strategy) {
        return strategy == InventoryStrategy.DISH_AS_INVENTORY_UNIT;
    }

    @Override
    public void deduct(UUID tenantId, UUID branchId, UUID orderId, OrderItemSummary item, boolean allowNegative) {
        BigDecimal neededQty = BigDecimal.valueOf(item.quantity());

        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndDishId(tenantId, branchId, item.dishId())
            .orElse(null);

        BigDecimal currentQty = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;

        if (currentQty.compareTo(neededQty) < 0) {
            if (!allowNegative) {
                throw new InsufficientStockException(
                    "Stock insuficiente en almacén para el plato '" + item.dishName() +
                    "'. Requerido: " + neededQty + ", disponible: " + currentQty
                );
            }
            log.warn("STOCK_DEFICIT_WARNING: Plato '{}' ({}) con déficit post-venta. Saldo previo: {}, requerido: {}",
                item.dishName(), item.dishId(), currentQty, neededQty);
        }

        BigDecimal unitCost = stock != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQty.subtract(neededQty);

        if (stock != null) {
            stock.setCurrentQuantity(newQuantity);
            branchStockRepository.save(stock);
        } else {
            stock = new BranchStock(tenantId, branchId, null, item.dishId(), newQuantity, unitCost);
            branchStockRepository.save(stock);
        }

        BigDecimal totalCost = unitCost.multiply(neededQty).setScale(2, RoundingMode.HALF_UP);
        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            null,
            item.dishId(),
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
