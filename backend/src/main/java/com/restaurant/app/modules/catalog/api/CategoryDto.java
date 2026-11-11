package com.restaurant.app.modules.catalog.api;

import java.time.Instant;
import java.util.UUID;

public record CategoryDto(
    UUID id,
    UUID tenantId,
    String name,
    String icon,
    int sortOrder,
    String codePrefix,
    Instant createdAt
) {
    public CategoryDto(UUID id, UUID tenantId, String name, String icon, int sortOrder, Instant createdAt) {
        this(id, tenantId, name, icon, sortOrder, "CAT", createdAt);
    }
}
