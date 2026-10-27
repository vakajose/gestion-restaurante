package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "combo_items")
class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "selection_mode", nullable = false, length = 20)
    private String selectionMode = "FIXED";

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = true;

    @Column(name = "default_dish_id")
    private UUID defaultDishId;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @OneToMany(mappedBy = "comboItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ComboItemOption> options = new LinkedHashSet<>();

    ComboItem() {
    }

    ComboItem(String name, String selectionMode, boolean isRequired, UUID defaultDishId, int quantity, int sortOrder) {
        this.name = name;
        this.selectionMode = selectionMode != null ? selectionMode : "FIXED";
        this.isRequired = isRequired;
        this.defaultDishId = defaultDishId;
        this.quantity = quantity > 0 ? quantity : 1;
        this.sortOrder = sortOrder;
    }

    public void addOption(ComboItemOption option) {
        options.add(option);
        option.setComboItem(this);
    }

    public UUID getId() {
        return id;
    }

    public Combo getCombo() {
        return combo;
    }

    public void setCombo(Combo combo) {
        this.combo = combo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public UUID getDefaultDishId() {
        return defaultDishId;
    }

    public void setDefaultDishId(UUID defaultDishId) {
        this.defaultDishId = defaultDishId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Set<ComboItemOption> getOptions() {
        return options;
    }
}
