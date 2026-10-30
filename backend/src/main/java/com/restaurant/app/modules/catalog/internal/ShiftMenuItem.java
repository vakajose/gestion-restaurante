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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shift_menu_items", uniqueConstraints = {
    @UniqueConstraint(name = "uq_shift_menu_item", columnNames = {"shift_menu_assignment_id", "dish_id"})
})
class ShiftMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_menu_assignment_id", nullable = false)
    private ShiftMenuAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType;

    @Column(name = "initial_quota", nullable = false)
    private int initialQuota;

    @Column(name = "current_quota", nullable = false)
    private int currentQuota;

    @Column(name = "sold_count", nullable = false)
    private int soldCount = 0;

    @Column(name = "effective_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal effectivePrice;

    @Column(name = "is_sold_out", nullable = false)
    private boolean isSoldOut = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    ShiftMenuItem() {
    }

    ShiftMenuItem(ShiftMenuAssignment assignment, Dish dish, CourseType courseType,
                  int initialQuota, int currentQuota, int soldCount,
                  BigDecimal effectivePrice, boolean isSoldOut) {
        this.assignment = assignment;
        this.dish = dish;
        this.courseType = courseType;
        this.initialQuota = initialQuota;
        this.currentQuota = currentQuota;
        this.soldCount = soldCount;
        this.effectivePrice = effectivePrice;
        this.isSoldOut = isSoldOut;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ShiftMenuAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(ShiftMenuAssignment assignment) {
        this.assignment = assignment;
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

    public int getInitialQuota() {
        return initialQuota;
    }

    public void setInitialQuota(int initialQuota) {
        this.initialQuota = initialQuota;
    }

    public int getCurrentQuota() {
        return currentQuota;
    }

    public void setCurrentQuota(int currentQuota) {
        this.currentQuota = currentQuota;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(BigDecimal effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public boolean isSoldOut() {
        return isSoldOut;
    }

    public void setSoldOut(boolean soldOut) {
        isSoldOut = soldOut;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
