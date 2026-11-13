package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
class PlatformAdminManagementService {

    private final PlatformAdminRepository adminRepository;
    private final PlatformRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAuditService auditService;

    PlatformAdminManagementService(
        PlatformAdminRepository adminRepository,
        PlatformRefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        PlatformAuditService auditService
    ) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    record PlatformAdminDto(
        UUID id,
        String username,
        String email,
        String fullName,
        PlatformRole role,
        boolean isActive,
        boolean mustChangePassword,
        Instant lastLoginAt,
        Instant createdAt
    ) {
        static PlatformAdminDto fromEntity(PlatformAdmin entity) {
            return new PlatformAdminDto(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getRole(),
                entity.isActive(),
                entity.isMustChangePassword(),
                entity.getLastLoginAt(),
                entity.getCreatedAt()
            );
        }
    }

    record CreatePlatformAdminRequest(
        String username,
        String email,
        String fullName,
        String initialPassword,
        PlatformRole role
    ) {
    }

    record UpdateAdminStatusRequest(
        Boolean isActive,
        Boolean active
    ) {
        boolean resolvedActive() {
            if (isActive != null) {
                return isActive;
            }
            if (active != null) {
                return active;
            }
            throw new ValidationException("Debe indicar el campo isActive");
        }
    }

    @Transactional(readOnly = true)
    public List<PlatformAdminDto> listAll() {
        return adminRepository.findAllByOrderByCreatedAtAsc()
            .stream()
            .map(PlatformAdminDto::fromEntity)
            .toList();
    }

    public PlatformAdminDto create(CreatePlatformAdminRequest request, PlatformPrincipal actor, String ipAddress) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        String username = requireNonBlank(request.username(), "username");
        String email = requireNonBlank(request.email(), "email").toLowerCase();
        String fullName = requireNonBlank(request.fullName(), "fullName");
        PlatformAuthService.validatePlatformPasswordPolicy(request.initialPassword());

        if (adminRepository.findByUsernameNormalized(username).isPresent()) {
            throw new ResourceConflictException("Ya existe un administrador de plataforma con el usuario '" + username + "'");
        }
        if (adminRepository.findByEmailNormalized(email).isPresent()) {
            throw new ResourceConflictException("Ya existe un administrador de plataforma con el email '" + email + "'");
        }

        PlatformRole targetRole = request.role() != null ? request.role() : PlatformRole.PLATFORM_OPERATOR;
        PlatformAdmin created = new PlatformAdmin(
            username,
            email,
            fullName,
            passwordEncoder.encode(request.initialPassword()),
            true,
            targetRole
        );
        PlatformAdmin saved = adminRepository.save(created);

        auditService.record(
            actor,
            "PLATFORM_ADMIN_CREATED",
            "PLATFORM_ADMIN",
            saved.getId().toString(),
            Map.of("username", saved.getUsername(), "email", saved.getEmail(), "role", saved.getRole().name()),
            ipAddress
        );
        return PlatformAdminDto.fromEntity(saved);
    }

    public PlatformAdminDto updateStatus(
        UUID targetAdminId,
        UpdateAdminStatusRequest request,
        PlatformPrincipal actor,
        String ipAddress
    ) {
        if (request == null) {
            throw new ValidationException("El cuerpo de solicitud es obligatorio");
        }
        boolean targetActive = request.resolvedActive();
        PlatformAdmin target = adminRepository.findById(targetAdminId)
            .orElseThrow(() -> new EntityNotFoundException("Administrador de plataforma", targetAdminId));

        if (!targetActive) {
            if (actor != null && target.getId().equals(actor.adminId())) {
                throw new BusinessRuleException("No puede desactivar su propia cuenta de plataforma");
            }
            if (target.isActive() && target.getRole() == PlatformRole.SUPERADMIN) {
                long activeSuperadmins = adminRepository.countByRoleAndIsActiveTrue(PlatformRole.SUPERADMIN);
                if (activeSuperadmins <= 1) {
                    throw new BusinessRuleException("No se puede desactivar al último SUPERADMIN activo de la plataforma");
                }
            }
        }

        target.setActive(targetActive);
        PlatformAdmin saved = adminRepository.save(target);

        if (!targetActive) {
            refreshTokenRepository.revokeAllActiveByAdminId(targetAdminId, Instant.now());
        }

        auditService.record(
            actor,
            targetActive ? "PLATFORM_ADMIN_ACTIVATED" : "PLATFORM_ADMIN_DEACTIVATED",
            "PLATFORM_ADMIN",
            saved.getId().toString(),
            Map.of("username", saved.getUsername(), "role", saved.getRole().name(), "isActive", targetActive),
            ipAddress
        );
        return PlatformAdminDto.fromEntity(saved);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El campo '" + fieldName + "' es obligatorio");
        }
        return value.trim();
    }
}
