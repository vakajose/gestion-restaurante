package com.restaurant.app.core.common.repository;

import com.restaurant.app.core.common.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByTenantId(UUID tenantId);
    Optional<Branch> findByTenantIdAndName(UUID tenantId, String name);
}
