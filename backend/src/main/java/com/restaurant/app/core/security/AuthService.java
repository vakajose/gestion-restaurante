package com.restaurant.app.core.security;

import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.TenantRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final RolePermissionService rolePermissionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        TenantRepository tenantRepository,
        RolePermissionService rolePermissionService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
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

        String token = jwtService.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getTenantId(),
            user.getBranchId(),
            tenant.getSlug(),
            permissions
        );

        return LoginResponse.bearer(token, UserDto.fromEntity(user, tenant.getSlug(), permissions));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        TenantContext context = TenantContextHolder.getContext()
            .orElseThrow(() -> new BadCredentialsException("No hay una sesión de usuario activa"));

        Set<String> permissions = context.permissions() != null ? context.permissions() :
            rolePermissionService.getPermissionsForRole(context.tenantId(), context.role());

        return appUserRepository.findById(context.userId())
            .map(u -> UserDto.fromEntity(u, context.tenantSlug(), permissions))
            .orElseGet(() -> new UserDto(
                context.userId(),
                context.username(),
                null,
                context.role(),
                context.tenantId(),
                context.branchId(),
                context.tenantSlug(),
                permissions
            ));
    }
}
