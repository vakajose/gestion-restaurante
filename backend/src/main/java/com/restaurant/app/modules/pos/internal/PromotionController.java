package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.pos.api.CreatePromotionRequest;
import com.restaurant.app.modules.pos.api.PromotionDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionDto>> getPromotions(
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(promotionService.getPromotions(tenantId, activeOnly));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromotionDto>> getActivePromotions(
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "evalTime", required = false) Instant evalTime
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID effectiveBranchId = branchId != null ? branchId : TenantContextHolder.getBranchId();
        return ResponseEntity.ok(promotionService.getActivePromotionsForBranch(tenantId, effectiveBranchId, evalTime));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDto> getPromotionById(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(promotionService.findPromotionById(tenantId, id));
    }

    @PostMapping
    public ResponseEntity<PromotionDto> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        PromotionDto created = promotionService.createPromotion(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
