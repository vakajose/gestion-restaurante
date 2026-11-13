package com.restaurant.app.modules.company.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchPublicApi {

    List<BranchDto> getBranchesByTenant(UUID tenantId);

    BranchDto getBranchById(UUID tenantId, UUID branchId);

    Optional<BranchDto> findBranchById(UUID branchId);

    Optional<BranchDto> findBranchById(UUID tenantId, UUID branchId);

    String getBranchTimezone(UUID branchId);

    BranchDto createBranch(UUID tenantId, CreateBranchRequest request);

    BranchDto updateBranch(UUID tenantId, UUID branchId, UpdateBranchRequest request);

    BranchDto toggleBranchStatus(UUID tenantId, UUID branchId, boolean isActive);

    long countBranchesByTenant(UUID tenantId, boolean activeOnly);
}
