package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.config.api.PlatformSettingsService;
import com.restaurant.app.core.storage.api.StorageProviderType;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

/**
 * Estrategia S3-Compatible (OCI Object Storage, MinIO, Cloudflare R2, AWS S3) sobre UrlConnectionHttpClient.
 */
@Component
class S3CompatibleStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleStorageStrategy.class);

    private final PlatformSettingsService platformSettingsService;
    private final String endpoint;
    private final String region;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final boolean pathStyleAccess;
    private final String publicBaseUrl;

    private volatile S3Client s3Client;
    private final boolean externallyProvidedClient;

    @Autowired
    S3CompatibleStorageStrategy(
        PlatformSettingsService platformSettingsService,
        @Value("${app.storage.s3.endpoint:}") String endpoint,
        @Value("${app.storage.s3.region:us-ashburn-1}") String region,
        @Value("${app.storage.s3.bucket:}") String bucket,
        @Value("${app.storage.s3.access-key:}") String accessKey,
        @Value("${app.storage.s3.secret-key:}") String secretKey,
        @Value("${app.storage.s3.path-style-access:true}") boolean pathStyleAccess,
        @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.platformSettingsService = platformSettingsService;
        this.endpoint = endpoint != null ? endpoint.trim() : "";
        this.region = region != null ? region.trim() : "us-ashburn-1";
        this.bucket = bucket != null ? bucket.trim() : "";
        this.accessKey = accessKey != null ? accessKey.trim() : "";
        this.secretKey = secretKey != null ? secretKey.trim() : "";
        this.pathStyleAccess = pathStyleAccess;
        this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.trim() : "";
        this.externallyProvidedClient = false;
    }

    S3CompatibleStorageStrategy(
        PlatformSettingsService platformSettingsService,
        S3Client s3Client,
        String bucket,
        String publicBaseUrl
    ) {
        this.platformSettingsService = platformSettingsService;
        this.s3Client = s3Client;
        this.endpoint = "https://mock-s3.local";
        this.region = "us-ashburn-1";
        this.bucket = bucket;
        this.accessKey = "test";
        this.secretKey = "test";
        this.pathStyleAccess = true;
        this.publicBaseUrl = publicBaseUrl;
        this.externallyProvidedClient = true;
    }

    @Override
    public StorageProviderType supports() {
        return StorageProviderType.S3;
    }

    @Override
    public String store(UUID tenantId, UUID dishId, byte[] content, String extension, String contentType) {
        validateS3Configuration();
        String key = buildObjectKey(tenantId, dishId, extension);

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength((long) content.length)
            .cacheControl("public, max-age=31536000, immutable")
            .build();

        getOrCreateClient().putObject(request, RequestBody.fromBytes(content));
        return resolvePublicBaseUrl() + "/" + key;
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) {
            return;
        }
        String base = resolvePublicBaseUrl();
        if (base.isBlank() || !storedUrl.startsWith(base + "/")) {
            return;
        }
        String key = storedUrl.substring(base.length() + 1);
        if (key.isBlank()) {
            return;
        }
        try {
            validateS3Configuration();
            getOrCreateClient().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception ex) {
            log.warn("No se pudo eliminar objeto S3 obsoleto '{}': {}", key, ex.getMessage());
        }
    }

    @PreDestroy
    void closeClient() {
        if (!externallyProvidedClient && s3Client != null) {
            s3Client.close();
        }
    }

    private String buildObjectKey(UUID tenantId, UUID dishId, String extension) {
        String tenantPrefix = tenantId != null ? "tenants/" + tenantId : "shared";
        return tenantPrefix + "/dishes/dish_" + dishId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }

    private String resolvePublicBaseUrl() {
        String configured = platformSettingsService.getString(PlatformSettingsService.KEY_STORAGE_PUBLIC_URL_PREFIX, "");
        String base;
        if (configured.startsWith("http://") || configured.startsWith("https://")) {
            base = configured;
        } else if (!publicBaseUrl.isBlank()) {
            base = publicBaseUrl;
        } else if (!endpoint.isBlank() && !bucket.isBlank()) {
            String cleanEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            base = cleanEndpoint + "/" + bucket;
        } else {
            base = "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private void validateS3Configuration() {
        if (bucket.isBlank()) {
            throw new IllegalStateException("S3_BUCKET no está configurado para el almacenamiento de objetos");
        }
        if (!externallyProvidedClient && (endpoint.isBlank() || accessKey.isBlank() || secretKey.isBlank())) {
            throw new IllegalStateException(
                "Faltan credenciales o endpoint S3 (S3_ENDPOINT, S3_ACCESS_KEY, S3_SECRET_KEY) para el proveedor S3/OCI"
            );
        }
    }

    private S3Client getOrCreateClient() {
        if (s3Client != null) {
            return s3Client;
        }
        synchronized (this) {
            if (s3Client == null) {
                s3Client = S3Client.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                    )
                    .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build())
                    .build();
            }
            return s3Client;
        }
    }
}
