package com.restaurant.app.modules.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderAuditSummaryDto(
    UUID id,
    String ticketNumber,
    Instant createdAt,
    String customerName,
    String paymentMethod,
    BigDecimal totalAmount,
    String orderStatus,
    String orderType,
    String cancellationReason
) {}
