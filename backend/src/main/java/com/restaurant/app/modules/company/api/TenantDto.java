package com.restaurant.app.modules.company.api;

import com.restaurant.app.core.common.domain.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantDto(
    UUID id,
    String legalName,
    String nitOrTaxId,
    String slug,
    String logoUrl,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
    public TenantDto(
        UUID id,
        String legalName,
        String nitOrTaxId,
        String slug,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(id, legalName, nitOrTaxId, slug, null, isActive, createdAt, updatedAt);
    }

    public static TenantDto fromEntity(Tenant tenant) {
        return new TenantDto(
            tenant.getId(),
            tenant.getLegalName(),
            tenant.getNitOrTaxId(),
            tenant.getSlug(),
            tenant.getLogoUrl(),
            tenant.isActive(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }
}
