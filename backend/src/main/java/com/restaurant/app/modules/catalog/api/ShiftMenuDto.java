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
    List<ShiftMenuItemDto> items,
    UUID serviceSessionId,
    List<ComboDto> combos
) {
    public ShiftMenuDto(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        UUID menuId,
        String menuName,
        UUID assignedBy,
        Instant assignedAt,
        List<ShiftMenuItemDto> items
    ) {
        this(id, tenantId, branchId, cashShiftId, menuId, menuName, assignedBy, assignedAt, items, null, List.of());
    }

    public ShiftMenuDto {
        items = items != null ? List.copyOf(items) : List.of();
        combos = combos != null ? List.copyOf(combos) : List.of();
    }
}
