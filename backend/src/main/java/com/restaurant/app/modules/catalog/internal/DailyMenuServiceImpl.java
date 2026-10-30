package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ItemSoldOutException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.modules.catalog.api.AdjustQuotaRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuItemRequest;
import com.restaurant.app.modules.catalog.api.CreateDailyMenuRequest;
import com.restaurant.app.modules.catalog.api.DailyMenuDto;
import com.restaurant.app.modules.catalog.api.DailyMenuItemDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.ShiftMenuDto;
import com.restaurant.app.modules.catalog.api.ShiftMenuItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class DailyMenuServiceImpl implements DailyMenuPublicApi {

    private final DailyMenuRepository dailyMenuRepository;
    private final DishRepository dishRepository;
    private final ShiftMenuAssignmentRepository shiftMenuAssignmentRepository;
    private final ShiftMenuItemRepository shiftMenuItemRepository;
    private final ShiftMenuQuotaAdjustmentRepository shiftMenuQuotaAdjustmentRepository;

    DailyMenuServiceImpl(
        DailyMenuRepository dailyMenuRepository,
        DishRepository dishRepository,
        ShiftMenuAssignmentRepository shiftMenuAssignmentRepository,
        ShiftMenuItemRepository shiftMenuItemRepository,
        ShiftMenuQuotaAdjustmentRepository shiftMenuQuotaAdjustmentRepository
    ) {
        this.dailyMenuRepository = dailyMenuRepository;
        this.dishRepository = dishRepository;
        this.shiftMenuAssignmentRepository = shiftMenuAssignmentRepository;
        this.shiftMenuItemRepository = shiftMenuItemRepository;
        this.shiftMenuQuotaAdjustmentRepository = shiftMenuQuotaAdjustmentRepository;
    }

    @Override
    public DailyMenuDto createMenu(UUID tenantId, CreateDailyMenuRequest request) {
        DailyMenu menu = new DailyMenu(
            tenantId,
            request.branchId(),
            request.name().trim(),
            request.description(),
            request.dayOfWeek(),
            request.priceMode(),
            request.packagePrice(),
            request.isTemplate(),
            true
        );

        if (request.items() != null) {
            for (CreateDailyMenuItemRequest itemReq : request.items()) {
                Dish dish = dishRepository.findByIdAndTenantId(itemReq.dishId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Plato", itemReq.dishId()));

                DailyMenuItem item = new DailyMenuItem(
                    menu,
                    dish,
                    itemReq.courseType(),
                    itemReq.suggestedQuota(),
                    itemReq.overridePrice(),
                    itemReq.isMandatoryInPackage(),
                    itemReq.sortOrder()
                );
                menu.addItem(item);
            }
        }

        DailyMenu saved = dailyMenuRepository.save(menu);
        return toDailyMenuDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuDto> findMenus(UUID tenantId, UUID branchId, Boolean templateOnly) {
        return dailyMenuRepository.findMenus(tenantId, branchId, templateOnly).stream()
            .map(this::toDailyMenuDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DailyMenuDto getMenuById(UUID menuId) {
        return dailyMenuRepository.findById(menuId)
            .map(this::toDailyMenuDto)
            .orElseThrow(() -> new EntityNotFoundException("Menú Diario", menuId));
    }

    @Override
    public ShiftMenuDto instantiateTemplateForShift(
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        UUID menuTemplateId,
        Map<UUID, Integer> customQuotas,
        UUID assignedBy
    ) {
        Optional<ShiftMenuAssignment> existing = shiftMenuAssignmentRepository.findByCashShiftId(cashShiftId);
        if (existing.isPresent()) {
            throw new ResourceConflictException("El turno ya cuenta con un menú asignado.");
        }

        DailyMenu template = dailyMenuRepository.findById(menuTemplateId)
            .orElseThrow(() -> new EntityNotFoundException("Plantilla de Menú", menuTemplateId));

        ShiftMenuAssignment assignment = new ShiftMenuAssignment(
            tenantId,
            branchId,
            cashShiftId,
            template.getId(),
            assignedBy
        );

        for (DailyMenuItem templateItem : template.getItems()) {
            Dish dish = templateItem.getDish();
            if (!dish.isActive()) {
                continue;
            }

            int initialQuota = (customQuotas != null && customQuotas.containsKey(dish.getId()))
                ? customQuotas.get(dish.getId())
                : templateItem.getSuggestedQuota();

            BigDecimal effectivePrice = templateItem.getOverridePrice() != null
                ? templateItem.getOverridePrice()
                : dish.getSalePrice();

            ShiftMenuItem shiftItem = new ShiftMenuItem(
                assignment,
                dish,
                templateItem.getCourseType(),
                initialQuota,
                initialQuota,
                0,
                effectivePrice,
                initialQuota <= 0
            );
            assignment.addItem(shiftItem);
        }

        ShiftMenuAssignment saved = shiftMenuAssignmentRepository.save(assignment);
        return toShiftMenuDto(saved, template.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShiftMenuDto> getShiftMenu(UUID cashShiftId) {
        return shiftMenuAssignmentRepository.findByCashShiftId(cashShiftId)
            .map(assignment -> {
                String menuName = null;
                if (assignment.getMenuId() != null) {
                    menuName = dailyMenuRepository.findById(assignment.getMenuId())
                        .map(DailyMenu::getName)
                        .orElse(null);
                }
                return toShiftMenuDto(assignment, menuName);
            });
    }

    @Override
    public ShiftMenuItemDto adjustQuota(UUID shiftId, UUID itemId, int quantityDelta, String reason, UUID supervisorId) {
        ShiftMenuItem item = shiftMenuItemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú de Turno", itemId));

        int rows = shiftMenuItemRepository.adjustQuota(itemId, quantityDelta, item.getVersion());
        if (rows == 0) {
            throw new ResourceConflictException("Conflicto de concurrencia al actualizar cupos en el turno.");
        }

        ShiftMenuQuotaAdjustment adjustment = new ShiftMenuQuotaAdjustment(
            item,
            supervisorId,
            quantityDelta,
            reason
        );
        shiftMenuQuotaAdjustmentRepository.save(adjustment);

        ShiftMenuItem updated = shiftMenuItemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú de Turno", itemId));

        return toShiftMenuItemDto(updated);
    }

    @Override
    public void enforceAndDecrementQuota(UUID cashShiftId, UUID dishId, int quantity) {
        if (cashShiftId == null || dishId == null || quantity <= 0) {
            return;
        }

        Optional<ShiftMenuItem> shiftItemOpt = shiftMenuItemRepository.findByCashShiftIdAndDishId(cashShiftId, dishId);
        if (shiftItemOpt.isEmpty()) {
            // El plato no pertenece al menú diario del turno (carta abierta); no se aplica cupo de raciones.
            return;
        }

        ShiftMenuItem item = shiftItemOpt.get();
        if (item.getCurrentQuota() < quantity || item.isSoldOut()) {
            throw new ItemSoldOutException(
                "El plato '" + item.getDish().getName() + "' solo dispone de " +
                item.getCurrentQuota() + " raciones y se solicitaron " + quantity + "."
            );
        }

        int updatedRows = shiftMenuItemRepository.decrementQuota(item.getId(), quantity, item.getVersion());
        if (updatedRows == 0) {
            // Conflicto de versión u otro terminal consumió la última ración
            ShiftMenuItem reloaded = shiftMenuItemRepository.findById(item.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ítem de Menú", item.getId()));
            if (reloaded.getCurrentQuota() < quantity || reloaded.isSoldOut()) {
                throw new ItemSoldOutException(
                    "El plato '" + reloaded.getDish().getName() + "' solo dispone de " +
                    reloaded.getCurrentQuota() + " raciones y se solicitaron " + quantity + "."
                );
            }
            throw new ResourceConflictException("Conflicto concurrente al decrementar cupo para el plato: " + reloaded.getDish().getName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShiftMenuItemDto> findShiftMenuItem(UUID cashShiftId, UUID dishId) {
        return shiftMenuItemRepository.findByCashShiftIdAndDishId(cashShiftId, dishId)
            .map(this::toShiftMenuItemDto);
    }

    private DailyMenuDto toDailyMenuDto(DailyMenu menu) {
        List<DailyMenuItemDto> items = menu.getItems().stream()
            .sorted(Comparator.comparingInt(DailyMenuItem::getSortOrder))
            .map(i -> new DailyMenuItemDto(
                i.getId(),
                i.getDish().getId(),
                i.getDish().getName(),
                i.getDish().getCode(),
                i.getCourseType(),
                i.getSuggestedQuota(),
                i.getOverridePrice(),
                i.isMandatoryInPackage(),
                i.getSortOrder()
            ))
            .toList();

        return new DailyMenuDto(
            menu.getId(),
            menu.getTenantId(),
            menu.getBranchId(),
            menu.getName(),
            menu.getDescription(),
            menu.getDayOfWeek(),
            menu.getPriceMode(),
            menu.getPackagePrice(),
            menu.isTemplate(),
            menu.isActive(),
            menu.getCreatedAt(),
            menu.getUpdatedAt(),
            menu.getVersion(),
            items
        );
    }

    private ShiftMenuDto toShiftMenuDto(ShiftMenuAssignment assignment, String menuName) {
        List<ShiftMenuItemDto> items = assignment.getItems().stream()
            .map(this::toShiftMenuItemDto)
            .toList();

        return new ShiftMenuDto(
            assignment.getId(),
            assignment.getTenantId(),
            assignment.getBranchId(),
            assignment.getCashShiftId(),
            assignment.getMenuId(),
            menuName,
            assignment.getAssignedBy(),
            assignment.getAssignedAt(),
            items
        );
    }

    private ShiftMenuItemDto toShiftMenuItemDto(ShiftMenuItem item) {
        return new ShiftMenuItemDto(
            item.getId(),
            item.getAssignment().getId(),
            item.getDish().getId(),
            item.getDish().getName(),
            item.getDish().getCode(),
            item.getCourseType(),
            item.getInitialQuota(),
            item.getCurrentQuota(),
            item.getSoldCount(),
            item.getEffectivePrice(),
            item.isSoldOut(),
            item.getVersion()
        );
    }
}
