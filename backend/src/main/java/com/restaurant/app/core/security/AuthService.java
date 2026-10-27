package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        RolePermissionService rolePermissionService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.rolePermissionService = rolePermissionService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
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

        Set<String> permissions = rolePermissionService.getPermissionsForRole(tenant.getId(), user.getRole());

        // Resolución de sucursales autorizadas y sucursal activa predeterminada
        Set<UUID> authorizedBranchIds;
        UUID effectiveBranchId = user.getBranchId();

        if ("ADMIN_TENANT".equalsIgnoreCase(user.getRole()) || "SUPERADMIN".equalsIgnoreCase(user.getRole())) {
            List<Branch> tenantBranches = branchRepository.findByTenantId(tenant.getId());
            authorizedBranchIds = tenantBranches.stream().map(Branch::getId).collect(Collectors.toSet());
            if (effectiveBranchId == null && !tenantBranches.isEmpty()) {
                effectiveBranchId = tenantBranches.getFirst().getId();
            }
        } else {
            if (user.getBranchId() != null) {
                authorizedBranchIds = Set.of(user.getBranchId());
            } else {
                authorizedBranchIds = Collections.emptySet();
            }
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

        UserDto userDto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getTenantId(),
            effectiveBranchId,
            tenant.getSlug(),
            permissions,
            authorizedBranchIds
        );

        return LoginResponse.bearer(token, userDto);
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
                u.getEmail(),
                u.getRole(),
                context.tenantId(),
                context.branchId() != null ? context.branchId() : u.getBranchId(),
                context.tenantSlug(),
                permissions,
                authorizedBranchIds
            ))
            .orElseGet(() -> new UserDto(
                context.userId(),
                context.username(),
                null,
                context.role(),
                context.tenantId(),
                context.branchId(),
                context.tenantSlug(),
                permissions,
                authorizedBranchIds
            ));
    }
}
