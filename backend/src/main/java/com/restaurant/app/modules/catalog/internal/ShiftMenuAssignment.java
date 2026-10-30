package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shift_menu_assignments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_shift_menu_assignment", columnNames = {"cash_shift_id"})
})
class ShiftMenuAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "cash_shift_id", nullable = false)
    private UUID cashShiftId;

    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShiftMenuItem> items = new ArrayList<>();

    ShiftMenuAssignment() {
    }

    ShiftMenuAssignment(UUID tenantId, UUID branchId, UUID cashShiftId, UUID menuId, UUID assignedBy) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.cashShiftId = cashShiftId;
        this.menuId = menuId;
        this.assignedBy = assignedBy;
    }

    public void addItem(ShiftMenuItem item) {
        items.add(item);
        item.setAssignment(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getCashShiftId() {
        return cashShiftId;
    }

    public void setCashShiftId(UUID cashShiftId) {
        this.cashShiftId = cashShiftId;
    }

    public UUID getMenuId() {
        return menuId;
    }

    public void setMenuId(UUID menuId) {
        this.menuId = menuId;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public List<ShiftMenuItem> getItems() {
        return items;
    }

    public void setItems(List<ShiftMenuItem> items) {
        this.items = items;
    }
}
