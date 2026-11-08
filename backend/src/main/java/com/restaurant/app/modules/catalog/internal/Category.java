package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "code_prefix", nullable = false, length = 10)
    private String codePrefix;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    Category() {
    }

    static String derivePrefix(String name) {
        if (name == null || name.isBlank()) {
            return "CAT";
        }
        String clean = name.trim().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() >= 3) {
            return clean.substring(0, 3);
        } else if (!clean.isEmpty()) {
            return clean;
        }
        return "CAT";
    }

    Category(UUID tenantId, String name, String icon, int sortOrder) {
        this(tenantId, name, icon, sortOrder, derivePrefix(name));
    }

    Category(UUID tenantId, String name, String icon, int sortOrder, String codePrefix) {
        this.tenantId = tenantId;
        this.name = name;
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.codePrefix = (codePrefix != null && !codePrefix.isBlank()) ? codePrefix.trim().toUpperCase() : derivePrefix(name);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCodePrefix() {
        return codePrefix;
    }

    public void setCodePrefix(String codePrefix) {
        this.codePrefix = codePrefix;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
