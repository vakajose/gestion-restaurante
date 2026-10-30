package com.restaurant.app.modules.catalog.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShiftMenuDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID cashShiftId,
    UUID menuId,
    String menuName,
    UUID assignedBy,
    Instant assignedAt,
    List<ShiftMenuItemDto> items
) {}
