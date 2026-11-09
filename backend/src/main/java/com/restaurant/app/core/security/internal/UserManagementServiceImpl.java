package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.Role;
import com.restaurant.app.core.security.RolePermission;
import com.restaurant.app.core.security.RolePermissionRepository;
import com.restaurant.app.core.security.RoleRepository;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.UserBranch;
import com.restaurant.app.core.security.UserBranchRepository;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class UserManagementServiceImpl implements UserPublicApi {

    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserBranchRepository userBranchRepository;
    private final PasswordEncoder passwordEncoder;

    UserManagementServiceImpl(
        AppUserRepository appUserRepository,
        BranchRepository branchRepository,
        RoleRepository roleRepository,
        RolePermissionRepository rolePermissionRepository,
        UserBranchRepository userBranchRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userBranchRepository = userBranchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDto> getUsersByTenant(UUID tenantId) {
        return appUserRepository.findByTenantId(tenantId).stream()
            .map(this::toUserSummaryDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getUserById(UUID tenantId, UUID userId) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));
        return toUserSummaryDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> findUserSummary(UUID userId) {
        return appUserRepository.findById(userId)
            .map(this::toUserSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> findByTenantIdAndUsername(UUID tenantId, String username) {
        return appUserRepository.findByTenantIdAndUsername(tenantId, username)
            .map(this::toUserSummaryDto);
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
    public void seedDefaultRolesForTenant(UUID tenantId) {
        createSystemRoleIfNotExists(tenantId, "ADMIN_TENANT", "Administrador de Empresa", "Acceso total a la administración de la empresa");
        createSystemRoleIfNotExists(tenantId, "BRANCH_MANAGER", "Gerente de Sucursal", "Gestión operativa, inventarios y caja de sucursal");
        createSystemRoleIfNotExists(tenantId, "CASHIER", "Cajero", "Operación de ventas y punto de venta");
        createSystemRoleIfNotExists(tenantId, "KITCHEN", "Cocina", "Visualización de comandas y recetas");
    }

    private void createSystemRoleIfNotExists(UUID tenantId, String code, String name, String description) {
        if (roleRepository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            return;
        }
        Role role = roleRepository.save(new Role(tenantId, code, name, description, true));
        Set<String> permissions = SystemPermissions.getDefaultPermissionsForRole(code);
        for (String perm : permissions) {
            rolePermissionRepository.save(new RolePermission(tenantId, role.getId(), perm));
        }
    }

    @Override
    @Transactional
    public UserSummaryDto createUser(UUID tenantId, CreateUserRequest request) {
        String username = request.username().trim();
        if (appUserRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new ResourceConflictException("El nombre de usuario '" + username + "' ya existe en esta empresa");
        }

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ResourceConflictException("El correo electrónico '" + email + "' ya está registrado en esta empresa");
        }

        if (request.branchId() != null && branchRepository.findByTenantIdAndId(tenantId, request.branchId()).isEmpty()) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + request.branchId());
        }

        String roleCode = request.role().trim().toUpperCase();
        String passwordHash = passwordEncoder.encode(request.password());
        AppUser newUser = new AppUser(
            tenantId,
            request.branchId(),
            username,
            email,
            passwordHash,
            roleCode
        );
        if (request.fullName() != null && !request.fullName().isBlank()) {
            newUser.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            newUser.setPhoneNumber(request.phoneNumber().trim());
        }
        if (request.roleValidUntil() != null) {
            newUser.setRoleValidUntil(request.roleValidUntil());
        }
        roleRepository.findByTenantIdAndCode(tenantId, roleCode)
            .ifPresent(r -> newUser.setRoleId(r.getId()));
        newUser.setActive(true);

        AppUser saved = appUserRepository.save(newUser);
        syncUserBranches(tenantId, saved.getId(), saved.getBranchId(), request.authorizedBranchIds());

        return toUserSummaryDto(saved);
    }

    @Override
    @Transactional
    public UserSummaryDto updateUser(UUID tenantId, UUID userId, UpdateUserRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        String username = request.username().trim();
        if (!user.getUsername().equalsIgnoreCase(username) && appUserRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new ResourceConflictException("El nombre de usuario '" + username + "' ya existe en esta empresa");
        }

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ResourceConflictException("El correo electrónico '" + email + "' ya está registrado en esta empresa");
        }

        if (request.branchId() != null && branchRepository.findByTenantIdAndId(tenantId, request.branchId()).isEmpty()) {
            throw new EntityNotFoundException("Sucursal no encontrada: " + request.branchId());
        }

        String roleCode = request.role().trim().toUpperCase();
        user.setUsername(username);
        if (request.fullName() != null) {
            user.setFullName(request.fullName().isBlank() ? null : request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().isBlank() ? null : request.phoneNumber().trim());
        }
        user.setEmail(email);
        user.setRole(roleCode);
        roleRepository.findByTenantIdAndCode(tenantId, roleCode)
            .ifPresentOrElse(r -> user.setRoleId(r.getId()), () -> user.setRoleId(null));
        user.setBranchId(request.branchId());
        user.setRoleValidUntil(request.roleValidUntil());
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setMustChangePassword(false);
        }

        AppUser updated = appUserRepository.save(user);
        if (request.authorizedBranchIds() != null) {
            syncUserBranches(tenantId, updated.getId(), updated.getBranchId(), request.authorizedBranchIds());
        }

        return toUserSummaryDto(updated);
    }

    @Override
    @Transactional
    public UserSummaryDto toggleUserStatus(UUID tenantId, UUID userId, boolean isActive) {
        if (!isActive && userId.equals(TenantContextHolder.getUserId())) {
            throw new BusinessRuleException("No puede desactivar su propio usuario activo");
        }

        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        user.setActive(isActive);
        AppUser updated = appUserRepository.save(user);
        return toUserSummaryDto(updated);
    }

    @Override
    @Transactional
    public UserSummaryDto updateProfile(UUID tenantId, UUID userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        if (email != null && (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) && appUserRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ResourceConflictException("El correo electrónico ya se encuentra registrado por otro usuario");
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().isBlank() ? null : request.phoneNumber().trim());
        }
        user.setEmail(email);
        AppUser updated = appUserRepository.save(user);
        return toUserSummaryDto(updated);
    }

    @Override
    @Transactional
    public void changePassword(UUID tenantId, UUID userId, ChangePasswordRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePin(UUID tenantId, UUID userId, String pin) {
        if (pin == null || !pin.matches("^\\d{4,6}$")) {
            throw new ValidationException("El PIN debe contener entre 4 y 6 dígitos numéricos");
        }
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        user.setPinHash(passwordEncoder.encode(pin));
        appUserRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummaryDto> validateSupervisorPin(UUID tenantId, UUID branchId, String pin) {
        if (pin == null || pin.isBlank()) {
            return Optional.empty();
        }
        List<AppUser> users = appUserRepository.findByTenantId(tenantId);
        for (AppUser user : users) {
            if (!user.isActive() || user.getPinHash() == null || user.getPinHash().isBlank()) {
                continue;
            }
            if (passwordEncoder.matches(pin, user.getPinHash())) {
                String role = user.getRole();
                boolean isSupervisor = "SUPERADMIN".equalsIgnoreCase(role)
                    || "ADMIN_TENANT".equalsIgnoreCase(role)
                    || "BRANCH_MANAGER".equalsIgnoreCase(role);

                if (!isSupervisor && user.getRoleId() != null) {
                    List<String> permissions = rolePermissionRepository.findPermissionsByRoleId(user.getRoleId());
                    isSupervisor = permissions.contains(SystemPermissions.FINANCE_SHIFTS_SUPERVISOR) ||
                        permissions.contains(SystemPermissions.FINANCE_EXPENSES_APPROVE) ||
                        permissions.contains(SystemPermissions.POS_VOID_ITEM);
                }

                if (isSupervisor) {
                    if ("SUPERADMIN".equalsIgnoreCase(role) || "ADMIN_TENANT".equalsIgnoreCase(role)) {
                        return Optional.of(toUserSummaryDto(user));
                    }
                    if (branchId == null || (user.getBranchId() != null && user.getBranchId().equals(branchId))) {
                        return Optional.of(toUserSummaryDto(user));
                    }
                    boolean branchAuthorized = userBranchRepository.findByUserId(user.getId()).stream()
                        .anyMatch(ub -> ub.getBranchId().equals(branchId));
                    if (branchAuthorized) {
                        return Optional.of(toUserSummaryDto(user));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void syncUserBranches(UUID tenantId, UUID userId, UUID defaultBranchId, Set<UUID> authorizedBranchIds) {
        Set<UUID> toAssign = new LinkedHashSet<>();
        if (authorizedBranchIds != null) {
            toAssign.addAll(authorizedBranchIds);
        }
        if (defaultBranchId != null) {
            toAssign.add(defaultBranchId);
        }
        if (toAssign.isEmpty()) {
            return;
        }
        userBranchRepository.deleteByUserId(userId);
        userBranchRepository.flush();
        for (UUID bid : toAssign) {
            if (branchRepository.findByTenantIdAndId(tenantId, bid).isPresent()) {
                userBranchRepository.save(new UserBranch(tenantId, userId, bid));
            }
        }
    }

    private UserSummaryDto toUserSummaryDto(AppUser user) {
        Set<UUID> branches = userBranchRepository.findByUserId(user.getId()).stream()
            .map(UserBranch::getBranchId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (user.getBranchId() != null) {
            branches.add(user.getBranchId());
        }
        return UserSummaryDto.fromEntity(user, branches);
    }
}
