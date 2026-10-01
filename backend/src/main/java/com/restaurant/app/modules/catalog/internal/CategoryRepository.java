package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTenantIdOrderBySortOrderAscNameAsc(UUID tenantId);

    Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
