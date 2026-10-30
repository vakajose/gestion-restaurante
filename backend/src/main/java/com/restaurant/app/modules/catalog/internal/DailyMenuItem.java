package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.modules.catalog.api.CourseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "daily_menu_items")
class DailyMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private DailyMenu menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType;

    @Column(name = "suggested_quota", nullable = false)
    private int suggestedQuota = 0;

    @Column(name = "override_price", precision = 12, scale = 2)
    private BigDecimal overridePrice;

    @Column(name = "is_mandatory_in_package", nullable = false)
    private boolean isMandatoryInPackage = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    DailyMenuItem() {
    }

    DailyMenuItem(DailyMenu menu, Dish dish, CourseType courseType, int suggestedQuota,
                  BigDecimal overridePrice, boolean isMandatoryInPackage, int sortOrder) {
        this.menu = menu;
        this.dish = dish;
        this.courseType = courseType;
        this.suggestedQuota = suggestedQuota;
        this.overridePrice = overridePrice;
        this.isMandatoryInPackage = isMandatoryInPackage;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DailyMenu getMenu() {
        return menu;
    }

    public void setMenu(DailyMenu menu) {
        this.menu = menu;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public int getSuggestedQuota() {
        return suggestedQuota;
    }

    public void setSuggestedQuota(int suggestedQuota) {
        this.suggestedQuota = suggestedQuota;
    }

    public BigDecimal getOverridePrice() {
        return overridePrice;
    }

    public void setOverridePrice(BigDecimal overridePrice) {
        this.overridePrice = overridePrice;
    }

    public boolean isMandatoryInPackage() {
        return isMandatoryInPackage;
    }

    public void setMandatoryInPackage(boolean mandatoryInPackage) {
        isMandatoryInPackage = mandatoryInPackage;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
