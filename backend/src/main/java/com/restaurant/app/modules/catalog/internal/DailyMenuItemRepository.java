package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DailyMenuItemRepository extends JpaRepository<DailyMenuItem, UUID> {

    List<DailyMenuItem> findByMenuIdOrderBySortOrderAsc(UUID menuId);
}
