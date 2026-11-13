package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.security.api.SessionPublicApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;
    private final RolePermissionService rolePermissionService;
    private final SessionPublicApi sessionPublicApi;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantEntitlementService tenantEntitlementService;
    private final long accessTokenExpirationSeconds;

    public AuthService(
        AppUserRepository appUserRepository,
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        UserBranchRepository userBranchRepository,
        RolePermissionService rolePermissionService,
        SessionPublicApi sessionPublicApi,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        TenantEntitlementService tenantEntitlementService,
        @Value("${restaurant.security.jwt.expiration-seconds:900}") long accessTokenExpirationSeconds
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.userBranchRepository = userBranchRepository;
        this.rolePermissionService = rolePermissionService;
        this.sessionPublicApi = sessionPublicApi;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tenantEntitlementService = tenantEntitlementService;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        return login(request, "127.0.0.1", "Web Client");
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String login = request.login().trim();

        Tenant tenant;
        if (request.tenantIdentifier() != null && !request.tenantIdentifier().isBlank()) {
            tenant = tenantRepository.findByIdentifier(request.tenantIdentifier().trim())
                .orElseThrow(() -> new BadCredentialsException("Empresa no encontrada con identificador: " + request.tenantIdentifier()));
        } else {
            // Compatibilidad hacia atrás si no se envía tenantIdentifier
            AppUser tempUser = appUserRepository.findByUsernameOrEmail(login)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
            tenant = tenantRepository.findById(tempUser.getTenantId())
                .orElseThrow(() -> new BadCredentialsException("Empresa no encontrada"));
        }

        AppUser user = appUserRepository.findByTenantIdAndUsernameOrEmail(tenant.getId(), login)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new BadCredentialsException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Validación de vigencia temporal del rol (ADR-0010)
        if (user.getRoleValidUntil() != null && user.getRoleValidUntil().isBefore(Instant.now())) {
            throw new BadCredentialsException("Tu rol temporal ha expirado. Contacta a tu administrador.");
        }

        // Evalúa bloqueo TOTAL (permite login en POS_AND_SHIFT_ONLY para consulta y regularización)
        tenantEntitlementService.assertLoginAllowed(tenant.getId());

        Set<String> permissions;
        if ("ADMIN_TENANT".equalsIgnoreCase(user.getRole()) || "SUPERADMIN".equalsIgnoreCase(user.getRole())) {
            permissions = new LinkedHashSet<>(SystemPermissions.ALL_PERMISSIONS);
            permissions.add("*");
        } else {
            permissions = rolePermissionService.getPermissionsForRole(tenant.getId(), user.getRole());
        }

        // Resolución de sucursales autorizadas (user_branches + default branch)
        Set<UUID> authorizedBranchIds;
        UUID effectiveBranchId = user.getBranchId();

        if ("ADMIN_TENANT".equalsIgnoreCase(user.getRole()) || "SUPERADMIN".equalsIgnoreCase(user.getRole())) {
            List<Branch> tenantBranches = branchRepository.findByTenantId(tenant.getId());
            authorizedBranchIds = tenantBranches.stream().map(Branch::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            if (effectiveBranchId == null && !tenantBranches.isEmpty()) {
                effectiveBranchId = tenantBranches.getFirst().getId();
            }
        } else {
            Set<UUID> assignedBranches = userBranchRepository.findByUserId(user.getId()).stream()
                .map(UserBranch::getBranchId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (user.getBranchId() != null) {
                assignedBranches.add(user.getBranchId());
            }
            if (effectiveBranchId == null && !assignedBranches.isEmpty()) {
                effectiveBranchId = assignedBranches.iterator().next();
            }
            authorizedBranchIds = assignedBranches;
        }

        String token = jwtService.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getTenantId(),
            effectiveBranchId,
            tenant.getSlug(),
            permissions,
            authorizedBranchIds
        );

        String refreshToken = sessionPublicApi.issueRefreshToken(tenant.getId(), user.getId(), ipAddress, userAgent);

        UserDto userDto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getRole(),
            user.getTenantId(),
            effectiveBranchId,
            tenant.getSlug(),
            permissions,
            authorizedBranchIds,
            user.getRoleValidUntil(),
            user.isMustChangePassword()
        );

        return LoginResponse.bearer(token, refreshToken, accessTokenExpirationSeconds, userDto);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        TenantContext context = TenantContextHolder.getContext()
            .orElseThrow(() -> new BadCredentialsException("No hay una sesión de usuario activa"));

        Set<String> permissions = context.permissions() != null ? context.permissions() :
            rolePermissionService.getPermissionsForRole(context.tenantId(), context.role());

        Set<UUID> authorizedBranchIds = context.authorizedBranchIds() != null
            ? context.authorizedBranchIds()
            : Collections.emptySet();

        return appUserRepository.findById(context.userId())
            .map(u -> new UserDto(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getRole(),
                context.tenantId(),
                context.branchId() != null ? context.branchId() : u.getBranchId(),
                context.tenantSlug(),
                permissions,
                authorizedBranchIds,
                u.getRoleValidUntil(),
                u.isMustChangePassword()
            ))
            .orElseGet(() -> new UserDto(
                context.userId(),
                context.username(),
                null,
                null,
                null,
                context.role(),
                context.tenantId(),
                context.branchId(),
                context.tenantSlug(),
                permissions,
                authorizedBranchIds,
                null,
                false
            ));
    }
}
