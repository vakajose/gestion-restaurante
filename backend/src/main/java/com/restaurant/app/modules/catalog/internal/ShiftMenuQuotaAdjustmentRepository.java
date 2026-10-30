package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ShiftMenuQuotaAdjustmentRepository extends JpaRepository<ShiftMenuQuotaAdjustment, UUID> {

    List<ShiftMenuQuotaAdjustment> findByShiftMenuItemIdOrderByCreatedAtDesc(UUID shiftMenuItemId);
}
