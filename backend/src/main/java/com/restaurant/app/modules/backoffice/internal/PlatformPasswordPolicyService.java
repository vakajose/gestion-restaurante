package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.config.api.PlatformSettingsService;
import org.springframework.stereotype.Service;

/**
 * Evalúa la política configurable de contraseñas para administradores de plataforma
 * leyendo los parámetros dinámicos desde {@link PlatformSettingsService}.
 */
@Service
class PlatformPasswordPolicyService {

    static final int DEFAULT_MIN_LENGTH = 12;

    private final PlatformSettingsService platformSettingsService;

    PlatformPasswordPolicyService(PlatformSettingsService platformSettingsService) {
        this.platformSettingsService = platformSettingsService;
    }

    record PasswordPolicyDto(int minLength, boolean requireComplexity) {
    }

    PasswordPolicyDto currentPolicy() {
        long configured = platformSettingsService.getLong(
            PlatformSettingsService.KEY_PLATFORM_PASSWORD_MIN_LENGTH,
            DEFAULT_MIN_LENGTH
        );
        int minLength = (int) Math.max(6L, Math.min(64L, configured));
        boolean requireComplexity = platformSettingsService.getBoolean(
            PlatformSettingsService.KEY_PLATFORM_PASSWORD_REQUIRE_COMPLEXITY,
            false
        );
        return new PasswordPolicyDto(minLength, requireComplexity);
    }

    void validate(String rawPassword) {
        PasswordPolicyDto policy = currentPolicy();
        if (rawPassword == null || rawPassword.length() < policy.minLength()) {
            throw new BusinessRuleException(
                "La contraseña de administrador de plataforma debe tener al menos "
                    + policy.minLength() + " caracteres"
            );
        }
        if (policy.requireComplexity()) {
            boolean hasUpper = rawPassword.chars().anyMatch(Character::isUpperCase);
            boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
            boolean hasSymbol = rawPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
            if (!hasUpper || !hasDigit || !hasSymbol) {
                throw new BusinessRuleException(
                    "La contraseña debe incluir al menos una letra mayúscula, un dígito y un carácter especial"
                );
            }
        }
    }
}
