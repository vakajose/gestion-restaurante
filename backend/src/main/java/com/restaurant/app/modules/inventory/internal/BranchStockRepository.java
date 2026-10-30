package com.restaurant.app.modules.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BranchStockRepository extends JpaRepository<BranchStock, UUID> {

    Optional<BranchStock> findByTenantIdAndBranchIdAndIngredientId(UUID tenantId, UUID branchId, UUID ingredientId);

    Optional<BranchStock> findByTenantIdAndBranchIdAndDishId(UUID tenantId, UUID branchId, UUID dishId);

    List<BranchStock> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
}
