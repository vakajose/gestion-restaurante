package com.restaurant.app.modules.company.api;

import com.restaurant.app.core.common.domain.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDto(
    UUID id,
    String legalName,
    String nitOrTaxId,
    String slug,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public static TenantDto fromEntity(Tenant tenant) {
        return new TenantDto(
            tenant.getId(),
            tenant.getLegalName(),
            tenant.getNitOrTaxId(),
            tenant.getSlug(),
            tenant.isActive(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }
}
