package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ModifierRepository extends JpaRepository<Modifier, UUID> {

    List<Modifier> findByTenantIdAndModifierGroupIdOrderBySortOrderAsc(UUID tenantId, UUID groupId);
}
