package com.restaurant.app.core.config.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.config.PlatformSetting;
import com.restaurant.app.core.config.PlatformSettingRepository;
import com.restaurant.app.core.config.api.PlatformSettingDto;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
class PlatformSettingsServiceImpl implements PlatformSettingsService {

    private static final long MIN_UPLOAD_BYTES = 102_400L;     // 100 KB
    private static final long MAX_UPLOAD_BYTES = 10_485_760L;  // 10 MB (techo servlet Tomcat)
    private static final long MIN_PLATFORM_PASSWORD_LEN = 6L;
    private static final long MAX_PLATFORM_PASSWORD_LEN = 64L;

    private final PlatformSettingRepository repository;
    private final Map<String, PlatformSettingDto> cache = new ConcurrentHashMap<>();

    PlatformSettingsServiceImpl(PlatformSettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformSettingDto> findAll() {
        return repository.findAll().stream()
            .map(this::toDto)
            .sorted(Comparator.comparing(PlatformSettingDto::key))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(String key, String defaultValue) {
        Optional<PlatformSettingDto> resolved = resolve(key);
        if (resolved.isEmpty() || resolved.get().value() == null || resolved.get().value().isBlank()) {
            return defaultValue;
        }
        return resolved.get().value().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public long getLong(String key, long defaultValue) {
        Optional<PlatformSettingDto> resolved = resolve(key);
        if (resolved.isEmpty() || resolved.get().value() == null || resolved.get().value().isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(resolved.get().value().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean defaultValue) {
        Optional<PlatformSettingDto> resolved = resolve(key);
        if (resolved.isEmpty() || resolved.get().value() == null || resolved.get().value().isBlank()) {
            return defaultValue;
        }
        String normalized = resolved.get().value().trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    @Override
    @Transactional
    public PlatformSettingDto updateSetting(String key, String rawValue, UUID platformAdminId) {
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException("La clave de configuración no puede estar vacía");
        }
        String normalizedKey = key.trim();
        String value = rawValue != null ? rawValue.trim() : "";

        PlatformSetting entity = repository.findById(normalizedKey)
            .orElseGet(() -> createDefaultEntityIfKnown(normalizedKey, platformAdminId));

        validateValue(normalizedKey, entity.getValueType(), value);

        entity.setSettingValue(value);
        entity.setUpdatedBy(platformAdminId);
        PlatformSetting saved = repository.save(entity);

        PlatformSettingDto dto = toDto(saved);
        cache.put(normalizedKey, dto);
        return dto;
    }

    @Override
    public void invalidateCache() {
        cache.clear();
    }

    private Optional<PlatformSettingDto> resolve(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalizedKey = key.trim();
        PlatformSettingDto cached = cache.get(normalizedKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<PlatformSettingDto> fromDb = repository.findById(normalizedKey).map(this::toDto);
        fromDb.ifPresent(dto -> cache.put(normalizedKey, dto));
        return fromDb;
    }

    private PlatformSetting createDefaultEntityIfKnown(String key, UUID adminId) {
        return switch (key) {
            case KEY_STORAGE_PROVIDER -> new PlatformSetting(key, "LOCAL", "STRING", "Proveedor activo de almacenamiento de medios (LOCAL o S3)", adminId);
            case KEY_STORAGE_MAX_FILE_SIZE_BYTES -> new PlatformSetting(key, "1048576", "NUMBER", "Tamaño máximo permitido por imagen subida (bytes)", adminId);
            case KEY_STORAGE_PUBLIC_URL_PREFIX -> new PlatformSetting(key, "/uploads/dishes/", "STRING", "Prefijo público base para construir las URLs de imágenes", adminId);
            case KEY_PLATFORM_PASSWORD_MIN_LENGTH -> new PlatformSetting(key, "12", "NUMBER", "Longitud mínima de contraseña para administradores de plataforma (entre 6 y 64 caracteres)", adminId);
            case KEY_PLATFORM_PASSWORD_REQUIRE_COMPLEXITY -> new PlatformSetting(key, "false", "BOOLEAN", "Exigir al menos una letra mayúscula, un dígito y un símbolo en contraseñas de plataforma", adminId);
            default -> throw new BusinessRuleException("Parámetro de plataforma desconocido: " + key);
        };
    }

    private void validateValue(String key, String valueType, String value) {
        if (value.isBlank()) {
            throw new BusinessRuleException("El valor del parámetro '" + key + "' no puede estar vacío");
        }
        if ("NUMBER".equalsIgnoreCase(valueType)) {
            long parsed;
            try {
                parsed = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("El parámetro '" + key + "' requiere un valor numérico entero válido");
            }
            if (KEY_STORAGE_MAX_FILE_SIZE_BYTES.equals(key) && (parsed < MIN_UPLOAD_BYTES || parsed > MAX_UPLOAD_BYTES)) {
                throw new BusinessRuleException(
                    "storage.max_file_size_bytes debe estar entre " + MIN_UPLOAD_BYTES + " bytes (100 KB) y " + MAX_UPLOAD_BYTES + " bytes (10 MB)"
                );
            }
            if (KEY_PLATFORM_PASSWORD_MIN_LENGTH.equals(key)
                && (parsed < MIN_PLATFORM_PASSWORD_LEN || parsed > MAX_PLATFORM_PASSWORD_LEN)) {
                throw new BusinessRuleException(
                    "security.platform_password_min_length debe estar entre "
                        + MIN_PLATFORM_PASSWORD_LEN + " y " + MAX_PLATFORM_PASSWORD_LEN + " caracteres"
                );
            }
        }
        if ("BOOLEAN".equalsIgnoreCase(valueType)
            && !"true".equalsIgnoreCase(value)
            && !"false".equalsIgnoreCase(value)) {
            throw new BusinessRuleException("El parámetro '" + key + "' requiere 'true' o 'false'");
        }
        if (KEY_STORAGE_PROVIDER.equals(key) && !"LOCAL".equalsIgnoreCase(value) && !"S3".equalsIgnoreCase(value)) {
            throw new BusinessRuleException("storage.active_provider debe ser 'LOCAL' o 'S3'");
        }
    }

    private PlatformSettingDto toDto(PlatformSetting entity) {
        return new PlatformSettingDto(
            entity.getSettingKey(),
            entity.getSettingValue(),
            entity.getValueType(),
            entity.getDescription(),
            entity.getUpdatedAt(),
            entity.getUpdatedBy()
        );
    }
}
