package com.restaurant.app.modules.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID cashShiftId,
    String expenseType,
    String description,
    BigDecimal amount,
    boolean paidFromCashDrawer,
    String approvalStatus,
    UUID approvedBy,
    String approvedByName,
    String evidenceUrl,
    String approvalNotes,
    LocalDate expenseDate,
    UUID createdBy,
    String createdByName,
    Instant createdAt
) {}
