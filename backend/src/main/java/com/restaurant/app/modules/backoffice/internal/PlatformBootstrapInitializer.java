package com.restaurant.app.modules.backoffice.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Inicializador limpio de plataforma (Go-Live sin tenants demo) y modo de rescate Break-Glass.
 */
@Component
@Order(0)
class PlatformBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapInitializer.class);
    private static final String DEV_DEFAULT_USERNAME = "superadmin";
    private static final String DEV_DEFAULT_EMAIL = "superadmin@platform.local";
    private static final String DEV_DEFAULT_PASSWORD = "SuperAdminDev123!";

    private final PlatformAdminRepository adminRepository;
    private final PlatformRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;

    private final String configuredUsername;
    private final String configuredEmail;
    private final String configuredPassword;
    private final String configuredFullName;
    private final boolean breakGlassReset;
    private final boolean exitAfterReset;

    PlatformBootstrapInitializer(
        PlatformAdminRepository adminRepository,
        PlatformRefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        Environment environment,
        @Autowired(required = false) ConfigurableApplicationContext applicationContext,
        @Value("${app.platform.admin.username:}") String configuredUsername,
        @Value("${app.platform.admin.email:}") String configuredEmail,
        @Value("${app.platform.admin.password:}") String configuredPassword,
        @Value("${app.platform.admin.full-name:Platform Superadmin}") String configuredFullName,
        @Value("${app.platform.admin.break-glass-reset:false}") boolean breakGlassReset,
        @Value("${app.platform.admin.exit-after-reset:false}") boolean exitAfterReset
    ) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.applicationContext = applicationContext;
        this.configuredUsername = configuredUsername;
        this.configuredEmail = configuredEmail;
        this.configuredPassword = configuredPassword;
        this.configuredFullName = configuredFullName;
        this.breakGlassReset = breakGlassReset;
        this.exitAfterReset = exitAfterReset;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean strictEnvironment = !environment.acceptsProfiles(Profiles.of("dev", "test"));
        executeBootstrap(strictEnvironment, breakGlassReset);

        if (breakGlassReset && exitAfterReset && applicationContext != null) {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }
    }

    void executeBootstrap(boolean strictEnvironment, boolean isBreakGlassReset) {
        if (isBreakGlassReset) {
            performBreakGlassReset();
            return;
        }

        if (adminRepository.count() > 0) {
            return;
        }

        String username = resolveValue(configuredUsername, strictEnvironment, DEV_DEFAULT_USERNAME, "PLATFORM_ADMIN_USERNAME");
        String email = resolveValue(configuredEmail, strictEnvironment, DEV_DEFAULT_EMAIL, "PLATFORM_ADMIN_EMAIL");
        String rawPassword = resolveValue(configuredPassword, strictEnvironment, DEV_DEFAULT_PASSWORD, "PLATFORM_ADMIN_PASSWORD");
        String fullName = isBlank(configuredFullName) ? "Platform Superadmin" : configuredFullName.trim();

        validateMinimumPasswordLength(rawPassword);

        PlatformAdmin initialAdmin = new PlatformAdmin(
            username,
            email,
            fullName,
            passwordEncoder.encode(rawPassword),
            true
        );
        adminRepository.save(initialAdmin);
        log.info("Administrador inicial de plataforma '{}' creado con must_change_password=true.", initialAdmin.getUsername());
    }

    private void performBreakGlassReset() {
        if (isBlank(configuredUsername) || isBlank(configuredPassword)) {
            throw new IllegalStateException(
                "Break-Glass reset requiere definir PLATFORM_ADMIN_USERNAME y PLATFORM_ADMIN_PASSWORD."
            );
        }
        validateMinimumPasswordLength(configuredPassword);

        PlatformAdmin admin = adminRepository.findByUsernameNormalized(configuredUsername).orElse(null);
        if (admin == null) {
            if (isBlank(configuredEmail)) {
                throw new IllegalStateException(
                    "El administrador '" + configuredUsername + "' no existe y falta PLATFORM_ADMIN_EMAIL para crearlo."
                );
            }
            admin = new PlatformAdmin(
                configuredUsername,
                configuredEmail,
                isBlank(configuredFullName) ? "Platform Superadmin" : configuredFullName,
                passwordEncoder.encode(configuredPassword),
                true
            );
            admin = adminRepository.save(admin);
            log.warn("BREAK-GLASS: Administrador de plataforma '{}' creado en modo rescate.", admin.getUsername());
            return;
        }

        admin.setPasswordHash(passwordEncoder.encode(configuredPassword));
        admin.setActive(true);
        admin.setMustChangePassword(true);
        adminRepository.save(admin);
        refreshTokenRepository.revokeAllActiveByAdminId(admin.getId(), Instant.now());
        log.warn("BREAK-GLASS: Contraseña restablecida, cuenta reactivada, must_change_password=true y sesiones revocadas para '{}'.", admin.getUsername());
    }

    private String resolveValue(String value, boolean strictEnvironment, String devFallback, String envName) {
        if (!isBlank(value)) {
            return value.trim();
        }
        if (strictEnvironment) {
            throw new IllegalStateException(
                "Arranque abortado (fail-fast): falta la variable obligatoria " + envName + " para inicializar platform_admins."
            );
        }
        return devFallback;
    }

    private void validateMinimumPasswordLength(String password) {
        if (isBlank(password) || password.length() < PlatformAuthService.MIN_PLATFORM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "Arranque abortado: PLATFORM_ADMIN_PASSWORD debe tener al menos "
                    + PlatformAuthService.MIN_PLATFORM_PASSWORD_LENGTH + " caracteres."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
