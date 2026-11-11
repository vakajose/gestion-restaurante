package com.restaurant.app.modules.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface PreparedDishBufferRepository extends JpaRepository<PreparedDishBuffer, UUID> {

    List<PreparedDishBuffer> findByTenantIdAndBranchIdAndDishIdAndIsConsumedFalse(UUID tenantId, UUID branchId, UUID dishId);

    List<PreparedDishBuffer> findByTenantIdAndBranchIdAndIsConsumedFalse(UUID tenantId, UUID branchId);
}
