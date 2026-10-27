package com.restaurant.app.core.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_name", nullable = false, length = 150)
    private String legalName;

    @Column(name = "nit_or_tax_id", nullable = false, length = 50)
    private String nitOrTaxId;

    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Tenant() {
    }

    public Tenant(String legalName, String nitOrTaxId, String slug) {
        this.legalName = legalName;
        this.nitOrTaxId = nitOrTaxId;
        this.slug = (slug != null && !slug.isBlank()) ? slug : slugify(legalName);
        this.isActive = true;
    }

    public Tenant(String legalName, String nitOrTaxId) {
        this(legalName, nitOrTaxId, slugify(legalName));
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "tenant-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String s = normalized.replaceAll("\\p{M}", "")
            .toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return s.isBlank() ? "tenant-" + UUID.randomUUID().toString().substring(0, 8) : s;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getNitOrTaxId() {
        return nitOrTaxId;
    }

    public void setNitOrTaxId(String nitOrTaxId) {
        this.nitOrTaxId = nitOrTaxId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
