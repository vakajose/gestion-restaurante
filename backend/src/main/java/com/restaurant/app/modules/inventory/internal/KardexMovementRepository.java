package com.restaurant.app.modules.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface KardexMovementRepository extends JpaRepository<KardexMovement, UUID> {

    List<KardexMovement> findByTenantIdAndBranchIdOrderByMovementDateDesc(UUID tenantId, UUID branchId);

    List<KardexMovement> findByTenantIdAndBranchIdAndIngredientIdOrderByMovementDateDesc(UUID tenantId, UUID branchId, UUID ingredientId);
}
