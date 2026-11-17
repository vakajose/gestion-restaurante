package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "daily_menu_combos")
class DailyMenuCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private DailyMenu menu;

    @Column(name = "combo_id", nullable = false)
    private UUID comboId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    DailyMenuCombo() {
    }

    DailyMenuCombo(DailyMenu menu, UUID comboId, int sortOrder) {
        this.menu = menu;
        this.comboId = comboId;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public DailyMenu getMenu() {
        return menu;
    }

    public void setMenu(DailyMenu menu) {
        this.menu = menu;
    }

    public UUID getComboId() {
        return comboId;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
