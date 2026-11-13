package com.restaurant.app.core.entitlements.api;

public enum QuotaResource {
    BRANCHES("Sucursales activas"),
    USERS("Usuarios activos"),
    DISHES("Platos del catálogo"),
    INVENTORY_ITEMS("Insumos / ítems de inventario"),
    MONTHLY_ORDERS("Órdenes mensuales");

    private final String displayName;

    QuotaResource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
