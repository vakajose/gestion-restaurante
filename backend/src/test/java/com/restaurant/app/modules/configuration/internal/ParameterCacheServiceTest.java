package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.modules.configuration.api.ParameterScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterCacheServiceTest {

    private ParameterCacheService cacheService;
    private UUID tenantId;
    private UUID branchId1;
    private UUID branchId2;

    @BeforeEach
    void setUp() {
        cacheService = new ParameterCacheService();
        tenantId = UUID.randomUUID();
        branchId1 = UUID.randomUUID();
        branchId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Put y Get concurrentes en memoria")
    void putAndGet() {
        cacheService.put(tenantId, branchId1, "pos.display_dish_images", "false");
        assertThat(cacheService.get(tenantId, branchId1, "pos.display_dish_images")).contains("false");
        assertThat(cacheService.get(tenantId, branchId2, "pos.display_dish_images")).isEmpty();
        assertThat(cacheService.get(tenantId, null, "pos.display_dish_images")).isEmpty();
    }

    @Test
    @DisplayName("Desalojo puntual para ámbito BRANCH")
    void evictBranchScope() {
        cacheService.put(tenantId, branchId1, "pos.display_dish_images", "false");
        cacheService.put(tenantId, branchId2, "pos.display_dish_images", "true");

        cacheService.evictForParameter("pos.display_dish_images", ParameterScope.BRANCH, tenantId, branchId1);

        assertThat(cacheService.get(tenantId, branchId1, "pos.display_dish_images")).isEmpty();
        assertThat(cacheService.get(tenantId, branchId2, "pos.display_dish_images")).contains("true");
    }

    @Test
    @DisplayName("Desalojo en cascada para ámbito TENANT desaloja todas las sucursales de ese tenant")
    void evictTenantScope() {
        cacheService.put(tenantId, branchId1, "pos.display_dish_images", "false");
        cacheService.put(tenantId, branchId2, "pos.display_dish_images", "true");
        cacheService.put(tenantId, null, "pos.display_dish_images", "true");

        UUID otherTenant = UUID.randomUUID();
        cacheService.put(otherTenant, branchId1, "pos.display_dish_images", "false");

        cacheService.evictForParameter("pos.display_dish_images", ParameterScope.TENANT, tenantId, null);

        assertThat(cacheService.get(tenantId, branchId1, "pos.display_dish_images")).isEmpty();
        assertThat(cacheService.get(tenantId, branchId2, "pos.display_dish_images")).isEmpty();
        assertThat(cacheService.get(tenantId, null, "pos.display_dish_images")).isEmpty();
        assertThat(cacheService.get(otherTenant, branchId1, "pos.display_dish_images")).contains("false");
    }

    @Test
    @DisplayName("Clear limpia completamente la caché")
    void clearCache() {
        cacheService.put(tenantId, branchId1, "k1", "v1");
        cacheService.put(tenantId, branchId2, "k2", "v2");
        assertThat(cacheService.size()).isEqualTo(2);

        cacheService.clear();
        assertThat(cacheService.size()).isEqualTo(0);
    }
}
