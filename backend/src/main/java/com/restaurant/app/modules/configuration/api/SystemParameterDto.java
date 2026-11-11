package com.restaurant.app.modules.configuration.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SystemParameterDto(
    String key,
    String name,
    String description,
    ParameterCategory category,
    ParameterType type,
    String effectiveValue,
    String defaultValue,
    ParameterScope scope,
    ParameterScope inheritedFrom,
    boolean isOverridden,
    ParameterCriticality criticality,
    boolean requiresRestart,
    List<ParameterOptionDto> options,
    ParameterHelpDto help,
    Instant updatedAt,
    UUID updatedBy,
    String auditReason
) {}
