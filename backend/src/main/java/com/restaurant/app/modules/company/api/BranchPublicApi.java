package com.restaurant.app.modules.company.api;

import java.util.List;
import java.util.UUID;

public interface BranchPublicApi {

    List<BranchDto> getBranchesByTenant(UUID tenantId);

    BranchDto getBranchById(UUID tenantId, UUID branchId);

    BranchDto createBranch(UUID tenantId, CreateBranchRequest request);

    BranchDto updateBranch(UUID tenantId, UUID branchId, UpdateBranchRequest request);

    BranchDto toggleBranchStatus(UUID tenantId, UUID branchId, boolean isActive);
}
