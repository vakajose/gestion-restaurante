package com.restaurant.app.core.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class BaseBranchEntity extends BaseTenantEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }
}
