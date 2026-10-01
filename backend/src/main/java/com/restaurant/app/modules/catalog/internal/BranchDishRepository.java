package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BranchDishRepository extends JpaRepository<BranchDish, UUID> {

    Optional<BranchDish> findByBranchIdAndDishId(UUID branchId, UUID dishId);

    List<BranchDish> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
}
