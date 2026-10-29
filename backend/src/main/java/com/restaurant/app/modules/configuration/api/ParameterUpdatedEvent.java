package com.restaurant.app.modules.configuration.api;

import java.util.UUID;

public record ParameterUpdatedEvent(
    String parameterKey,
    ParameterScope scope,
    UUID tenantId,
    UUID branchId,
    String oldValue,
    String newValue
) {}
