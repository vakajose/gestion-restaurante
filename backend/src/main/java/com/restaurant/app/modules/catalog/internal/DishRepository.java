package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DishRepository extends JpaRepository<Dish, UUID> {

    List<Dish> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<Dish> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndBranchIdAndCode(UUID tenantId, UUID branchId, String code);

    boolean existsByTenantIdAndBranchIdIsNullAndCode(UUID tenantId, String code);

    @Query("SELECT d FROM Dish d WHERE d.tenantId = :tenantId AND (d.branchId IS NULL OR d.branchId = :branchId) AND d.isActive = true ORDER BY d.name ASC")
    List<Dish> findActiveDishesForBranch(@Param("tenantId") UUID tenantId, @Param("branchId") UUID branchId);
}
