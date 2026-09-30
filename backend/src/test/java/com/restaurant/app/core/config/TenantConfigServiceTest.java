package com.restaurant.app.core.config;

import com.restaurant.app.core.config.api.BranchConfigService;
import com.restaurant.app.core.config.api.TenantConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantConfigServiceTest {

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private BranchConfigService branchConfigService;

    @Test
    @DisplayName("Lectura de settings de tenant con valores por defecto cuando no existen")
    void testTenantConfigDefaults() {
        UUID randomTenantId = UUID.randomUUID();

        assertThat(tenantConfigService.getString(randomTenantId, "UNKNOWN_KEY", "fallback")).isEqualTo("fallback");
        assertThat(tenantConfigService.getBoolean(randomTenantId, "UNKNOWN_KEY", true)).isTrue();
        assertThat(tenantConfigService.getInt(randomTenantId, "UNKNOWN_KEY", 42)).isEqualTo(42);
        assertThat(tenantConfigService.getDouble(randomTenantId, "UNKNOWN_KEY", 3.14)).isEqualTo(3.14);
    }

    @Test
    @DisplayName("Escritura y lectura tipada de configuraciones dinámicas de tenant")
    void testTenantConfigReadWrite() {
        UUID tenantId = UUID.randomUUID();

        tenantConfigService.setSetting(tenantId, "KEY_STR", "custom_value", "STRING", "test string");
        tenantConfigService.setSetting(tenantId, "KEY_BOOL", "true", "BOOLEAN", "test bool");
        tenantConfigService.setSetting(tenantId, "KEY_INT", "100", "NUMBER", "test int");

        assertThat(tenantConfigService.getString(tenantId, "KEY_STR", "def")).isEqualTo("custom_value");
        assertThat(tenantConfigService.getBoolean(tenantId, "KEY_BOOL", false)).isTrue();
        assertThat(tenantConfigService.getInt(tenantId, "KEY_INT", 0)).isEqualTo(100);
    }

    @Test
    @DisplayName("Lectura y escritura tipada de configuraciones de branch")
    void testBranchConfigReadWrite() {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        branchConfigService.setSetting(tenantId, branchId, "AUTO_PRINT", "true", "BOOLEAN", "Auto print tickets");
        assertThat(branchConfigService.getBoolean(branchId, "AUTO_PRINT", false)).isTrue();
        assertThat(branchConfigService.getString(branchId, "AUTO_PRINT", "none")).isEqualTo("true");
    }
}
