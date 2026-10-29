package com.restaurant.app.modules.configuration.api;

import java.util.UUID;

public interface ConfigurationPublicApi {

    String getString(SystemParameter param, UUID branchId, UUID tenantId);

    boolean getBoolean(SystemParameter param, UUID branchId, UUID tenantId);

    int getInt(SystemParameter param, UUID branchId, UUID tenantId);

    double getDouble(SystemParameter param, UUID branchId, UUID tenantId);

    <E extends Enum<E>> E getEnum(SystemParameter param, UUID branchId, UUID tenantId, Class<E> enumClass);

    void setOverride(SystemParameter param, ParameterScope scope, UUID tenantId, UUID branchId, String value, UUID userId, String reason);

    void resetOverride(SystemParameter param, ParameterScope scope, UUID tenantId, UUID branchId);

    // Métodos de conveniencia por clave String
    String getString(String key, UUID branchId, UUID tenantId);

    boolean getBoolean(String key, UUID branchId, UUID tenantId);

    int getInt(String key, UUID branchId, UUID tenantId);

    double getDouble(String key, UUID branchId, UUID tenantId);

    void setOverride(String key, ParameterScope scope, UUID tenantId, UUID branchId, String value, UUID userId, String reason);

    void resetOverride(String key, ParameterScope scope, UUID tenantId, UUID branchId);
}
