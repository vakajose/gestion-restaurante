package com.restaurant.app.modules.company.api;

import com.restaurant.app.core.common.domain.Branch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BranchDto(
    UUID id,
    UUID tenantId,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String currency,
    String timezone,
    boolean isActive,
    Instant createdAt
) {
    public BranchDto(
        UUID id,
        UUID tenantId,
        String name,
        String address,
        String phone,
        String timezone,
        boolean isActive,
        Instant createdAt
    ) {
        this(id, tenantId, name, address, null, null, phone, "BOB", timezone, isActive, createdAt);
    }

    public static BranchDto fromEntity(Branch branch) {
        return new BranchDto(
            branch.getId(),
            branch.getTenantId(),
            branch.getName(),
            branch.getAddress(),
            branch.getLatitude(),
            branch.getLongitude(),
            branch.getPhone(),
            branch.getCurrency(),
            branch.getTimezone(),
            branch.isActive(),
            branch.getCreatedAt()
        );
    }
}
