package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.ChangePasswordRequest;
import com.restaurant.app.core.security.api.CreateUserRequest;
import com.restaurant.app.core.security.api.UpdateProfileRequest;
import com.restaurant.app.core.security.api.UpdateUserRequest;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
class UserManagementServiceImpl implements UserPublicApi {

    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    UserManagementServiceImpl(
        AppUserRepository appUserRepository,
        BranchRepository branchRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getUsersByTenant(UUID tenantId) {
        return appUserRepository.findByTenantId(tenantId).stream()
            .map(UserSummaryDto::fromEntity)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getUserById(UUID tenantId, UUID userId) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + userId));
        return UserSummaryDto.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> findUserSummary(UUID userId) {
        return appUserRepository.findById(userId)
            .map(UserSummaryDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserActive(UUID userId) {
        return appUserRepository.findById(userId)
            .map(AppUser::isActive)
            .orElse(false);
    }

    @Override
    @Transactional
    public UserSummaryDto createUser(UUID tenantId, CreateUserRequest request) {
        String username = request.username().trim();
        if (appUserRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya existe en esta empresa");
        }

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new IllegalArgumentException("El correo electrónico '" + email + "' ya está registrado en esta empresa");
        }

        if (request.branchId() != null && branchRepository.findByTenantIdAndId(tenantId, request.branchId()).isEmpty()) {
            throw new IllegalArgumentException("La sucursal seleccionada no existe o no pertenece a la empresa actual");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        AppUser newUser = new AppUser(
            tenantId,
            request.branchId(),
            username,
            email,
            passwordHash,
            request.role().trim().toUpperCase()
        );
        newUser.setActive(true);

        AppUser saved = appUserRepository.save(newUser);
        return UserSummaryDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserSummaryDto updateUser(UUID tenantId, UUID userId, UpdateUserRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + userId));

        String username = request.username().trim();
        if (!user.getUsername().equalsIgnoreCase(username) && appUserRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new IllegalArgumentException("El nombre de usuario '" + username + "' ya existe en esta empresa");
        }

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new IllegalArgumentException("El correo electrónico '" + email + "' ya está registrado en esta empresa");
        }

        if (request.branchId() != null && branchRepository.findByTenantIdAndId(tenantId, request.branchId()).isEmpty()) {
            throw new IllegalArgumentException("La sucursal seleccionada no existe o no pertenece a la empresa actual");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole(request.role().trim().toUpperCase());
        user.setBranchId(request.branchId());
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        AppUser updated = appUserRepository.save(user);
        return UserSummaryDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserSummaryDto toggleUserStatus(UUID tenantId, UUID userId, boolean isActive) {
        if (!isActive && userId.equals(TenantContextHolder.getUserId())) {
            throw new IllegalStateException("No puede desactivar su propio usuario activo");
        }

        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + userId));

        user.setActive(isActive);
        AppUser updated = appUserRepository.save(user);
        return UserSummaryDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserSummaryDto updateProfile(UUID tenantId, UUID userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado por otro usuario");
        }

        user.setEmail(email);
        AppUser updated = appUserRepository.save(user);
        return UserSummaryDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void changePassword(UUID tenantId, UUID userId, ChangePasswordRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }
}
