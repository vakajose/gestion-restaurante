package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.PayloadTooLargeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(
            tempDir.toString(),
            "/uploads/dishes/"
        );
    }

    @Test
    @DisplayName("Debe almacenar un archivo WebP válido y retornar su URL relativa")
    void shouldStoreValidWebpImage() {
        UUID dishId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "burger.webp",
            "image/webp",
            "fake-webp-bytes".getBytes()
        );

        String resultUrl = storageService.storeDishImage(dishId, file);

        assertThat(resultUrl).startsWith("/uploads/dishes/dish_" + dishId + "_");
        assertThat(resultUrl).endsWith(".webp");
    }

    @Test
    @DisplayName("Debe rechazar archivos que superen 1 MB con PayloadTooLargeException")
    void shouldRejectFilesExceedingOneMegabyte() {
        UUID dishId = UUID.randomUUID();
        byte[] largeBytes = new byte[(int) LocalFileStorageService.MAX_FILE_SIZE + 10];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "giant.webp",
            "image/webp",
            largeBytes
        );

        assertThatThrownBy(() -> storageService.storeDishImage(dishId, largeFile))
            .isInstanceOf(PayloadTooLargeException.class)
            .hasMessageContaining("1 MB");
    }

    @Test
    @DisplayName("Debe rechazar archivos con extensiones no permitidas")
    void shouldRejectDisallowedExtensions() {
        UUID dishId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "malware.exe",
            "application/octet-stream",
            "echo 1".getBytes()
        );

        assertThatThrownBy(() -> storageService.storeDishImage(dishId, file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Extensión de imagen no permitida");
    }

    @Test
    @DisplayName("Debe rechazar archivos vacíos")
    void shouldRejectEmptyFiles() {
        UUID dishId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.png",
            "image/png",
            new byte[0]
        );

        assertThatThrownBy(() -> storageService.storeDishImage(dishId, emptyFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no puede estar vacío");
    }
}
