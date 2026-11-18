package com.restaurant.app.modules.catalog.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DailyMenuPublicApi {

    DailyMenuDto createMenu(UUID tenantId, CreateDailyMenuRequest request);

    DailyMenuDto updateMenu(UUID tenantId, UUID menuId, CreateDailyMenuRequest request);

    void deleteMenu(UUID tenantId, UUID menuId);

    DailyMenuDto cloneMenu(UUID tenantId, UUID menuId, UUID targetBranchId, String newName);

    List<DailyMenuDto> findMenus(UUID tenantId, UUID branchId, Boolean templateOnly);

    DailyMenuDto getMenuById(UUID menuId);

    BranchServiceSessionDto configureServiceSession(UUID tenantId, UUID userId, ConfigureServiceSessionRequest request);

    Optional<BranchServiceSessionDto> getActiveServiceSession(UUID tenantId, UUID branchId);

    BranchServiceSessionDto attachCashShiftToServiceSession(
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        UUID menuTemplateId,
        Map<UUID, Integer> customQuotas,
        UUID openedBy,
        boolean allowAutoFullCatalog
    );

    BranchServiceSessionDto closeServiceSession(UUID tenantId, UUID sessionId);

    ShiftMenuDto instantiateTemplateForShift(UUID tenantId, UUID branchId, UUID cashShiftId, UUID menuTemplateId, Map<UUID, Integer> customQuotas, UUID assignedBy);

    Optional<ShiftMenuDto> getShiftMenu(UUID cashShiftId);

    ShiftMenuItemDto adjustQuota(UUID shiftId, UUID itemId, int quantityDelta, String reason, UUID supervisorId);

    void enforceAndDecrementQuota(UUID cashShiftId, UUID dishId, int quantity);

    default void enforceAndDecrementQuota(UUID branchId, UUID cashShiftId, UUID dishId, int quantity) {
        enforceAndDecrementQuota(cashShiftId, dishId, quantity);
    }

    Optional<ShiftMenuItemDto> findShiftMenuItem(UUID cashShiftId, UUID dishId);
}
