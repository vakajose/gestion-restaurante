package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.Role;
import com.restaurant.app.core.security.RolePermission;
import com.restaurant.app.core.security.RolePermissionRepository;
import com.restaurant.app.core.security.RoleRepository;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.UserBranch;
import com.restaurant.app.core.security.UserBranchRepository;
import com.restaurant.app.core.security.api.RolePublicApi;
import com.restaurant.app.core.security.api.SessionPublicApi;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
class RoleManagementServiceImpl implements RolePublicApi {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AppUserRepository appUserRepository;
    private final BranchRepository branchRepository;
    private final UserBranchRepository userBranchRepository;
    private final SessionPublicApi sessionPublicApi;
    private final PasswordEncoder passwordEncoder;

    RoleManagementServiceImpl(
        RoleRepository roleRepository,
        RolePermissionRepository rolePermissionRepository,
        AppUserRepository appUserRepository,
        BranchRepository branchRepository,
        UserBranchRepository userBranchRepository,
        SessionPublicApi sessionPublicApi,
        PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.userBranchRepository = userBranchRepository;
        this.sessionPublicApi = sessionPublicApi;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId).stream()
            .map(this::toRoleDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleDto> getRole(UUID tenantId, UUID roleId) {
        return roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .map(this::toRoleDto);
    }

    @Override
    @Transactional
    public RoleDto createRole(UUID tenantId, CreateRoleRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ValidationException("El nombre del rol es obligatorio");
        }

        String name = request.name().trim();
        String rawCode = (request.code() != null && !request.code().isBlank())
            ? request.code().trim()
            : name;
        String code = normalizeRoleCode(rawCode);

        if ("ADMIN_TENANT".equalsIgnoreCase(code) || "SUPERADMIN".equalsIgnoreCase(code)) {
            throw new BusinessRuleException("El código de rol '" + code + "' está reservado por el sistema y es inmutable");
        }

        if (roleRepository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new ResourceConflictException("Ya existe un rol con el código '" + code + "' en esta empresa");
        }

        Role role = new Role(tenantId, code, name, request.description(), false);
        role = roleRepository.save(role);

        Set<String> perms = sanitizePermissions(request.permissions());
        for (String perm : perms) {
            rolePermissionRepository.save(new RolePermission(tenantId, role.getId(), perm));
        }

        return new RoleDto(
            role.getId(),
            role.getTenantId(),
            role.getCode(),
            role.getName(),
            role.getDescription(),
            role.isSystem(),
            perms
        );
    }

    @Override
    @Transactional
    public RoleDto updateRole(UUID tenantId, UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("Rol", roleId));

        if ("ADMIN_TENANT".equalsIgnoreCase(role.getCode()) || "SUPERADMIN".equalsIgnoreCase(role.getCode())) {
            throw new BusinessRuleException("El rol '" + role.getCode() + "' es inmutable y no puede ser modificado ni restringido");
        }

        if (request.name() != null && !request.name().isBlank()) {
            role.setName(request.name().trim());
        }
        if (request.description() != null) {
            role.setDescription(request.description().trim());
        }
        role = roleRepository.save(role);

        if (request.permissions() != null) {
            rolePermissionRepository.deleteByRoleId(role.getId());
            rolePermissionRepository.flush();
            Set<String> perms = sanitizePermissions(request.permissions());
            for (String perm : perms) {
                rolePermissionRepository.save(new RolePermission(tenantId, role.getId(), perm));
            }
        }

        return toRoleDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(UUID tenantId, UUID roleId) {
        Role role = roleRepository.findById(roleId)
            .filter(r -> tenantId.equals(r.getTenantId()))
            .orElseThrow(() -> new EntityNotFoundException("Rol", roleId));

        if ("ADMIN_TENANT".equalsIgnoreCase(role.getCode()) || "SUPERADMIN".equalsIgnoreCase(role.getCode()) || role.isSystem()) {
            throw new BusinessRuleException("No está permitido eliminar el rol nativo del sistema '" + role.getCode() + "'");
        }

        if (appUserRepository.existsByTenantIdAndRoleIgnoreCaseAndIsActiveTrue(tenantId, role.getCode())) {
            throw new ResourceConflictException("No se puede eliminar el rol '" + role.getName() + "' porque tiene usuarios activos asignados");
        }

        rolePermissionRepository.deleteByRoleId(role.getId());
        roleRepository.delete(role);
    }

