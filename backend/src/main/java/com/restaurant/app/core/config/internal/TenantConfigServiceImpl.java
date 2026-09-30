package com.restaurant.app.core.config.internal;

import com.restaurant.app.core.config.TenantSetting;
import com.restaurant.app.core.config.TenantSettingRepository;
import com.restaurant.app.core.config.api.TenantConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class TenantConfigServiceImpl implements TenantConfigService {

    private static final Logger log = LoggerFactory.getLogger(TenantConfigServiceImpl.class);

    private final TenantSettingRepository repository;

    TenantConfigServiceImpl(TenantSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(UUID tenantId, String key, String defaultValue) {
        if (tenantId == null || key == null) {
            return defaultValue;
        }
        return repository.findByTenantIdAndSettingKey(tenantId, key)
            .map(TenantSetting::getSettingValue)
            .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(UUID tenantId, String key, boolean defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(UUID tenantId, String key, int defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer setting '{}' for tenant '{}': '{}'. Falling back to default: {}",
                key, tenantId, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getDouble(UUID tenantId, String key, double defaultValue) {
        String val = getString(tenantId, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid double setting '{}' for tenant '{}': '{}'. Falling back to default: {}",
                key, tenantId, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public void setSetting(UUID tenantId, String key, String value, String valueType, String description) {
        TenantSetting setting = repository.findByTenantIdAndSettingKey(tenantId, key)
            .orElseGet(() -> new TenantSetting(tenantId, key, value, valueType, description));
        setting.setSettingValue(value);
        if (valueType != null) {
            setting.setValueType(valueType);
        }
        if (description != null) {
            setting.setDescription(description);
        }
        repository.save(setting);
    }
}
