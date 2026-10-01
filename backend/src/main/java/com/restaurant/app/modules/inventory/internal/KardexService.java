package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.config.api.TenantConfigService;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.catalog.api.RecipeIngredientDto;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.inventory.api.StockAdjustmentRequest;
import com.restaurant.app.modules.inventory.api.StockBalanceDto;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
class KardexService implements InventoryPublicApi {

    private static final Logger log = LoggerFactory.getLogger(KardexService.class);

    private final BranchStockRepository branchStockRepository;
    private final KardexMovementRepository kardexMovementRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final TenantConfigService tenantConfigService;
    private final ManualIsolatedStrategy manualIsolatedStrategy;
    private final AutomaticPermissiveStrategy automaticPermissiveStrategy;
    private final AutomaticStrictStrategy automaticStrictStrategy;

    KardexService(
        BranchStockRepository branchStockRepository,
        KardexMovementRepository kardexMovementRepository,
        CatalogPublicApi catalogPublicApi,
        TenantConfigService tenantConfigService,
        ManualIsolatedStrategy manualIsolatedStrategy,
        AutomaticPermissiveStrategy automaticPermissiveStrategy,
        AutomaticStrictStrategy automaticStrictStrategy
    ) {
        this.branchStockRepository = branchStockRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.catalogPublicApi = catalogPublicApi;
        this.tenantConfigService = tenantConfigService;
        this.manualIsolatedStrategy = manualIsolatedStrategy;
        this.automaticPermissiveStrategy = automaticPermissiveStrategy;
        this.automaticStrictStrategy = automaticStrictStrategy;
    }

    public void processOrderDeduction(OrderPaidEvent event) {
        if (event == null || event.items() == null || event.items().isEmpty()) {
            return;
        }

        UUID tenantId = event.tenantId();
        UUID branchId = event.branchId();

        // 1. Agregar deducciones requeridas por ingrediente a partir de las recetas (BOM)
        Map<UUID, BigDecimal> neededQuantities = new HashMap<>();
        Map<UUID, String> ingredientNames = new HashMap<>();

        for (OrderItemSummary orderItem : event.items()) {
            Optional<DishRecipeDto> recipeOpt = catalogPublicApi.getRecipeForDish(tenantId, orderItem.dishId());
            if (recipeOpt.isEmpty() || recipeOpt.get().items() == null) {
                continue;
            }

            BigDecimal itemQty = BigDecimal.valueOf(orderItem.quantity());
            for (RecipeIngredientDto ing : recipeOpt.get().items()) {
                BigDecimal neededForDish = ing.quantity().multiply(itemQty);
                neededQuantities.merge(ing.ingredientId(), neededForDish, BigDecimal::add);
                ingredientNames.putIfAbsent(ing.ingredientId(), ing.ingredientName());
            }
        }

        if (neededQuantities.isEmpty()) {
            log.info("Orden {} ({}) no requiere deducción de insumos (sin recetas asociadas)",
                event.ticketNumber(), event.orderId());
            return;
        }

        List<RecipeDeductionItem> deductionItems = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> entry : neededQuantities.entrySet()) {
            String name = ingredientNames.getOrDefault(entry.getKey(), "Insumo");
            deductionItems.add(new RecipeDeductionItem(entry.getKey(), name, entry.getValue()));
        }

        // 2. Determinar la estrategia según la configuración del tenant
        String modeStr = tenantConfigService.getString(tenantId, "INVENTORY_DEDUCTION_MODE", "AUTOMATIC_PERMISSIVE");
        InventoryDeductionMode mode = InventoryDeductionMode.fromString(modeStr);

        DeductionStrategy strategy = switch (mode) {
            case MANUAL_ISOLATED -> manualIsolatedStrategy;
            case AUTOMATIC_STRICT -> automaticStrictStrategy;
            case AUTOMATIC_PERMISSIVE -> automaticPermissiveStrategy;
        };

