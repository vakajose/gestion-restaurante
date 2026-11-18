package com.restaurant.app.modules.catalog.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BranchServiceSessionDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    LocalDate serviceDate,
    String menuMode,
    UUID dailyMenuId,
    String dailyMenuName,
    String status,
    UUID configuredBy,
    String notes,
    Instant createdAt,
    Instant activatedAt,
    Instant closedAt,
    ShiftMenuDto shiftMenu
) {}
