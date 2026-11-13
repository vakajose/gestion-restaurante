package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.TenantInventoryMetricsDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lector desacoplado de métricas estructurales y uso de cuotas por tenant vía APIs públicas (.api).
 */
@Component
class TenantUsageSnapshotReader {

    static final String DEFAULT_TIMEZONE = "America/La_Paz";

    private final BranchPublicApi branchPublicApi;
    private final UserPublicApi userPublicApi;
    private final CatalogPublicApi catalogPublicApi;
    private final InventoryPublicApi inventoryPublicApi;
    private final PosPublicApi posPublicApi;

    TenantUsageSnapshotReader(
        BranchPublicApi branchPublicApi,
        UserPublicApi userPublicApi,
        CatalogPublicApi catalogPublicApi,
        InventoryPublicApi inventoryPublicApi,
        PosPublicApi posPublicApi
    ) {
        this.branchPublicApi = branchPublicApi;
        this.userPublicApi = userPublicApi;
        this.catalogPublicApi = catalogPublicApi;
        this.inventoryPublicApi = inventoryPublicApi;
        this.posPublicApi = posPublicApi;
    }

    record TenantStructuralUsage(
        long activeBranches,
        long totalBranches,
        long activeUsers,
        long totalUsers,
        long activeDishes,
        long totalDishes,
        long totalIngredients,
        long monthlyOrders,
        long stockRecordsCount,
        long distinctStockedIngredients,
        BigDecimal totalInventoryUnits,
        BigDecimal totalInventoryValuation,
        List<BranchDto> branches,
        ZoneId primaryTimezone
    ) {
        Map<QuotaResource, Long> toQuotaCountsMap() {
            Map<QuotaResource, Long> map = new EnumMap<>(QuotaResource.class);
            map.put(QuotaResource.BRANCHES, activeBranches);
            map.put(QuotaResource.USERS, activeUsers);
            map.put(QuotaResource.DISHES, activeDishes);
            map.put(QuotaResource.INVENTORY_ITEMS, totalIngredients);
            map.put(QuotaResource.MONTHLY_ORDERS, monthlyOrders);
            return map;
        }
    }

    @Transactional(readOnly = true)
    public Map<QuotaResource, Long> loadQuotaUsageCounts(UUID tenantId) {
        List<BranchDto> branches = branchPublicApi.getBranchesByTenant(tenantId);
        ZoneId zoneId = resolvePrimaryZone(branches);

        long activeBranches = branchPublicApi.countBranchesByTenant(tenantId, true);
        long activeUsers = userPublicApi.countUsersByTenant(tenantId, true);
        long activeDishes = catalogPublicApi.countDishesByTenant(tenantId, true);
        long totalIngredients = catalogPublicApi.countIngredientsByTenant(tenantId);
        long monthlyOrders = countOrdersCurrentMonth(tenantId, zoneId);

        Map<QuotaResource, Long> map = new EnumMap<>(QuotaResource.class);
        map.put(QuotaResource.BRANCHES, activeBranches);
        map.put(QuotaResource.USERS, activeUsers);
        map.put(QuotaResource.DISHES, activeDishes);
        map.put(QuotaResource.INVENTORY_ITEMS, totalIngredients);
        map.put(QuotaResource.MONTHLY_ORDERS, monthlyOrders);
        return map;
    }

    @Transactional(readOnly = true)
    public TenantStructuralUsage readFullUsage(UUID tenantId) {
        List<BranchDto> branches = branchPublicApi.getBranchesByTenant(tenantId);
        ZoneId zoneId = resolvePrimaryZone(branches);

        long activeBranches = branchPublicApi.countBranchesByTenant(tenantId, true);
        long totalBranches = branchPublicApi.countBranchesByTenant(tenantId, false);
        long activeUsers = userPublicApi.countUsersByTenant(tenantId, true);
        long totalUsers = userPublicApi.countUsersByTenant(tenantId, false);
        long activeDishes = catalogPublicApi.countDishesByTenant(tenantId, true);
        long totalDishes = catalogPublicApi.countDishesByTenant(tenantId, false);
        long totalIngredients = catalogPublicApi.countIngredientsByTenant(tenantId);
        long monthlyOrders = countOrdersCurrentMonth(tenantId, zoneId);
        TenantInventoryMetricsDto inv = inventoryPublicApi.getTenantInventoryMetrics(tenantId);

        return new TenantStructuralUsage(
            activeBranches,
            totalBranches,
            activeUsers,
            totalUsers,
            activeDishes,
            totalDishes,
            totalIngredients,
            monthlyOrders,
            inv.stockRecordsCount(),
            inv.distinctIngredientsCount(),
            inv.totalUnitsOnHand(),
            inv.totalValuation(),
            branches,
            zoneId
        );
    }

    ZoneId resolveBranchZone(UUID branchId) {
        if (branchId == null) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        return parseZone(branchPublicApi.getBranchTimezone(branchId));
    }

    private long countOrdersCurrentMonth(UUID tenantId, ZoneId zoneId) {
        LocalDate today = LocalDate.now(zoneId);
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();
        Instant nextMonthStart = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zoneId).toInstant();
        return posPublicApi.countActiveOrdersInRange(tenantId, monthStart, nextMonthStart);
    }

    private ZoneId resolvePrimaryZone(List<BranchDto> branches) {
        if (branches == null || branches.isEmpty()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        return branches.stream()
            .filter(BranchDto::isActive)
            .map(BranchDto::timezone)
            .findFirst()
            .or(() -> branches.stream().map(BranchDto::timezone).findFirst())
            .map(this::parseZone)
            .orElse(ZoneId.of(DEFAULT_TIMEZONE));
    }

    private ZoneId parseZone(String tz) {
        if (tz == null || tz.isBlank()) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
        try {
            return ZoneId.of(tz);
        } catch (Exception ex) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }
}
