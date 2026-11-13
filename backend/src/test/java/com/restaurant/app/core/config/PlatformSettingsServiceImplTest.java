package com.restaurant.app.core.config.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.config.PlatformSetting;
import com.restaurant.app.core.config.PlatformSettingRepository;
import com.restaurant.app.core.config.api.PlatformSettingDto;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformSettingsServiceImplTest {

    private PlatformSettingRepository repository;
    private PlatformSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PlatformSettingRepository.class);
        service = new PlatformSettingsServiceImpl(repository);
    }

    @Test
    @DisplayName("Debe cachear lecturas y actualizar caché en caliente al modificar storage.max_file_size_bytes")
    void shouldCacheReadsAndHotReloadOnUpdate() {
        UUID adminId = UUID.randomUUID();
        PlatformSetting existing = new PlatformSetting(
            PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES,
            "1048576",
            "NUMBER",
            "Max bytes",
            null
        );
        when(repository.findById(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES))
            .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.getLong(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, 0L)).isEqualTo(1_048_576L);
        assertThat(service.getLong(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, 0L)).isEqualTo(1_048_576L);
        verify(repository, times(1)).findById(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES);

        PlatformSettingDto updated = service.updateSetting(
            PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES,
            "2097152",
            adminId
        );
        assertThat(updated.value()).isEqualTo("2097152");
        assertThat(updated.updatedBy()).isEqualTo(adminId);
        assertThat(service.getLong(PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, 0L)).isEqualTo(2_097_152L);
    }

    @Test
    @DisplayName("Debe validar rango seguro [100 KB .. 10 MB] y valores válidos de storage.active_provider")
    void shouldValidateStorageBoundsAndProviderValues() {
        UUID adminId = UUID.randomUUID();
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.updateSetting(
            PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, "50000", adminId
        )).isInstanceOf(BusinessRuleException.class);

        assertThatThrownBy(() -> service.updateSetting(
            PlatformSettingsService.KEY_STORAGE_MAX_FILE_SIZE_BYTES, "50000000", adminId
        )).isInstanceOf(BusinessRuleException.class);

        assertThatThrownBy(() -> service.updateSetting(
            PlatformSettingsService.KEY_STORAGE_PROVIDER, "FTP", adminId
        )).isInstanceOf(BusinessRuleException.class);

        PlatformSettingDto s3 = service.updateSetting(PlatformSettingsService.KEY_STORAGE_PROVIDER, "S3", adminId);
        assertThat(s3.value()).isEqualTo("S3");
    }
}
