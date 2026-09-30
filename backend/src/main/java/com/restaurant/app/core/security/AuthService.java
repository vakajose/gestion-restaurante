package com.restaurant.app.core.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String login = request.login().trim();

        AppUser user = appUserRepository.findByUsernameOrEmail(login)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new BadCredentialsException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user);
        return LoginResponse.bearer(token, UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        TenantContext context = TenantContextHolder.getContext()
            .orElseThrow(() -> new BadCredentialsException("No hay una sesión de usuario activa"));

        return appUserRepository.findById(context.userId())
            .map(UserDto::fromEntity)
            .orElseGet(() -> new UserDto(
                context.userId(),
                context.username(),
                null,
                context.role(),
                context.tenantId(),
                context.branchId()
            ));
    }
}
