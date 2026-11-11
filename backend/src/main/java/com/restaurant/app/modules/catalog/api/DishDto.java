package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DishDto(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID clonedFromId,
    UUID categoryId,
    String categoryName,
    String code,
    String name,
    String description,
    BigDecimal salePrice,
    boolean isActive,
    Instant createdAt,
    Long version,
    InventoryStrategy inventoryStrategy,
    Boolean allowNegativeStockOverride,
    String imageUrl
) {
    public DishDto(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID clonedFromId,
        UUID categoryId,
        String categoryName,
        String code,
        String name,
        String description,
        BigDecimal salePrice,
        boolean isActive,
        Instant createdAt,
        Long version,
        InventoryStrategy inventoryStrategy,
        Boolean allowNegativeStockOverride
    ) {
        this(
            id,
            tenantId,
            branchId,
            clonedFromId,
            categoryId,
            categoryName,
            code,
            name,
            description,
            salePrice,
            isActive,
            createdAt,
            version,
            inventoryStrategy,
            allowNegativeStockOverride,
            null
        );
    }

    public DishDto(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID clonedFromId,
        UUID categoryId,
        String categoryName,
        String code,
        String name,
        String description,
        BigDecimal salePrice,
        boolean isActive,
        Instant createdAt,
        Long version
    ) {
        this(
            id,
            tenantId,
            branchId,
            clonedFromId,
            categoryId,
            categoryName,
            code,
            name,
            description,
            salePrice,
            isActive,
            createdAt,
            version,
            InventoryStrategy.RECIPE_BOM_STRICT,
            null,
            null
        );
    }
}
