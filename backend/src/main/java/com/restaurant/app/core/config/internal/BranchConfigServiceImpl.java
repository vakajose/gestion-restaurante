package com.restaurant.app.core.config.internal;

import com.restaurant.app.core.config.BranchSetting;
import com.restaurant.app.core.config.BranchSettingRepository;
import com.restaurant.app.core.config.api.BranchConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class BranchConfigServiceImpl implements BranchConfigService {

    private static final Logger log = LoggerFactory.getLogger(BranchConfigServiceImpl.class);

    private final BranchSettingRepository repository;

    BranchConfigServiceImpl(BranchSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(UUID branchId, String key, String defaultValue) {
        if (branchId == null || key == null) {
            return defaultValue;
        }
        return repository.findByBranchIdAndSettingKey(branchId, key)
            .map(BranchSetting::getSettingValue)
            .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(UUID branchId, String key, boolean defaultValue) {
        String val = getString(branchId, key, null);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(UUID branchId, String key, int defaultValue) {
        String val = getString(branchId, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer setting '{}' for branch '{}': '{}'. Falling back to default: {}",
                key, branchId, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getDouble(UUID branchId, String key, double defaultValue) {
        String val = getString(branchId, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid double setting '{}' for branch '{}': '{}'. Falling back to default: {}",
                key, branchId, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public void setSetting(UUID tenantId, UUID branchId, String key, String value, String valueType, String description) {
        BranchSetting setting = repository.findByBranchIdAndSettingKey(branchId, key)
            .orElseGet(() -> new BranchSetting(tenantId, branchId, key, value, valueType, description));
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
