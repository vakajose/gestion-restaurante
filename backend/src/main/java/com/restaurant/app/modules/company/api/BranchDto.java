package com.restaurant.app.modules.company.api;

import com.restaurant.app.core.common.domain.Branch;

import java.time.Instant;
import java.util.UUID;

public record BranchDto(
    UUID id,
    UUID tenantId,
    String name,
    String address,
    String phone,
    String timezone,
    boolean isActive,
    Instant createdAt
) {
    public static BranchDto fromEntity(Branch branch) {
        return new BranchDto(
            branch.getId(),
            branch.getTenantId(),
            branch.getName(),
            branch.getAddress(),
            branch.getPhone(),
            branch.getTimezone(),
            branch.isActive(),
            branch.getCreatedAt()
        );
    }
}
