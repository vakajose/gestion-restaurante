package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
class AutomaticStrictStrategy implements DeductionStrategy {

    private static final Logger log = LoggerFactory.getLogger(AutomaticStrictStrategy.class);

    private final BranchStockRepository branchStockRepository;
    private final KardexMovementRepository kardexMovementRepository;

    AutomaticStrictStrategy(
        BranchStockRepository branchStockRepository,
        KardexMovementRepository kardexMovementRepository
    ) {
        this.branchStockRepository = branchStockRepository;
        this.kardexMovementRepository = kardexMovementRepository;
    }

    @Override
    @Transactional
    public void executeDeduction(UUID tenantId, UUID branchId, OrderPaidEvent event, List<RecipeDeductionItem> items) {
        // En modo estricto, verificar disponibilidad previa
        for (RecipeDeductionItem item : items) {
            BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, item.ingredientId())
                .orElse(null);

            BigDecimal current = stock != null && stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
            if (current.compareTo(item.totalQuantityNeeded()) < 0) {
                log.warn("STOCK_DEFICIT_WARNING [AUTOMATIC_STRICT]: Pedido {} cobrado requiere {} de '{}', pero el stock actual es {}.",
                    event.ticketNumber(), item.totalQuantityNeeded(), item.ingredientName(), current);
            }
        }

        // Aplicar la deducción y movimiento en Kardex
        for (RecipeDeductionItem item : items) {
            BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, item.ingredientId())
                .orElseGet(() -> {
                    BranchStock newStock = new BranchStock(tenantId, branchId, item.ingredientId(), BigDecimal.ZERO, BigDecimal.ZERO);
                    return branchStockRepository.save(newStock);
                });

            BigDecimal previousBalance = stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
            BigDecimal newBalance = previousBalance.subtract(item.totalQuantityNeeded());
            stock.setCurrentQuantity(newBalance);
            branchStockRepository.save(stock);

            BigDecimal unitCost = stock.getAverageUnitCost() != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
            BigDecimal totalCost = item.totalQuantityNeeded().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

            KardexMovement movement = new KardexMovement(
                tenantId,
                branchId,
                item.ingredientId(),
                "SALE_OUT",
                item.totalQuantityNeeded(),
                unitCost,
                totalCost,
                newBalance,
                event.orderId(),
                event.paidAt(),
                null
            );
            kardexMovementRepository.save(movement);
        }
    }
}
