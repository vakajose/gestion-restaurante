package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.config.api.PlatformSettingsService;
import com.restaurant.app.core.storage.api.StorageConnectionTestResult;
import com.restaurant.app.core.storage.api.StorageProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Component
class LocalStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageStrategy.class);

    private final PlatformSettingsService platformSettingsService;
    private final String localPath;
    private final String defaultPublicPrefix;

    LocalStorageStrategy(
        PlatformSettingsService platformSettingsService,
        @Value("${app.storage.local-path:./uploads/dishes/}") String localPath,
        @Value("${app.storage.public-url-prefix:/uploads/dishes/}") String defaultPublicPrefix
    ) {
        this.platformSettingsService = platformSettingsService;
        this.localPath = localPath;
        this.defaultPublicPrefix = defaultPublicPrefix;
    }

    @Override
    public StorageProviderType supports() {
        return StorageProviderType.LOCAL;
    }

    @Override
    public String store(UUID tenantId, UUID dishId, byte[] content, String extension, String contentType) {
        String fileName = "dish_" + dishId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        try {
            Path targetDir = Paths.get(localPath).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(fileName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new IllegalArgumentException("Ruta de almacenamiento local inválida");
            }
            Files.write(targetFile, content);

            String prefix = resolveLocalPrefix();
            return prefix + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Error al almacenar el archivo en disco: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank() || storedUrl.startsWith("http://") || storedUrl.startsWith("https://")) {
            return;
        }
        String prefix = resolveLocalPrefix();
        if (!storedUrl.startsWith(prefix)) {
            return;
        }
        String fileName = storedUrl.substring(prefix.length());
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        try {
            Path targetDir = Paths.get(localPath).toAbsolutePath().normalize();
            Path targetFile = targetDir.resolve(fileName).normalize();
            if (targetFile.startsWith(targetDir)) {
                Files.deleteIfExists(targetFile);
            }
        } catch (IOException ex) {
            log.warn("No se pudo eliminar imagen local obsoleta '{}': {}", storedUrl, ex.getMessage());
        }
    }

    @Override
    public StorageConnectionTestResult testConnection() {
        Instant checkedAt = Instant.now();
        try {
            Path targetDir = Paths.get(localPath).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            boolean writable = Files.isWritable(targetDir);
            return new StorageConnectionTestResult(
                StorageProviderType.LOCAL,
                writable,
                targetDir.toString(),
                writable
                    ? "Directorio local accesible y con permisos de escritura."
                    : "El directorio local existe pero no tiene permisos de escritura.",
                checkedAt
            );
        } catch (Exception ex) {
            return new StorageConnectionTestResult(
                StorageProviderType.LOCAL,
                false,
                localPath,
                "Fallo al verificar almacenamiento local: " + ex.getMessage(),
                checkedAt
            );
        }
    }

    private String resolveLocalPrefix() {
        String configured = platformSettingsService.getString(
            PlatformSettingsService.KEY_STORAGE_PUBLIC_URL_PREFIX,
            defaultPublicPrefix
        );
        String effective = (configured.startsWith("http://") || configured.startsWith("https://"))
            ? defaultPublicPrefix
            : configured;
        return effective.endsWith("/") ? effective : effective + "/";
    }
}