        log.info("Ejecutando deducción de inventario para ticket {} usando estrategia {}", event.ticketNumber(), mode);
        strategy.executeDeduction(tenantId, branchId, event, deductionItems);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getAvailableStock(UUID tenantId, UUID branchId, UUID ingredientId) {
        return branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientId)
            .map(BranchStock::getCurrentQuantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBalanceDto> getBranchStockSummary(UUID tenantId, UUID branchId) {
        Map<UUID, IngredientDto> ingredientMap = catalogPublicApi.getAllIngredients(tenantId)
            .stream()
            .collect(Collectors.toMap(IngredientDto::id, i -> i, (a, b) -> a));

        Map<UUID, BranchStock> stockMap = branchStockRepository.findByTenantIdAndBranchId(tenantId, branchId)
            .stream()
            .collect(Collectors.toMap(BranchStock::getIngredientId, s -> s, (a, b) -> a));

        List<StockBalanceDto> summary = new ArrayList<>();
        for (IngredientDto ing : ingredientMap.values()) {
            BranchStock stock = stockMap.get(ing.id());
            BigDecimal qty = stock != null && stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
            BigDecimal cost = stock != null && stock.getAverageUnitCost() != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
            BigDecimal totalVal = qty.multiply(cost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal alert = ing.minStockAlert() != null ? ing.minStockAlert() : BigDecimal.ZERO;
            boolean isLow = qty.compareTo(alert) <= 0;

            summary.add(new StockBalanceDto(
                stock != null ? stock.getId() : null,
                ing.id(),
                ing.name(),
                ing.unitOfMeasure(),
                qty,
                cost,
                totalVal,
                alert,
                isLow,
                stock != null ? stock.getLastUpdated() : Instant.now()
            ));
        }

        return summary;
    }

    @Override
    public void registerInitialStock(UUID tenantId, UUID branchId, InitialStockRequest request) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, request.ingredientId())
            .orElseGet(() -> new BranchStock(tenantId, branchId, request.ingredientId(), BigDecimal.ZERO, BigDecimal.ZERO));

        stock.setCurrentQuantity(request.quantity());
        stock.setAverageUnitCost(request.unitCost());
        BranchStock saved = branchStockRepository.save(stock);

        BigDecimal totalCost = request.quantity().multiply(request.unitCost()).setScale(2, RoundingMode.HALF_UP);

        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            request.ingredientId(),
            "INITIAL_INVENTORY",
            request.quantity(),
            request.unitCost(),
            totalCost,
            request.quantity(),
            saved.getId(),
            Instant.now(),
            null
        );
        kardexMovementRepository.save(movement);
    }

    @Override
    public StockBalanceDto adjustStock(UUID tenantId, UUID branchId, StockAdjustmentRequest request, UUID userId) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, request.ingredientId())
            .orElseGet(() -> new BranchStock(tenantId, branchId, request.ingredientId(), BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimal previousQty = stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = request.newQuantity();
        BigDecimal diff = newQty.subtract(previousQty);

        BigDecimal unitCost = request.unitCost() != null ? request.unitCost()
            : (stock.getAverageUnitCost() != null ? stock.getAverageUnitCost() : BigDecimal.ZERO);

        stock.setCurrentQuantity(newQty);
        stock.setAverageUnitCost(unitCost);
        BranchStock saved = branchStockRepository.save(stock);

        BigDecimal totalCost = diff.abs().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            request.ingredientId(),
            "PHYSICAL_COUNT_ADJUSTMENT",
            diff,
            unitCost,
            totalCost,
            newQty,
            saved.getId(),
            Instant.now(),
            userId
        );
        kardexMovementRepository.save(movement);

        IngredientDto ing = catalogPublicApi.findIngredientById(request.ingredientId()).orElse(null);
        String name = ing != null ? ing.name() : "Insumo";
        String uom = ing != null ? ing.unitOfMeasure() : "UNIT";
        BigDecimal alert = ing != null && ing.minStockAlert() != null ? ing.minStockAlert() : BigDecimal.ZERO;

        return new StockBalanceDto(
            saved.getId(),
            saved.getIngredientId(),
            name,
            uom,
            newQty,
            unitCost,
            newQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP),
            alert,
            newQty.compareTo(alert) <= 0,
            saved.getLastUpdated()
        );
    }

    @Transactional(readOnly = true)
    public List<KardexMovementDto> getKardexMovements(UUID tenantId, UUID branchId, UUID ingredientId) {
        Map<UUID, IngredientDto> ingredientMap = catalogPublicApi.getAllIngredients(tenantId)
            .stream()
            .collect(Collectors.toMap(IngredientDto::id, i -> i, (a, b) -> a));

        List<KardexMovement> movements = (ingredientId != null)
            ? kardexMovementRepository.findByTenantIdAndBranchIdAndIngredientIdOrderByMovementDateDesc(tenantId, branchId, ingredientId)
            : kardexMovementRepository.findByTenantIdAndBranchIdOrderByMovementDateDesc(tenantId, branchId);

        return movements.stream()
            .map(m -> {
                IngredientDto ing = ingredientMap.get(m.getIngredientId());
                String name = ing != null ? ing.name() : "Desconocido";
                String uom = ing != null ? ing.unitOfMeasure() : "UNIT";
                return new KardexMovementDto(
                    m.getId(),
                    m.getIngredientId(),
                    name,
                    uom,
                    m.getMovementType(),
                    m.getQuantity(),
                    m.getUnitCost(),
                    m.getTotalCost(),
                    m.getBalanceQuantity(),
                    m.getReferenceId(),
                    m.getMovementDate(),
                    m.getCreatedBy()
                );
            })
            .toList();
    }
}