    @Override
    public List<PermissionCatalogItemDto> getPermissionsCatalog() {
        return SystemPermissions.getPermissionsCatalog();
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetUserPassword(UUID callerId, UUID targetUserId, ResetPasswordRequest request) {
        if (request == null || request.resolvedPassword() == null || request.resolvedPassword().length() < 6) {
            throw new ValidationException("La contraseña temporal debe tener al menos 6 caracteres");
        }

        AppUser targetUser = appUserRepository.findById(targetUserId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", targetUserId));

        Optional<TenantContext> ctxOpt = TenantContextHolder.getContext();
        String callerRole;
        UUID callerTenantId;
        Set<UUID> callerBranches = Set.of();

        if (ctxOpt.isPresent()) {
            TenantContext ctx = ctxOpt.get();
            callerRole = ctx.role();
            callerTenantId = ctx.tenantId();
            callerBranches = ctx.authorizedBranchIds() != null ? ctx.authorizedBranchIds() : Set.of();
        } else if (callerId != null) {
            AppUser caller = appUserRepository.findById(callerId)
                .orElseThrow(() -> new UnauthorizedOperationException("Usuario solicitante no encontrado"));
            callerRole = caller.getRole();
            callerTenantId = caller.getTenantId();
        } else {
            throw new UnauthorizedOperationException("Se requiere sesión autenticada para resetear contraseñas");
        }

        if (!"SUPERADMIN".equalsIgnoreCase(callerRole) && !targetUser.getTenantId().equals(callerTenantId)) {
            throw new EntityNotFoundException("Usuario", targetUserId);
        }

        // Validación estricta de jerarquía (ADR-0010)
        if ("SUPERADMIN".equalsIgnoreCase(targetUser.getRole()) && !"SUPERADMIN".equalsIgnoreCase(callerRole)) {
            throw new UnauthorizedOperationException("No tiene privilegios para resetear la contraseña de un SUPERADMIN");
        }

        if ("BRANCH_MANAGER".equalsIgnoreCase(callerRole)) {
            if ("ADMIN_TENANT".equalsIgnoreCase(targetUser.getRole()) || "BRANCH_MANAGER".equalsIgnoreCase(targetUser.getRole())) {
                throw new UnauthorizedOperationException("Un Gerente de Sucursal no tiene jerarquía para resetear la contraseña de un " + targetUser.getRole());
            }
            if (!callerBranches.isEmpty() && targetUser.getBranchId() != null && !callerBranches.contains(targetUser.getBranchId())) {
                throw new UnauthorizedOperationException("El colaborador no pertenece a una sucursal bajo su supervisión");
            }
        } else if (!"ADMIN_TENANT".equalsIgnoreCase(callerRole) && !"SUPERADMIN".equalsIgnoreCase(callerRole)) {
            boolean hasWritePerm = ctxOpt.map(c -> c.hasPermission(SystemPermissions.USERS_WRITE)).orElse(false);
            if (!hasWritePerm || "ADMIN_TENANT".equalsIgnoreCase(targetUser.getRole())) {
                throw new UnauthorizedOperationException("No cuenta con jerarquía suficiente para resetear la contraseña de este usuario");
            }
        }

        targetUser.setPasswordHash(passwordEncoder.encode(request.resolvedPassword()));
        targetUser.setMustChangePassword(true);
        appUserRepository.save(targetUser);

        // Revocar sesiones activas del usuario objetivo por seguridad
        sessionPublicApi.revokeAllUserSessions(targetUserId);

        return new ResetPasswordResponse(targetUser.getId(), true, Instant.now());
    }

    @Override
    @Transactional
    public void changeOwnPassword(UUID userId, ChangePasswordRequest request) {
        if (request == null || request.newPassword() == null || request.newPassword().length() < 6) {
            throw new ValidationException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", userId));

        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);
    }

    @Override
    @Transactional
    public void assignUserBranches(UUID tenantId, UUID targetUserId, AssignBranchesRequest request) {
        AppUser user = appUserRepository.findByTenantIdAndId(tenantId, targetUserId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario", targetUserId));

        Set<UUID> branchIds = new LinkedHashSet<>();
        if (request != null && request.resolvedBranchIds() != null) {
            branchIds.addAll(request.resolvedBranchIds());
        }
        if (request != null && request.defaultBranchId() != null) {
            branchIds.add(request.defaultBranchId());
        }

        for (UUID bid : branchIds) {
            if (branchRepository.findByTenantIdAndId(tenantId, bid).isEmpty()) {
                throw new EntityNotFoundException("Sucursal", bid);
            }
        }

        UUID defaultBranch = (request != null && request.defaultBranchId() != null)
            ? request.defaultBranchId()
            : (!branchIds.isEmpty() ? branchIds.iterator().next() : user.getBranchId());

        user.setBranchId(defaultBranch);
        appUserRepository.save(user);

        userBranchRepository.deleteByUserId(targetUserId);
        userBranchRepository.flush();

        for (UUID bid : branchIds) {
            userBranchRepository.save(new UserBranch(tenantId, targetUserId, bid));
        }
    }

    private RoleDto toRoleDto(Role role) {
        Set<String> perms;
        if ("ADMIN_TENANT".equalsIgnoreCase(role.getCode()) || "SUPERADMIN".equalsIgnoreCase(role.getCode())) {
            perms = new LinkedHashSet<>(SystemPermissions.ALL_PERMISSIONS);
            perms.add("*");
        } else {
            perms = new LinkedHashSet<>(rolePermissionRepository.findPermissionsByRoleId(role.getId()));
        }
        return new RoleDto(
            role.getId(),
            role.getTenantId(),
            role.getCode(),
            role.getName(),
            role.getDescription(),
            role.isSystem(),
            perms
        );
    }

    private static String normalizeRoleCode(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "CUSTOM_ROLE" : normalized;
    }

    private static Set<String> sanitizePermissions(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        Set<String> clean = new LinkedHashSet<>();
        for (String p : permissions) {
            if (p != null && !p.isBlank() && !"*".equals(p.trim())) {
                clean.add(p.trim());
            }
        }
        return clean;
    }
}
