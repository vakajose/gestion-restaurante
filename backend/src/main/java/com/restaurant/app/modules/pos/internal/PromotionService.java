package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.modules.pos.api.CreatePromotionRequest;
import com.restaurant.app.modules.pos.api.PromotionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final BranchRepository branchRepository;
    private final PromotionEngine promotionEngine;

    public PromotionService(PromotionRepository promotionRepository,
                            BranchRepository branchRepository,
                            PromotionEngine promotionEngine) {
        this.promotionRepository = promotionRepository;
        this.branchRepository = branchRepository;
        this.promotionEngine = promotionEngine;
    }

    @Transactional(readOnly = true)
    public List<PromotionDto> getPromotions(UUID tenantId, boolean activeOnly) {
        List<Promotion> list = activeOnly
                ? promotionRepository.findActiveWithBranchesByTenantId(tenantId)
                : promotionRepository.findAllWithBranchesByTenantId(tenantId);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionDto> getActivePromotionsForBranch(UUID tenantId, UUID branchId, Instant now) {
        String timezone = PromotionEngine.DEFAULT_TIMEZONE;
        if (branchId != null) {
            timezone = branchRepository.findById(branchId)
                    .map(Branch::getTimezone)
                    .orElse(PromotionEngine.DEFAULT_TIMEZONE);
        }

        Instant evalTime = now != null ? now : Instant.now();
        final String effectiveTimezone = timezone;

        List<Promotion> promotions = promotionRepository.findActiveWithBranchesByTenantId(tenantId);
        return promotions.stream()
                .filter(p -> promotionEngine.isPromotionActiveNow(p, evalTime, effectiveTimezone, branchId))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionDto findPromotionById(UUID tenantId, UUID id) {
        return promotionRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Promoción no encontrada: " + id));
    }

    public PromotionDto createPromotion(UUID tenantId, CreatePromotionRequest req) {
        Promotion promo = new Promotion(
                tenantId,
                req.name(),
                req.description(),
                req.discountType(),
                req.discountValue(),
                req.scope(),
                req.targetCategoryId(),
                req.targetDishId(),
                req.applicationMode(),
                req.daysOfWeek(),
                req.startTime(),
                req.endTime(),
                req.startDate(),
                req.endDate(),
                req.minOrderAmount(),
                true
        );

        if (req.branchIds() != null) {
            for (UUID branchId : req.branchIds()) {
                promo.addBranch(branchId);
            }
        }

        Promotion saved = promotionRepository.save(promo);
        return toDto(saved);
    }

    private PromotionDto toDto(Promotion p) {
        Set<UUID> branchIds = p.getBranches() != null
                ? p.getBranches().stream().map(PromotionBranch::getBranchId).collect(Collectors.toSet())
                : Set.of();

        return new PromotionDto(
                p.getId(),
                p.getTenantId(),
                p.getName(),
                p.getDescription(),
                p.getDiscountType(),
                p.getDiscountValue(),
                p.getScope(),
                p.getTargetCategoryId(),
                p.getTargetDishId(),
                p.getApplicationMode(),
                p.getDaysOfWeek(),
                p.getStartTime(),
                p.getEndTime(),
                p.getStartDate(),
                p.getEndDate(),
                p.getMinOrderAmount(),
                p.isActive(),
                branchIds
        );
    }
}
