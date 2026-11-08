package com.restaurant.app.modules.finance.api;

import java.math.BigDecimal;
import java.util.List;

public record ShiftAuditDetailDto(
    CashShiftDto shift,
    BigDecimal cashSales,
    BigDecimal cardSales,
    BigDecimal qrSales,
    BigDecimal transferSales,
    List<OrderAuditSummaryDto> orders,
    List<ExpenseDto> expenses,
    List<OrderAuditSummaryDto> refunds
) {}
