package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.exception.InsufficientStockException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.config.api.TenantConfigService;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import com.restaurant.app.modules.catalog.api.InventoryStrategy;
import com.restaurant.app.modules.catalog.api.RecipeIngredientDto;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.inventory.api.StockAdjustmentRequest;
import com.restaurant.app.modules.inventory.api.StockBalanceDto;
import com.restaurant.app.modules.pos.api.ModifierItemSummary;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
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
    private final InventoryStrategyFactory inventoryStrategyFactory;
    private final ConfigurationPublicApi configurationPublicApi;
    private final PreparedDishBufferRepository preparedDishBufferRepository;

    KardexService(
        BranchStockRepository branchStockRepository,
        KardexMovementRepository kardexMovementRepository,
        CatalogPublicApi catalogPublicApi,
        TenantConfigService tenantConfigService,
        ManualIsolatedStrategy manualIsolatedStrategy,
        AutomaticPermissiveStrategy automaticPermissiveStrategy,
        AutomaticStrictStrategy automaticStrictStrategy,
        InventoryStrategyFactory inventoryStrategyFactory,
        ConfigurationPublicApi configurationPublicApi,
        PreparedDishBufferRepository preparedDishBufferRepository
    ) {
        this.branchStockRepository = branchStockRepository;
        this.kardexMovementRepository = kardexMovementRepository;
        this.catalogPublicApi = catalogPublicApi;
        this.tenantConfigService = tenantConfigService;
        this.manualIsolatedStrategy = manualIsolatedStrategy;
        this.automaticPermissiveStrategy = automaticPermissiveStrategy;
        this.automaticStrictStrategy = automaticStrictStrategy;
        this.inventoryStrategyFactory = inventoryStrategyFactory;
        this.configurationPublicApi = configurationPublicApi;
        this.preparedDishBufferRepository = preparedDishBufferRepository;
    }

    public void processOrderDeduction(OrderPaidEvent event) {
        if (event == null || event.items() == null || event.items().isEmpty()) {
            return;
        }

        UUID tenantId = event.tenantId();
        UUID branchId = event.branchId();

        // Si el tenant tiene configurado MANUAL_ISOLATED, omitir deducción automática de almacén
        String modeStr = tenantConfigService.getString(tenantId, "INVENTORY_DEDUCTION_MODE", "AUTOMATIC_PERMISSIVE");
        if ("MANUAL_ISOLATED".equalsIgnoreCase(modeStr)) {
            log.info("Deducción de inventario omitida para ticket {} debido a modo MANUAL_ISOLATED", event.ticketNumber());
            return;
        }

        boolean globalAllowNegative = "AUTOMATIC_PERMISSIVE".equalsIgnoreCase(modeStr);
        try {
            if (configurationPublicApi != null) {
                globalAllowNegative = configurationPublicApi.getBoolean(
                    SystemParameter.INVENTORY_ALLOW_NEGATIVE_STOCK,
                    branchId,
                    tenantId
                );
            }
        } catch (Exception ignored) {
            // Se mantiene el default derivado de modeStr
        }

        log.info("Procesando deducción polimórfica para ticket {} ({} ítems)...", event.ticketNumber(), event.items().size());
        for (OrderItemSummary orderItem : event.items()) {
            Optional<DishDto> dishOpt = catalogPublicApi.findDishById(orderItem.dishId());
            InventoryStrategy strategy = dishOpt.map(DishDto::inventoryStrategy)
                .orElse(InventoryStrategy.RECIPE_BOM_STRICT);

            boolean allowNegative = dishOpt.flatMap(d -> Optional.ofNullable(d.allowNegativeStockOverride()))
                .orElse(globalAllowNegative);

            InventoryDeductionStrategy deductionStrategy = inventoryStrategyFactory.getStrategy(strategy);
            deductionStrategy.deduct(tenantId, branchId, event.orderId(), orderItem, allowNegative);

            // Deducción atómica de modificadores / extras con insumo asociado (SALE_CONSUMPTION)
            if (orderItem.modifiers() != null && !orderItem.modifiers().isEmpty()) {
                BigDecimal orderQty = BigDecimal.valueOf(orderItem.quantity());
                for (ModifierItemSummary mod : orderItem.modifiers()) {
                    if (mod.ingredientId() != null && mod.quantityConsumed() != null && mod.quantityConsumed().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal neededQty = mod.quantityConsumed().multiply(orderQty);
                        deductModifierStock(tenantId, branchId, event.orderId(), mod.ingredientId(), mod.name(), neededQty, allowNegative);
                    }
                }
            }
        }
    }

    private void deductModifierStock(
        UUID tenantId,
        UUID branchId,
        UUID orderId,
        UUID ingredientId,
        String modifierName,
        BigDecimal neededQty,
        boolean allowNegative
    ) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientId)
            .orElse(null);

        BigDecimal currentQty = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;

        if (currentQty.compareTo(neededQty) < 0) {
            if (!allowNegative) {
                throw new InsufficientStockException(
                    "Stock insuficiente para el extra '" + modifierName +
                    "'. Requerido: " + neededQty + ", disponible: " + currentQty
                );
            }
            log.warn("STOCK_DEFICIT_WARNING: Insumo de modificador '{}' ({}) con déficit post-venta. Saldo previo: {}, requerido: {}",
                modifierName, ingredientId, currentQty, neededQty);
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
            "SALE_CONSUMPTION",
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

    public void processOrderCancellation(OrderCancelledEvent event) {
        if (event == null || event.items() == null || event.items().isEmpty()) {
            return;
        }

        UUID tenantId = event.tenantId();
        UUID branchId = event.branchId();
        UUID orderId = event.orderId();
        boolean wasPrepared = event.wasPrepared();
        String wastePolicy = event.wastePolicyApplied() != null ? event.wastePolicyApplied() : "ALLOW_AUTO_BUFFER_REUSE";

        log.info("Procesando cancelación de comanda {} en Kardex (wasPrepared={}, wastePolicy={})...",
            event.ticketNumber(), wasPrepared, wastePolicy);

        if (!wasPrepared) {
            // Caso 1: NO preparado en cocina -> Reincorporar insumos con CANCELLATION_REVERSAL
            for (OrderItemSummary orderItem : event.items()) {
                Optional<DishDto> dishOpt = catalogPublicApi.findDishById(orderItem.dishId());
                InventoryStrategy strategy = dishOpt.map(DishDto::inventoryStrategy)
                    .orElse(InventoryStrategy.RECIPE_BOM_STRICT);

                if (strategy == InventoryStrategy.COMMERCIAL_NO_INVENTORY) {
                    continue;
                }

                BigDecimal orderQty = BigDecimal.valueOf(orderItem.quantity());

                if (strategy == InventoryStrategy.RECIPE_BOM_STRICT) {
                    Optional<DishRecipeDto> recipeOpt = catalogPublicApi.getRecipeForDish(tenantId, orderItem.dishId());
                    if (recipeOpt.isPresent() && recipeOpt.get().items() != null) {
                        for (RecipeIngredientDto recipeItem : recipeOpt.get().items()) {
                            BigDecimal neededQty = recipeItem.quantity().multiply(orderQty);
                            restituteStock(tenantId, branchId, orderId, recipeItem.ingredientId(), neededQty);
                        }
                    }
                }

                // Reversión de modificadores con insumo
                if (orderItem.modifiers() != null && !orderItem.modifiers().isEmpty()) {
                    for (ModifierItemSummary mod : orderItem.modifiers()) {
                        if (mod.ingredientId() != null && mod.quantityConsumed() != null && mod.quantityConsumed().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal neededQty = mod.quantityConsumed().multiply(orderQty);
                            restituteStock(tenantId, branchId, orderId, mod.ingredientId(), neededQty);
                        }
                    }
                }
            }
        } else {
            // Caso 2: SÍ preparado en cocina
            if ("STRICT_WASTE".equalsIgnoreCase(wastePolicy)) {
                // Registrar WASTE_ADJUSTMENT para auditar la pérdida económica
                for (OrderItemSummary orderItem : event.items()) {
                    Optional<DishDto> dishOpt = catalogPublicApi.findDishById(orderItem.dishId());
                    InventoryStrategy strategy = dishOpt.map(DishDto::inventoryStrategy)
                        .orElse(InventoryStrategy.RECIPE_BOM_STRICT);

                    if (strategy == InventoryStrategy.COMMERCIAL_NO_INVENTORY) {
                        continue;
                    }

                    BigDecimal orderQty = BigDecimal.valueOf(orderItem.quantity());

                    if (strategy == InventoryStrategy.RECIPE_BOM_STRICT) {
                        Optional<DishRecipeDto> recipeOpt = catalogPublicApi.getRecipeForDish(tenantId, orderItem.dishId());
                        if (recipeOpt.isPresent() && recipeOpt.get().items() != null) {
                            for (RecipeIngredientDto recipeItem : recipeOpt.get().items()) {
                                BigDecimal neededQty = recipeItem.quantity().multiply(orderQty);
                                recordWasteMovement(tenantId, branchId, orderId, recipeItem.ingredientId(), neededQty);
                            }
                        }
                    }

                    if (orderItem.modifiers() != null && !orderItem.modifiers().isEmpty()) {
                        for (ModifierItemSummary mod : orderItem.modifiers()) {
                            if (mod.ingredientId() != null && mod.quantityConsumed() != null && mod.quantityConsumed().compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal neededQty = mod.quantityConsumed().multiply(orderQty);
                                recordWasteMovement(tenantId, branchId, orderId, mod.ingredientId(), neededQty);
                            }
                        }
                    }
                }
            } else {
                // Buffer de vitrina: Guardar en prepared_dish_buffer para despacho sin nuevo descuento
                for (OrderItemSummary orderItem : event.items()) {
                    PreparedDishBuffer bufferItem = new PreparedDishBuffer(
                        tenantId,
                        branchId,
                        orderId,
                        orderItem.dishId(),
                        orderItem.quantity(),
                        Instant.now().plus(java.time.Duration.ofHours(8)),
                        false
                    );
                    preparedDishBufferRepository.save(bufferItem);
                }
            }
        }
    }

    private void restituteStock(UUID tenantId, UUID branchId, UUID orderId, UUID ingredientId, BigDecimal qtyToRestitute) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientId)
            .orElse(null);

        BigDecimal currentQty = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
        BigDecimal unitCost = stock != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQty.add(qtyToRestitute);

        if (stock != null) {
            stock.setCurrentQuantity(newQuantity);
            branchStockRepository.save(stock);
        } else {
            stock = new BranchStock(tenantId, branchId, ingredientId, null, newQuantity, unitCost);
            branchStockRepository.save(stock);
        }

        BigDecimal totalCost = unitCost.multiply(qtyToRestitute).setScale(2, RoundingMode.HALF_UP);
        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            ingredientId,
            null,
            "CANCELLATION_REVERSAL",
            qtyToRestitute,
            unitCost,
            totalCost,
            newQuantity,
            orderId,
            Instant.now(),
            null
        );
        kardexMovementRepository.save(movement);
    }

    private void recordWasteMovement(UUID tenantId, UUID branchId, UUID orderId, UUID ingredientId, BigDecimal wasteQty) {
        BranchStock stock = branchStockRepository.findByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, ingredientId)
            .orElse(null);

        BigDecimal currentQty = stock != null ? stock.getCurrentQuantity() : BigDecimal.ZERO;
        BigDecimal unitCost = stock != null ? stock.getAverageUnitCost() : BigDecimal.ZERO;
        BigDecimal totalCost = unitCost.multiply(wasteQty).setScale(2, RoundingMode.HALF_UP);

        KardexMovement movement = new KardexMovement(
            tenantId,
            branchId,
            ingredientId,
            null,
            "WASTE_ADJUSTMENT",
            wasteQty,
            unitCost,
            totalCost,
            currentQty,
            orderId,
            Instant.now(),
            null
        );
        kardexMovementRepository.save(movement);
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
        if (kardexMovementRepository.existsByTenantIdAndBranchIdAndIngredientId(tenantId, branchId, request.ingredientId())) {
            throw new ResourceConflictException("El insumo ya cuenta con movimientos registrados en esta sucursal. Para modificar existencias utilice Recepción de Compras o Ajuste de Inventario.");
        }

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
            "INITIAL_STOCK",
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
