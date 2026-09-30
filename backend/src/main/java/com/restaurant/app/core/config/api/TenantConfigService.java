package com.restaurant.app.core.config.api;

import java.util.UUID;

public interface TenantConfigService {

    String getString(UUID tenantId, String key, String defaultValue);

    boolean getBoolean(UUID tenantId, String key, boolean defaultValue);

    int getInt(UUID tenantId, String key, int defaultValue);

    double getDouble(UUID tenantId, String key, double defaultValue);

    void setSetting(UUID tenantId, String key, String value, String valueType, String description);
}
