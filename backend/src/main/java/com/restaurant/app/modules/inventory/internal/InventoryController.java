package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.inventory.api.InitialStockRequest;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.inventory.api.StockAdjustmentRequest;
import com.restaurant.app.modules.inventory.api.StockBalanceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController {

    private final KardexService kardexService;

    InventoryController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    private UUID resolveTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Contexto de tenant requerido");
        }
        return tenantId;
    }

    private UUID resolveBranchId(UUID paramBranchId) {
        if (paramBranchId != null) {
            return paramBranchId;
        }
        UUID contextBranchId = TenantContextHolder.getBranchId();
        if (contextBranchId != null) {
            return contextBranchId;
        }
        throw new IllegalArgumentException("Se requiere especificar branchId");
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockBalanceDto>> getStock(@RequestParam(value = "branchId", required = false) UUID branchId) {
        UUID tenantId = resolveTenantId();
        UUID effectiveBranchId = resolveBranchId(branchId);
        List<StockBalanceDto> stock = kardexService.getBranchStockSummary(tenantId, effectiveBranchId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/kardex")
    public ResponseEntity<List<KardexMovementDto>> getKardex(
        @RequestParam(value = "branchId", required = false) UUID branchId,
        @RequestParam(value = "ingredientId", required = false) UUID ingredientId
    ) {
        UUID tenantId = resolveTenantId();
        UUID effectiveBranchId = resolveBranchId(branchId);
        List<KardexMovementDto> movements = kardexService.getKardexMovements(tenantId, effectiveBranchId, ingredientId);
        return ResponseEntity.ok(movements);
    }

    @PostMapping("/stock/adjust")
    public ResponseEntity<StockBalanceDto> adjustStock(
        @RequestParam(value = "branchId", required = false) UUID branchId,
        @Valid @RequestBody StockAdjustmentRequest request
    ) {
        UUID tenantId = resolveTenantId();
        UUID effectiveBranchId = resolveBranchId(branchId);
        UUID userId = TenantContextHolder.getUserId();
        StockBalanceDto result = kardexService.adjustStock(tenantId, effectiveBranchId, request, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/stock/initial")
    public ResponseEntity<Void> registerInitialStock(
        @RequestParam(value = "branchId", required = false) UUID branchId,
        @Valid @RequestBody InitialStockRequest request
    ) {
        UUID tenantId = resolveTenantId();
        UUID effectiveBranchId = resolveBranchId(branchId);
        kardexService.registerInitialStock(tenantId, effectiveBranchId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
