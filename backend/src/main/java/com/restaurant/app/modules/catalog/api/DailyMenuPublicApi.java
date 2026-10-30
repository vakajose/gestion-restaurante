package com.restaurant.app.modules.catalog.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DailyMenuPublicApi {

    DailyMenuDto createMenu(UUID tenantId, CreateDailyMenuRequest request);

    List<DailyMenuDto> findMenus(UUID tenantId, UUID branchId, Boolean templateOnly);

    DailyMenuDto getMenuById(UUID menuId);

    ShiftMenuDto instantiateTemplateForShift(UUID tenantId, UUID branchId, UUID cashShiftId, UUID menuTemplateId, Map<UUID, Integer> customQuotas, UUID assignedBy);

    Optional<ShiftMenuDto> getShiftMenu(UUID cashShiftId);

    ShiftMenuItemDto adjustQuota(UUID shiftId, UUID itemId, int quantityDelta, String reason, UUID supervisorId);

    void enforceAndDecrementQuota(UUID cashShiftId, UUID dishId, int quantity);

    Optional<ShiftMenuItemDto> findShiftMenuItem(UUID cashShiftId, UUID dishId);
}
