package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {

    List<ModifierGroup> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<ModifierGroup> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT DISTINCT mg FROM ModifierGroup mg LEFT JOIN FETCH mg.modifiers WHERE mg.tenantId = :tenantId")
    List<ModifierGroup> findAllWithModifiersByTenantId(@Param("tenantId") UUID tenantId);
}
