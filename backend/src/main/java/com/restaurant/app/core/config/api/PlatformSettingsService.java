package com.restaurant.app.core.config.api;

import java.util.List;
import java.util.UUID;

/**
 * Contrato público para consultar y mutar configuraciones globales de plataforma (tabla platform_settings).
 */
public interface PlatformSettingsService {

    String KEY_STORAGE_PROVIDER = "storage.active_provider";
    String KEY_STORAGE_MAX_FILE_SIZE_BYTES = "storage.max_file_size_bytes";
    String KEY_STORAGE_PUBLIC_URL_PREFIX = "storage.public_url_prefix";
    String KEY_PLATFORM_PASSWORD_MIN_LENGTH = "security.platform_password_min_length";
    String KEY_PLATFORM_PASSWORD_REQUIRE_COMPLEXITY = "security.platform_password_require_complexity";

    List<PlatformSettingDto> findAll();

    String getString(String key, String defaultValue);

    long getLong(String key, long defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    PlatformSettingDto updateSetting(String key, String value, UUID platformAdminId);

    void invalidateCache();
}
