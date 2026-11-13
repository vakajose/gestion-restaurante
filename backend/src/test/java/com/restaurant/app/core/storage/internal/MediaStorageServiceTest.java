package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.common.exception.PayloadTooLargeException;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import com.restaurant.app.core.storage.api.MediaStorageService;
import com.restaurant.app.core.storage.api.StorageProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    private PlatformSettingsService platformSettingsService;
    private S3Client s3Client;
    private MediaStorageServiceImpl mediaStorageService;

    @BeforeEach
    void setUp() {
        platformSettingsService = mock(PlatformSettingsService.class);
        s3Client = mock(S3Client.class);

        when(platformSettingsService.getString(PlatformSettingsService.KEY_STORAGE_PROVIDER, "LOCAL"))
            .thenReturn("LOCAL");
        when(platformSettingsService.getLong(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, MediaStorageService.DEFAULT_MAX_FILE_SIZE_BYTES))
            .thenReturn(MediaStorageService.DEFAULT_MAX_FILE_SIZE_BYTES);
        when(platformSettingsService.getString(PlatformSettingsService.KEY_STORAGE_PUBLIC_URL_PREFIX, "/uploads/dishes/"))
            .thenReturn("/uploads/dishes/");
        when(platformSettingsService.getString(PlatformSettingsService.KEY_STORAGE_PUBLIC_URL_PREFIX, ""))
            .thenReturn("");

        LocalStorageStrategy localStrategy = new LocalStorageStrategy(
            platformSettingsService,
            tempDir.toString(),
            "/uploads/dishes/"
        );
        S3CompatibleStorageStrategy s3Strategy = new S3CompatibleStorageStrategy(
            platformSettingsService,
            s3Client,
            "restaurant-media-public",
            "https://media.restaurant.example.com"
        );

        mediaStorageService = new MediaStorageServiceImpl(
            platformSettingsService,
            List.of(localStrategy, s3Strategy),
            "LOCAL"
        );
    }

    @Test
    @DisplayName("Debe almacenar WebP, PNG y JPEG válidos con firma binaria real y eliminar archivo anterior")
    void shouldStoreValidImagesAndDeletePreviousLocalFile() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID dishId = UUID.randomUUID();

        byte[] webpBytes = new byte[] {'R', 'I', 'F', 'F', 0x10, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
        MockMultipartFile webpFile = new MockMultipartFile("file", "dish.webp", "image/webp", webpBytes);

        String url1 = mediaStorageService.storeDishImage(tenantId, dishId, webpFile);
        assertThat(url1).startsWith("/uploads/dishes/dish_" + dishId + "_").endsWith(".webp");
        String fileName1 = url1.replace("/uploads/dishes/", "");
        assertThat(Files.exists(tempDir.resolve(fileName1))).isTrue();

        byte[] pngBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        MockMultipartFile pngFile = new MockMultipartFile("file", "dish.png", "image/png", pngBytes);
        String url2 = mediaStorageService.storeDishImage(tenantId, dishId, pngFile);
        assertThat(url2).endsWith(".png");

        mediaStorageService.deleteByUrl(url1);
        assertThat(Files.exists(tempDir.resolve(fileName1))).isFalse();

        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MockMultipartFile jpegFile = new MockMultipartFile("file", "dish.jpg", "image/jpeg", jpegBytes);
        assertThat(mediaStorageService.storeDishImage(tenantId, dishId, jpegFile)).endsWith(".jpg");
    }

    @Test
    @DisplayName("Debe rechazar archivos disfrazados (magic bytes inválidos), extensiones prohibidas y archivos vacíos")
    void shouldRejectFakeMagicBytesAndDisallowedExtensions() {
        UUID tenantId = UUID.randomUUID();
        UUID dishId = UUID.randomUUID();

        MockMultipartFile fakeWebp = new MockMultipartFile(
            "file", "shell.webp", "image/webp", "#!/bin/bash\nrm -rf /".getBytes()
        );
        assertThatThrownBy(() -> mediaStorageService.storeDishImage(tenantId, dishId, fakeWebp))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("magic bytes");

        MockMultipartFile exeFile = new MockMultipartFile(
            "file", "malware.exe", "application/octet-stream", new byte[] {'M', 'Z', 0x00, 0x01}
        );
        assertThatThrownBy(() -> mediaStorageService.storeDishImage(tenantId, dishId, exeFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Extensión de imagen no permitida");

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> mediaStorageService.storeDishImage(tenantId, dishId, emptyFile))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Debe respetar el límite dinámico configurado en platform_settings sin reiniciar")
    void shouldEnforceDynamicMaxFileSizeFromPlatformSettings() {
        UUID tenantId = UUID.randomUUID();
        UUID dishId = UUID.randomUUID();

        byte[] payload1500Kb = new byte[1_500_000];
        payload1500Kb[0] = 'R';
        payload1500Kb[1] = 'I';
        payload1500Kb[2] = 'F';
        payload1500Kb[3] = 'F';
        payload1500Kb[8] = 'W';
        payload1500Kb[9] = 'E';
        payload1500Kb[10] = 'B';
        payload1500Kb[11] = 'P';
        MockMultipartFile file = new MockMultipartFile("file", "big.webp", "image/webp", payload1500Kb);

        // Con límite por defecto 1 MB falla con 413
        assertThatThrownBy(() -> mediaStorageService.storeDishImage(tenantId, dishId, file))
            .isInstanceOf(PayloadTooLargeException.class)
            .hasMessageContaining("1 MB");

        // Subiendo límite dinámico a 2 MB permite guardar de inmediato
        when(platformSettingsService.getLong(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, MediaStorageService.DEFAULT_MAX_FILE_SIZE_BYTES))
            .thenReturn(2_097_152L);

        String storedUrl = mediaStorageService.storeDishImage(tenantId, dishId, file);
        assertThat(storedUrl).startsWith("/uploads/dishes/");
    }

    @Test
    @DisplayName("Debe cambiar dinámicamente a S3/OCI Object Storage aislando claves por tenantId y limpiar objeto al borrar")
    void shouldDispatchToS3StrategyWithTenantIsolatedKeyAndSupportDelete() {
        UUID tenantId = UUID.randomUUID();
        UUID dishId = UUID.randomUUID();

        when(platformSettingsService.getString(PlatformSettingsService.KEY_STORAGE_PROVIDER, "LOCAL"))
            .thenReturn("S3");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        assertThat(mediaStorageService.getActiveProvider()).isEqualTo(StorageProviderType.S3);

        byte[] webpBytes = new byte[] {'R', 'I', 'F', 'F', 0x10, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
        MockMultipartFile file = new MockMultipartFile("file", "plato.webp", "image/webp", webpBytes);

        String url = mediaStorageService.storeDishImage(tenantId, dishId, file);

        assertThat(url).startsWith("https://media.restaurant.example.com/tenants/" + tenantId + "/dishes/dish_" + dishId + "_");
        assertThat(url).endsWith(".webp");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().bucket()).isEqualTo("restaurant-media-public");
        assertThat(putCaptor.getValue().contentType()).isEqualTo("image/webp");
        assertThat(putCaptor.getValue().key()).startsWith("tenants/" + tenantId + "/dishes/");

        mediaStorageService.deleteByUrl(url);
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().bucket()).isEqualTo("restaurant-media-public");
        assertThat(deleteCaptor.getValue().key()).isEqualTo(putCaptor.getValue().key());
    }
}
