package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.JwtService;
import com.restaurant.app.core.security.RefreshToken;
import com.restaurant.app.core.security.RefreshTokenRepository;
import com.restaurant.app.core.security.RolePermissionService;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.UserBranch;
import com.restaurant.app.core.security.UserBranchRepository;
import com.restaurant.app.core.security.UserDto;
import com.restaurant.app.core.security.api.SessionPublicApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionPublicApi {

    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;
    private final RolePermissionService rolePermissionService;
    private final JwtService jwtService;
    private final long accessTokenExpirationSeconds;

    public SessionServiceImpl(
        RefreshTokenRepository refreshTokenRepository,
        AppUserRepository appUserRepository,
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        UserBranchRepository userBranchRepository,
        RolePermissionService rolePermissionService,
        JwtService jwtService,
        @Value("${restaurant.security.jwt.expiration-seconds:900}") long accessTokenExpirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.userBranchRepository = userBranchRepository;
        this.rolePermissionService = rolePermissionService;
        this.jwtService = jwtService;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    @Override
    @Transactional
    public String issueRefreshToken(UUID tenantId, UUID userId, String ipAddress, String userAgent) {
        String rawToken = generateOpaqueToken();
        String tokenHash = computeSha256Hex(rawToken);
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        RefreshToken entity = new RefreshToken(
            tenantId,
            userId,
            tokenHash,
            truncate(userAgent != null ? userAgent : "Web Client", 255),
            truncate(ipAddress != null ? ipAddress : "127.0.0.1", 45),
            expiresAt
        );
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, UnauthorizedOperationException.class})
    public RefreshResponse refreshSession(RefreshRequest request, String ipAddress, String userAgent) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token es obligatorio");
        }

        String presentedToken = request.refreshToken().trim();
        String tokenHash = computeSha256Hex(presentedToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o inexistente"));

        Instant now = Instant.now();

        // Protocolo de Detección de Reúso (ADR-0011): si ya fue revocado, revocar toda la familia activa del usuario
        if (storedToken.getRevokedAt() != null) {
            int revokedCount = refreshTokenRepository.revokeAllActiveByUserId(storedToken.getUserId(), now);
            log.warn("SUSPICIOUS_TOKEN_REUSE_DETECTED: Intento de reúso de refresh token revocado (id={}) para usuario {}. Sesiones activas revocadas: {}",
                storedToken.getId(), storedToken.getUserId(), revokedCount);
            throw new BadCredentialsException("Token reuse detected. All active sessions have been revoked for security.");
        }

        // Verificar expiración temporal del Refresh Token
        if (storedToken.getExpiresAt().isBefore(now)) {
            storedToken.setRevokedAt(now);
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("El refresh token ha expirado. Inicie sesión nuevamente.");
        }

        AppUser user = appUserRepository.findById(storedToken.getUserId())
            .orElseThrow(() -> new BadCredentialsException("Usuario asociado a la sesión no encontrado"));

        if (!user.isActive()) {
            storedToken.setRevokedAt(now);
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("La cuenta de usuario se encuentra inactiva");
        }

        if (user.getRoleValidUntil() != null && user.getRoleValidUntil().isBefore(now)) {
            storedToken.setRevokedAt(now);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedOperationException("Tu rol temporal ha expirado. Contacta a tu administrador.");
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId())
            .orElseThrow(() -> new BadCredentialsException("Empresa no encontrada"));

        // Rotación atómica: emitir nuevo Refresh Token y vincular replaced_by_id
        String newRawRefreshToken = generateOpaqueToken();
        String newTokenHash = computeSha256Hex(newRawRefreshToken);
        Instant newExpiresAt = now.plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        RefreshToken replacementToken = new RefreshToken(
            tenant.getId(),
            user.getId(),
            newTokenHash,
            truncate(userAgent != null ? userAgent : storedToken.getDeviceInfo(), 255),
            truncate(ipAddress != null ? ipAddress : storedToken.getIpAddress(), 45),
            newExpiresAt
        );
        replacementToken = refreshTokenRepository.save(replacementToken);

        storedToken.setRevokedAt(now);
        storedToken.setReplacedById(replacementToken.getId());
        refreshTokenRepository.save(storedToken);

        // Resolver permisos y sucursales autorizadas actualizados
        Set<String> permissions = resolvePermissionsForUser(tenant.getId(), user.getRole());
        Set<UUID> authorizedBranchIds = resolveAuthorizedBranches(tenant.getId(), user);
        UUID effectiveBranchId = resolveEffectiveBranchId(user, authorizedBranchIds);

        String newAccessToken = jwtService.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            tenant.getId(),
            effectiveBranchId,
            tenant.getSlug(),
            permissions,
            authorizedBranchIds
        );

        UserDto userDto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole(),
            tenant.getId(),
            effectiveBranchId,
            tenant.getSlug(),
            permissions,
            authorizedBranchIds,
            user.getRoleValidUntil(),
            user.isMustChangePassword()
        );

        return new RefreshResponse(
            newAccessToken,
            newAccessToken,
            newRawRefreshToken,
            "Bearer",
            accessTokenExpirationSeconds,
            userDto
        );
    }

    @Override
    @Transactional
    public void revokeCurrentSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String tokenHash = computeSha256Hex(refreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            if (rt.getRevokedAt() == null) {
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            }
        });
    }

    @Override
    @Transactional
    public void revokeSessionById(UUID userId, UUID sessionId) {
        RefreshToken rt = refreshTokenRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Sesión", sessionId));
        if (!rt.getUserId().equals(userId)) {
            throw new UnauthorizedOperationException("No tiene permisos para revocar una sesión de otro usuario");
        }
        if (rt.getRevokedAt() == null) {
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        }
    }

    @Override
    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        if (userId == null) {
            return;
        }
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveSessionDto> listActiveSessions(UUID userId, String currentRefreshToken) {
        String currentHash = (currentRefreshToken != null && !currentRefreshToken.isBlank())
            ? computeSha256Hex(currentRefreshToken.trim())
            : null;
        Instant now = Instant.now();

        return refreshTokenRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId).stream()
            .filter(rt -> rt.getExpiresAt().isAfter(now))
            .map(rt -> new ActiveSessionDto(
                rt.getId(),
                rt.getDeviceInfo(),
                rt.getIpAddress(),
                rt.getCreatedAt(),
                rt.getExpiresAt(),
                currentHash != null && currentHash.equals(rt.getTokenHash())
            ))
            .toList();
    }

    private Set<String> resolvePermissionsForUser(UUID tenantId, String roleCode) {
        if ("ADMIN_TENANT".equalsIgnoreCase(roleCode) || "SUPERADMIN".equalsIgnoreCase(roleCode)) {
            Set<String> adminPerms = new LinkedHashSet<>(SystemPermissions.ALL_PERMISSIONS);
            adminPerms.add("*");
            return adminPerms;
        }
        return rolePermissionService.getPermissionsForRole(tenantId, roleCode);
    }

    private Set<UUID> resolveAuthorizedBranches(UUID tenantId, AppUser user) {
        if ("ADMIN_TENANT".equalsIgnoreCase(user.getRole()) || "SUPERADMIN".equalsIgnoreCase(user.getRole())) {
            return branchRepository.findByTenantId(tenantId).stream()
                .map(Branch::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Set<UUID> assigned = userBranchRepository.findByUserId(user.getId()).stream()
            .map(UserBranch::getBranchId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (user.getBranchId() != null) {
            assigned.add(user.getBranchId());
        }
        return assigned;
    }

    private UUID resolveEffectiveBranchId(AppUser user, Set<UUID> authorizedBranchIds) {
        if (user.getBranchId() != null) {
            return user.getBranchId();
        }
        if (!authorizedBranchIds.isEmpty()) {
            return authorizedBranchIds.iterator().next();
        }
        return null;
    }

    public static String computeSha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String generateOpaqueToken() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return UUID.randomUUID() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) {
            return null;
        }
        return val.length() <= maxLen ? val : val.substring(0, maxLen);
    }
}
