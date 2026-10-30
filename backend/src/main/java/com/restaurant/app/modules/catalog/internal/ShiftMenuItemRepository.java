package com.restaurant.app.modules.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface ShiftMenuItemRepository extends JpaRepository<ShiftMenuItem, UUID> {

    Optional<ShiftMenuItem> findByAssignmentIdAndDishId(UUID assignmentId, UUID dishId);

    @Query("SELECT s FROM ShiftMenuItem s WHERE s.assignment.cashShiftId = :cashShiftId AND s.dish.id = :dishId")
    Optional<ShiftMenuItem> findByCashShiftIdAndDishId(@Param("cashShiftId") UUID cashShiftId, @Param("dishId") UUID dishId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ShiftMenuItem s SET s.currentQuota = s.currentQuota - :qty, s.soldCount = s.soldCount + :qty, s.isSoldOut = CASE WHEN (s.currentQuota - :qty) = 0 THEN TRUE ELSE FALSE END, s.version = s.version + 1 WHERE s.id = :id AND s.currentQuota >= :qty AND s.version = :version")
    int decrementQuota(
        @Param("id") UUID id,
        @Param("qty") int qty,
        @Param("version") Long version
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ShiftMenuItem s SET s.currentQuota = s.currentQuota + :delta, s.isSoldOut = CASE WHEN (s.currentQuota + :delta) <= 0 THEN TRUE ELSE FALSE END, s.version = s.version + 1 WHERE s.id = :id AND s.version = :version")
    int adjustQuota(
        @Param("id") UUID id,
        @Param("delta") int delta,
        @Param("version") Long version
    );
}
