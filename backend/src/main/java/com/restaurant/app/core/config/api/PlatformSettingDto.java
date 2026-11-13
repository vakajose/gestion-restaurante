package com.restaurant.app.core.config.api;

import java.time.Instant;
import java.util.UUID;

public record PlatformSettingDto(
    String key,
    String value,
    String valueType,
    String description,
    Instant updatedAt,
    UUID updatedBy
) {
}
