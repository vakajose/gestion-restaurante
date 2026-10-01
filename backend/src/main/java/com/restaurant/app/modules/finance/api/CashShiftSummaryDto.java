package com.restaurant.app.modules.finance.api;

public record CashShiftSummaryDto(
    CashShiftDto shift,
    int totalOrders,
    int approvedExpensesCount,
    int pendingExpensesCount
) {}
