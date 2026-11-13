package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.config.api.PlatformSettingsService;
import com.restaurant.app.core.storage.api.MediaStorageService;
import com.restaurant.app.core.storage.api.StorageConnectionTestResult;
import com.restaurant.app.core.storage.api.StorageProviderType;
import com.restaurant.app.core.storage.internal.MediaValidationSupport.ValidatedMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
class MediaStorageServiceImpl implements MediaStorageService {

    private final PlatformSettingsService platformSettingsService;
    private final Map<StorageProviderType, StorageStrategy> strategies = new EnumMap<>(StorageProviderType.class);
    private final StorageProviderType defaultProvider;

    MediaStorageServiceImpl(
        PlatformSettingsService platformSettingsService,
        List<StorageStrategy> availableStrategies,
        @Value("${app.storage.type:LOCAL}") String defaultProviderName
    ) {
        this.platformSettingsService = platformSettingsService;
        for (StorageStrategy strategy : availableStrategies) {
            this.strategies.put(strategy.supports(), strategy);
        }
        this.defaultProvider = parseProvider(defaultProviderName, StorageProviderType.LOCAL);
    }

    @Override
    public String storeDishImage(UUID tenantId, UUID dishId, MultipartFile file) {
        long maxFileSizeBytes = getMaxFileSizeBytes();
        ValidatedMedia media = MediaValidationSupport.validateDishImage(file, maxFileSizeBytes);

        StorageProviderType activeProvider = getActiveProvider();
        StorageStrategy strategy = strategies.get(activeProvider);
        if (strategy == null) {
            throw new IllegalStateException("No existe una estrategia registrada para el proveedor: " + activeProvider);
        }

        return strategy.store(tenantId, dishId, media.bytes(), media.extension(), media.contentType());
    }

    @Override
    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            StorageStrategy s3 = strategies.get(StorageProviderType.S3);
            if (s3 != null) {
                s3.delete(imageUrl);
            }
            return;
        }
        StorageStrategy local = strategies.get(StorageProviderType.LOCAL);
        if (local != null) {
            local.delete(imageUrl);
        }
    }

    @Override
    public StorageProviderType getActiveProvider() {
        String configured = platformSettingsService.getString(
            PlatformSettingsService.KEY_STORAGE_PROVIDER,
            defaultProvider.name()
        );
        return parseProvider(configured, defaultProvider);
    }

    @Override
    public long getMaxFileSizeBytes() {
        long configured = platformSettingsService.getLong(
            PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES,
            DEFAULT_MAX_FILE_SIZE_BYTES
        );
        return configured > 0 ? configured : DEFAULT_MAX_FILE_SIZE_BYTES;
    }

    @Override
    public StorageConnectionTestResult testActiveConnection() {
        StorageProviderType activeProvider = getActiveProvider();
        StorageStrategy strategy = strategies.get(activeProvider);
        if (strategy == null) {
            return new StorageConnectionTestResult(
                activeProvider,
                false,
                activeProvider.name(),
                "No existe estrategia registrada para el proveedor activo " + activeProvider,
                java.time.Instant.now()
            );
        }
        return strategy.testConnection();
    }

    private StorageProviderType parseProvider(String raw, StorageProviderType fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return StorageProviderType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
