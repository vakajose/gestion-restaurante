package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class InventoryStrategyFactory {

    private final List<InventoryDeductionStrategy> strategies;

    InventoryStrategyFactory(List<InventoryDeductionStrategy> strategies) {
        this.strategies = strategies;
    }

    public InventoryDeductionStrategy getStrategy(InventoryStrategy strategy) {
        if (strategy == null) {
            strategy = InventoryStrategy.RECIPE_BOM_STRICT;
        }
        for (InventoryDeductionStrategy s : strategies) {
            if (s.supports(strategy)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Estrategia de inventario no soportada: " + strategy);
    }
}
