package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DailyMenuRepository extends JpaRepository<DailyMenu, UUID> {

    Optional<DailyMenu> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT m FROM DailyMenu m WHERE m.tenantId = :tenantId AND (:branchId IS NULL OR m.branchId IS NULL OR m.branchId = :branchId) AND (:templateOnly IS NULL OR :templateOnly = FALSE OR m.isTemplate = TRUE) AND m.isActive = TRUE ORDER BY m.name ASC")
    List<DailyMenu> findMenus(
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId,
        @Param("templateOnly") Boolean templateOnly
    );
}
