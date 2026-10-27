package com.restaurant.app.modules.pos.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    List<Promotion> findByTenantId(UUID tenantId);

    Optional<Promotion> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT DISTINCT p FROM Promotion p LEFT JOIN FETCH p.branches WHERE p.tenantId = :tenantId AND p.isActive = true")
    List<Promotion> findActiveWithBranchesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT p FROM Promotion p LEFT JOIN FETCH p.branches WHERE p.tenantId = :tenantId")
    List<Promotion> findAllWithBranchesByTenantId(@Param("tenantId") UUID tenantId);
}
