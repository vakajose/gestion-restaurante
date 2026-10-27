package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ComboRepository extends JpaRepository<Combo, UUID> {

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.options WHERE c.tenantId = :tenantId AND (c.branchId IS NULL OR c.branchId = :branchId) AND c.isActive = true")
    List<Combo> findActiveCombosForBranch(@Param("tenantId") UUID tenantId, @Param("branchId") UUID branchId);

    @Query("SELECT DISTINCT c FROM Combo c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.options WHERE c.id = :id AND c.tenantId = :tenantId")
    Optional<Combo> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<Combo> findByTenantIdAndCode(UUID tenantId, String code);
}
