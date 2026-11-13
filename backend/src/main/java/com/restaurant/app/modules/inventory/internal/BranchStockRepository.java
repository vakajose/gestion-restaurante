package com.restaurant.app.modules.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BranchStockRepository extends JpaRepository<BranchStock, UUID> {

    Optional<BranchStock> findByTenantIdAndBranchIdAndIngredientId(UUID tenantId, UUID branchId, UUID ingredientId);

    Optional<BranchStock> findByTenantIdAndBranchIdAndDishId(UUID tenantId, UUID branchId, UUID dishId);

    List<BranchStock> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);

    List<BranchStock> findByTenantId(UUID tenantId);

    @Query("""
        SELECT COUNT(bs),
               COUNT(DISTINCT bs.ingredientId),
               COALESCE(SUM(bs.currentQuantity), 0),
               COALESCE(SUM(bs.currentQuantity * bs.averageUnitCost), 0)
        FROM BranchStock bs
        WHERE bs.tenantId = :tenantId
        """)
    List<Object[]> aggregateTenantStockMetrics(@Param("tenantId") UUID tenantId);
}
