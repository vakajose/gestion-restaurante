package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.common.exception.PayloadTooLargeException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validador de seguridad para cargas multimedia: tamaño dinámico, extensión, MIME y magic bytes reales.
 */
final class MediaValidationSupport {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".webp", ".jpg", ".jpeg", ".png");
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
        ".webp", "image/webp",
        ".jpg", "image/jpeg",
        ".jpeg", "image/jpeg",
        ".png", "image/png"
    );

    record ValidatedMedia(byte[] bytes, String extension, String contentType) {
    }

    private MediaValidationSupport() {
    }

    static ValidatedMedia validateDishImage(MultipartFile file, long maxFileSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new PayloadTooLargeException(
                "El archivo supera el tamaño máximo permitido de " + formatMaxLimit(maxFileSizeBytes)
            );
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                "Extensión de imagen no permitida: " + extension + ". Extensiones permitidas: " + ALLOWED_EXTENSIONS
            );
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el contenido binario del archivo subido", e);
        }

        if (bytes.length > maxFileSizeBytes) {
            throw new PayloadTooLargeException(
                "El archivo supera el tamaño máximo permitido de " + formatMaxLimit(maxFileSizeBytes)
            );
        }

        validateMimeTypeIfPresent(file.getContentType(), extension);
        validateMagicBytes(bytes, extension);

        return new ValidatedMedia(bytes, extension, EXTENSION_TO_CONTENT_TYPE.get(extension));
    }

    private static void validateMimeTypeIfPresent(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        boolean validMime = switch (extension) {
            case ".webp" -> "image/webp".equals(normalized);
            case ".png" -> "image/png".equals(normalized);
            case ".jpg", ".jpeg" -> "image/jpeg".equals(normalized) || "image/jpg".equals(normalized);
            default -> false;
        };
        if (!validMime) {
            throw new IllegalArgumentException("Tipo MIME incompatible con la extensión " + extension + ": " + normalized);
        }
    }

    private static void validateMagicBytes(byte[] bytes, String extension) {
        boolean matches = switch (extension) {
            case ".webp" -> isWebp(bytes);
            case ".png" -> isPng(bytes);
            case ".jpg", ".jpeg" -> isJpeg(bytes);
            default -> false;
        };
        if (!matches) {
            throw new IllegalArgumentException(
                "La firma binaria (magic bytes) del archivo no corresponde a una imagen válida de tipo " + extension
            );
        }
    }

    private static boolean isWebp(byte[] b) {
        // RIFF....WEBP
        return b.length >= 12
            && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
            && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static boolean isPng(byte[] b) {
        // 89 50 4E 47 0D 0A 1A 0A
        return b.length >= 8
            && (b[0] & 0xFF) == 0x89
            && b[1] == 0x50
            && b[2] == 0x4E
            && b[3] == 0x47
            && b[4] == 0x0D
            && b[5] == 0x0A
            && b[6] == 0x1A
            && b[7] == 0x0A;
    }

    private static boolean isJpeg(byte[] b) {
        // FF D8 FF
        return b.length >= 3
            && (b[0] & 0xFF) == 0xFF
            && (b[1] & 0xFF) == 0xD8
            && (b[2] & 0xFF) == 0xFF;
    }

    private static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".webp";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private static String formatMaxLimit(long bytes) {
        if (bytes % (1024L * 1024L) == 0) {
            return (bytes / (1024L * 1024L)) + " MB";
        }
        return (bytes / 1024L) + " KB";
    }
}
