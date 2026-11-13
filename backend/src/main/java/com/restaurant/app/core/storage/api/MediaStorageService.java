package com.restaurant.app.core.storage.api;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Contrato público agnóstico de almacenamiento de medios (Local vs S3/OCI Object Storage).
 */
public interface MediaStorageService {

    long DEFAULT_MAX_FILE_SIZE_BYTES = 1_048_576L; // 1 MB

    String storeDishImage(UUID tenantId, UUID dishId, MultipartFile file);

    void deleteByUrl(String imageUrl);

    StorageProviderType getActiveProvider();

    long getMaxFileSizeBytes();

    /**
     * Prueba conectividad/escritura exclusivamente sobre el proveedor actualmente activo.
     */
    StorageConnectionTestResult testActiveConnection();
}
