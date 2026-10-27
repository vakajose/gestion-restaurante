package com.restaurant.app.modules.catalog.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "modifier_groups")
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "min_selectable", nullable = false)
    private int minSelectable = 0;

    @Column(name = "max_selectable", nullable = false)
    private int maxSelectable = 1;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private Set<Modifier> modifiers = new LinkedHashSet<>();

    public ModifierGroup() {
    }

    public ModifierGroup(UUID tenantId, String name, int minSelectable, int maxSelectable, boolean isRequired) {
        this.tenantId = tenantId;
        this.name = name;
        this.minSelectable = minSelectable;
        this.maxSelectable = maxSelectable;
        this.isRequired = isRequired;
    }

    public void addModifier(Modifier mod) {
        modifiers.add(mod);
        mod.setModifierGroup(this);
    }

    public UUID getId() {
        return id;
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

    public int getMinSelectable() {
        return minSelectable;
    }

    public void setMinSelectable(int minSelectable) {
        this.minSelectable = minSelectable;
    }

    public int getMaxSelectable() {
        return maxSelectable;
    }

    public void setMaxSelectable(int maxSelectable) {
        this.maxSelectable = maxSelectable;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Set<Modifier> modifiers) {
        this.modifiers = modifiers;
    }
}
