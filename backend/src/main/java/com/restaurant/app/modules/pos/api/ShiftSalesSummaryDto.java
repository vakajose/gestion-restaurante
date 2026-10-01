package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;

public record ShiftSalesSummaryDto(
    BigDecimal totalCashSales,
    BigDecimal totalCardSales,
    BigDecimal totalOtherSales,
    int totalOrders
) {}
