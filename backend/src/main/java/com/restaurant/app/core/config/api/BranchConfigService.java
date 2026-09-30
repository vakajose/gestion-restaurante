package com.restaurant.app.core.config.api;

import java.util.UUID;

public interface BranchConfigService {

    String getString(UUID branchId, String key, String defaultValue);

    boolean getBoolean(UUID branchId, String key, boolean defaultValue);

    int getInt(UUID branchId, String key, int defaultValue);

    double getDouble(UUID branchId, String key, double defaultValue);

    void setSetting(UUID tenantId, UUID branchId, String key, String value, String valueType, String description);
}
