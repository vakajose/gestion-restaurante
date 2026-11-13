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

    List<PlatformSettingDto> findAll();

    String getString(String key, String defaultValue);

    long getLong(String key, long defaultValue);

    PlatformSettingDto updateSetting(String key, String value, UUID platformAdminId);

    void invalidateCache();
}
