package com.restaurant.app.modules.inventory.internal;

enum InventoryDeductionMode {
    MANUAL_ISOLATED,
    AUTOMATIC_PERMISSIVE,
    AUTOMATIC_STRICT;

    public static InventoryDeductionMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return AUTOMATIC_PERMISSIVE;
        }
        try {
            return InventoryDeductionMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTOMATIC_PERMISSIVE;
        }
    }
}
