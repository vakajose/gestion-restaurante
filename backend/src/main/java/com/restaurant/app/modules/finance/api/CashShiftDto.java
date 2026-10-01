package com.restaurant.app.modules.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashShiftDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID openedBy,
    String openedByName,
    UUID closedBy,
    String closedByName,
    Instant openedAt,
    Instant closedAt,
    BigDecimal initialCash,
    BigDecimal totalCashSales,
    BigDecimal totalCardSales,
    BigDecimal totalOtherSales,
    BigDecimal totalCashExpenses,
    BigDecimal expectedCash,
    BigDecimal actualCash,
    BigDecimal difference,
    String status
) {}
