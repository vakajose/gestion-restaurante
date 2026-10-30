package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.modules.catalog.api.PriceMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "daily_menus")
class DailyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "day_of_week", length = 15)
    private String dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_mode", nullable = false, length = 25)
    private PriceMode priceMode = PriceMode.INDIVIDUAL_ITEMS;

    @Column(name = "package_price", precision = 12, scale = 2)
    private BigDecimal packagePrice;

    @Column(name = "is_template", nullable = false)
    private boolean isTemplate = true;

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

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<DailyMenuItem> items = new ArrayList<>();

    DailyMenu() {
    }

    DailyMenu(UUID tenantId, UUID branchId, String name, String description, String dayOfWeek,
              PriceMode priceMode, BigDecimal packagePrice, boolean isTemplate, boolean isActive) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.name = name;
        this.description = description;
        this.dayOfWeek = dayOfWeek;
        this.priceMode = priceMode != null ? priceMode : PriceMode.INDIVIDUAL_ITEMS;
        this.packagePrice = packagePrice;
        this.isTemplate = isTemplate;
        this.isActive = isActive;
    }

    public void addItem(DailyMenuItem item) {
        items.add(item);
        item.setMenu(this);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public PriceMode getPriceMode() {
        return priceMode;
    }

    public void setPriceMode(PriceMode priceMode) {
        this.priceMode = priceMode;
    }

    public BigDecimal getPackagePrice() {
        return packagePrice;
    }

    public void setPackagePrice(BigDecimal packagePrice) {
        this.packagePrice = packagePrice;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
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

    public List<DailyMenuItem> getItems() {
        return items;
    }

    public void setItems(List<DailyMenuItem> items) {
        this.items = items;
    }
}
