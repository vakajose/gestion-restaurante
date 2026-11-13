package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.security.TokenHashSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
class PlatformAuthService {

    static final int MIN_PLATFORM_PASSWORD_LENGTH = 12;
    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;
    private static final Logger log = LoggerFactory.getLogger(PlatformAuthService.class);

    private final PlatformAdminRepository adminRepository;
    private final PlatformRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformJwtService platformJwtService;

    PlatformAuthService(
        PlatformAdminRepository adminRepository,
        PlatformRefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        PlatformJwtService platformJwtService
    ) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.platformJwtService = platformJwtService;
    }

    record LoginRequest(String login, String password) {
    }

    record RefreshRequest(String refreshToken) {
    }

    record LogoutRequest(String refreshToken) {
    }

    record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    record PlatformAuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        PlatformPrincipal admin
    ) {
    }

    @Transactional
    public PlatformAuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        if (request == null || request.login() == null || request.login().isBlank() || request.password() == null) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        PlatformAdmin admin = adminRepository.findByLogin(request.login())
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!admin.isActive()) {
            throw new BadCredentialsException("La cuenta de administrador de plataforma se encuentra inactiva");
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        admin.setLastLoginAt(Instant.now());
        adminRepository.save(admin);

        String accessToken = platformJwtService.generateToken(admin);
        String refreshToken = issueRefreshToken(admin.getId(), ipAddress, userAgent);

        return new PlatformAuthResponse(
            accessToken,
            refreshToken,
            platformJwtService.getExpirationSeconds(),
            toPrincipal(admin)
        );
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public PlatformAuthResponse refresh(RefreshRequest request, String ipAddress, String userAgent) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token es obligatorio");
        }

        String tokenHash = TokenHashSupport.computeSha256Hex(request.refreshToken());
        PlatformRefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Refresh token de plataforma inválido o inexistente"));

        Instant now = Instant.now();
        if (storedToken.getRevokedAt() != null) {
            log.warn("Reúso de refresh token detectado para platform_admin={}. Revocando sesiones activas.", storedToken.getAdminId());
            refreshTokenRepository.revokeAllActiveByAdminId(storedToken.getAdminId(), now);
            throw new BadCredentialsException("Se detectó reúso de sesión. Todas las sesiones activas han sido revocadas.");
        }

        if (storedToken.getExpiresAt().isBefore(now)) {
            storedToken.setRevokedAt(now);
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("El refresh token de plataforma ha expirado. Inicie sesión nuevamente.");
        }

        PlatformAdmin admin = adminRepository.findById(storedToken.getAdminId())
            .orElseThrow(() -> new BadCredentialsException("Administrador de plataforma no encontrado"));

        if (!admin.isActive()) {
            throw new BadCredentialsException("La cuenta de administrador de plataforma se encuentra inactiva");
        }

        String nextRawToken = TokenHashSupport.generateOpaqueToken();
        String nextHash = TokenHashSupport.computeSha256Hex(nextRawToken);
        PlatformRefreshToken nextEntity = new PlatformRefreshToken(
            admin.getId(),
            nextHash,
            TokenHashSupport.truncate(userAgent, "Backoffice Client", 255),
            TokenHashSupport.truncate(ipAddress, "127.0.0.1", 45),
            now.plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS)
        );
        PlatformRefreshToken savedNext = refreshTokenRepository.save(nextEntity);

        storedToken.setRevokedAt(now);
        storedToken.setReplacedById(savedNext.getId());
        refreshTokenRepository.save(storedToken);

        String accessToken = platformJwtService.generateToken(admin);
        return new PlatformAuthResponse(
            accessToken,
            nextRawToken,
            platformJwtService.getExpirationSeconds(),
            toPrincipal(admin)
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String tokenHash = TokenHashSupport.computeSha256Hex(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional
    public PlatformPrincipal changeOwnPassword(UUID adminId, ChangePasswordRequest request) {
        if (request == null || request.currentPassword() == null || request.newPassword() == null) {
            throw new BusinessRuleException("Debe indicar contraseña actual y nueva contraseña");
        }

        PlatformAdmin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("Administrador de plataforma no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        validatePlatformPasswordPolicy(request.newPassword());

        if (passwordEncoder.matches(request.newPassword(), admin.getPasswordHash())) {
            throw new BusinessRuleException("La nueva contraseña debe ser diferente a la actual");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        admin.setMustChangePassword(false);
        PlatformAdmin saved = adminRepository.save(admin);

        refreshTokenRepository.revokeAllActiveByAdminId(adminId, Instant.now());
        return toPrincipal(saved);
    }

    @Transactional(readOnly = true)
    public PlatformPrincipal getProfile(UUID adminId) {
        PlatformAdmin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new EntityNotFoundException("Administrador de plataforma no encontrado"));
        return toPrincipal(admin);
    }

    static void validatePlatformPasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PLATFORM_PASSWORD_LENGTH) {
            throw new BusinessRuleException(
                "La contraseña de administrador de plataforma debe tener al menos " + MIN_PLATFORM_PASSWORD_LENGTH + " caracteres"
            );
        }
    }

    private String issueRefreshToken(UUID adminId, String ipAddress, String userAgent) {
        String rawToken = TokenHashSupport.generateOpaqueToken();
        String tokenHash = TokenHashSupport.computeSha256Hex(rawToken);
        PlatformRefreshToken entity = new PlatformRefreshToken(
            adminId,
            tokenHash,
            TokenHashSupport.truncate(userAgent, "Backoffice Client", 255),
            TokenHashSupport.truncate(ipAddress, "127.0.0.1", 45),
            Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private PlatformPrincipal toPrincipal(PlatformAdmin admin) {
        return new PlatformPrincipal(
            admin.getId(),
            admin.getUsername(),
            admin.getEmail(),
            admin.getFullName(),
            admin.getRole(),
            admin.isMustChangePassword()
        );
    }
}
