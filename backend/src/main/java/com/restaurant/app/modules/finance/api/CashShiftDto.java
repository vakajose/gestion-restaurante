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
    BigDecimal totalCashRefunds,
    BigDecimal expectedCash,
    BigDecimal actualCash,
    BigDecimal difference,
    String status,
    UUID consolidatedBy,
    String consolidatedByName,
    Instant consolidatedAt,
    String consolidationNotes
) {
    public CashShiftDto(
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
        String status,
        UUID consolidatedBy,
        String consolidatedByName,
        Instant consolidatedAt,
        String consolidationNotes
    ) {
        this(
            id, tenantId, branchId, openedBy, openedByName, closedBy, closedByName,
            openedAt, closedAt, initialCash, totalCashSales, totalCardSales, totalOtherSales,
            totalCashExpenses, BigDecimal.ZERO, expectedCash, actualCash, difference,
            status, consolidatedBy, consolidatedByName, consolidatedAt, consolidationNotes
        );
    }
}
