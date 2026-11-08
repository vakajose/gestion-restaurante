package com.restaurant.app.modules.finance.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShiftAuthorizationDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID cashShiftId,
    String requestType,
    UUID referenceId,
    UUID requestedBy,
    String requestedByName,
    BigDecimal amount,
    String status,
    String justification,
    UUID decidedBy,
    String decidedByName,
    String decisionNotes,
    String decisionChannel,
    Instant createdAt,
    Instant decidedAt
) {}
