package com.restaurant.app.modules.configuration.api;

import java.util.UUID;

public record UpdateParameterRequest(
    ParameterScope scope,
    UUID branchId,
    String value,
    String passwordConfirmation,
    String auditReason
) {}
