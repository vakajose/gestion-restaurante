package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.common.exception.PayloadTooLargeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    public static final long MAX_FILE_SIZE = 1048576L; // 1 MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".webp", ".jpg", ".jpeg", ".png");

    private final String localPath;
    private final String publicUrlPrefix;

    public LocalFileStorageService(
            @Value("${app.storage.local-path:./uploads/dishes/}") String localPath,
            @Value("${app.storage.public-url-prefix:/uploads/dishes/}") String publicUrlPrefix
    ) {
        this.localPath = localPath;
        this.publicUrlPrefix = publicUrlPrefix;
    }

    public String storeDishImage(UUID dishId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new PayloadTooLargeException("El archivo supera el tamaño máximo permitido de 1 MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Extensión de imagen no permitida: " + extension + ". Extensiones permitidas: " + ALLOWED_EXTENSIONS);
        }

        String fileName = "dish_" + dishId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension.toLowerCase(Locale.ROOT);

        try {
            Path targetDir = Paths.get(localPath).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String prefix = publicUrlPrefix.endsWith("/") ? publicUrlPrefix : publicUrlPrefix + "/";
            return prefix + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Error al almacenar el archivo en disco: " + ex.getMessage(), ex);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".webp";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
