package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "branch_service_sessions")
class BranchServiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "menu_mode", nullable = false, length = 25)
    private String menuMode = "FULL_CATALOG";

    @Column(name = "daily_menu_id")
    private UUID dailyMenuId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "CONFIGURED";

    @Column(name = "configured_by")
    private UUID configuredBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    BranchServiceSession() {
    }

    BranchServiceSession(
        UUID tenantId,
        UUID branchId,
        LocalDate serviceDate,
        String menuMode,
        UUID dailyMenuId,
        String status,
        UUID configuredBy,
        String notes
    ) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.serviceDate = serviceDate;
        this.menuMode = menuMode != null ? menuMode : "FULL_CATALOG";
        this.dailyMenuId = dailyMenuId;
        this.status = status != null ? status : "CONFIGURED";
        this.configuredBy = configuredBy;
        this.notes = notes;
        if ("ACTIVE".equals(this.status)) {
            this.activatedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getMenuMode() {
        return menuMode;
    }

    public void setMenuMode(String menuMode) {
        this.menuMode = menuMode;
    }

    public UUID getDailyMenuId() {
        return dailyMenuId;
    }

    public void setDailyMenuId(UUID dailyMenuId) {
        this.dailyMenuId = dailyMenuId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getConfiguredBy() {
        return configuredBy;
    }

    public void setConfiguredBy(UUID configuredBy) {
        this.configuredBy = configuredBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Long getVersion() {
        return version;
    }
}
