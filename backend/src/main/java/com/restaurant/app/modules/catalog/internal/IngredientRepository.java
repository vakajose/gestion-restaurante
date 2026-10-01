package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    List<Ingredient> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<Ingredient> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    List<Ingredient> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
}
