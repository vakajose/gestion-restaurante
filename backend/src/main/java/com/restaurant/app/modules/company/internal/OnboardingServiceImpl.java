package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.JwtService;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.UserDto;
import com.restaurant.app.core.security.api.CreateUserRequest;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.modules.company.api.OnboardingPublicApi;
import com.restaurant.app.modules.company.api.TenantOnboardedEvent;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
class OnboardingServiceImpl implements OnboardingPublicApi {

    private static final Logger log = LoggerFactory.getLogger(OnboardingServiceImpl.class);
    private static final String DEFAULT_TIMEZONE = "America/La_Paz";
    private static final String DEFAULT_CURRENCY = "BOB";
    private static final String ONBOARDING_AUDIT_REASON = "Configuración inicial de Onboarding";

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final UserPublicApi userPublicApi;
    private final SessionPublicApi sessionPublicApi;
    private final ConfigurationPublicApi configurationPublicApi;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;

    OnboardingServiceImpl(
        TenantRepository tenantRepository,
        BranchRepository branchRepository,
        UserPublicApi userPublicApi,
        SessionPublicApi sessionPublicApi,
        ConfigurationPublicApi configurationPublicApi,
        ApplicationEventPublisher eventPublisher,
        JwtService jwtService
    ) {
        this.tenantRepository = tenantRepository;
        this.branchRepository = branchRepository;
        this.userPublicApi = userPublicApi;
        this.sessionPublicApi = sessionPublicApi;
        this.configurationPublicApi = configurationPublicApi;
        this.eventPublisher = eventPublisher;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckAvailabilityResultDto checkSlugAvailability(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("El identificador slug a consultar no puede estar vacío");
        }
        String normalized = Tenant.slugify(slug);
        boolean exists = tenantRepository.findBySlug(normalized).isPresent();
        if (!exists) {
            return new CheckAvailabilityResultDto(true, normalized, normalized, true, true);
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        String candidate = normalized + "-" + suffix;
        while (tenantRepository.findBySlug(candidate).isPresent()) {
            suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            candidate = normalized + "-" + suffix;
        }
        return new CheckAvailabilityResultDto(false, candidate, candidate, false, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckAvailabilityResultDto checkNitAvailability(String nit) {
        if (nit == null || nit.isBlank()) {
            return new CheckAvailabilityResultDto(true, null, null, true, true);
        }
        String trimmed = nit.trim();
        boolean exists = tenantRepository.findByNitOrTaxId(trimmed).isPresent();
        return new CheckAvailabilityResultDto(!exists, null, null, true, !exists);
    }

    @Override
    @Transactional
    public OnboardingResultDto executeOnboarding(OnboardingRequest request) {
        validateRequest(request);

        String legalName = request.company().legalName().trim();
        String rawSlug = (request.company().slug() != null && !request.company().slug().isBlank())
            ? request.company().slug().trim()
            : legalName;
        String slug = Tenant.slugify(rawSlug);

        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new ResourceConflictException("El identificador slug '" + slug + "' ya se encuentra registrado por otra empresa.");
        }

        String nit = (request.company().nitOrTaxId() != null && !request.company().nitOrTaxId().isBlank())
            ? request.company().nitOrTaxId().trim()
            : null;
        if (nit != null && tenantRepository.findByNitOrTaxId(nit).isPresent()) {
            throw new ResourceConflictException("El NIT o identificación fiscal '" + nit + "' ya se encuentra registrado.");
        }

        String logoUrl = (request.company().logoUrl() != null && !request.company().logoUrl().isBlank())
            ? request.company().logoUrl().trim()
            : null;

        // 1. Persistir Tenant
        Tenant tenant = new Tenant(legalName, nit, slug, logoUrl);
        tenant = tenantRepository.save(tenant);

        // 2. Persistir Branch Matriz
        String timezone = validateTimezone(request.branch().timezone());
        String currency = (request.branch().currency() != null && !request.branch().currency().isBlank())
            ? request.branch().currency().trim().toUpperCase()
            : DEFAULT_CURRENCY;

        Branch branch = new Branch(tenant.getId(), request.branch().name().trim(), timezone);
        branch.setAddress(request.branch().address() != null && !request.branch().address().isBlank()
            ? request.branch().address().trim()
            : null);
        branch.setPhone(request.branch().phone() != null && !request.branch().phone().isBlank()
            ? request.branch().phone().trim()
            : null);
        branch.setCurrency(currency);
        if (request.branch().latitude() != null) {
            branch.setLatitude(BigDecimal.valueOf(request.branch().latitude()));
        }
        if (request.branch().longitude() != null) {
            branch.setLongitude(BigDecimal.valueOf(request.branch().longitude()));
        }
        branch = branchRepository.save(branch);

        // 3. Semillar roles base del sistema para el nuevo Tenant
        userPublicApi.seedDefaultRolesForTenant(tenant.getId());

        // 4. Crear el usuario Administrador Propietario con hash Argon2id
        UserSummaryDto adminUser = userPublicApi.createUser(
            tenant.getId(),
            new CreateUserRequest(
                request.adminUser().username().trim(),
                request.adminUser().fullName(),
                request.adminUser().email(),
                request.adminUser().password(),
                "ADMIN_TENANT",
                branch.getId()
            )
        );

        // 5. Aplicar Presets Operativos (Sparse Storage - ADR-0008 / ADR-0009)
        String template = (request.businessTemplate() != null && !request.businessTemplate().isBlank())
            ? request.businessTemplate().trim().toUpperCase()
            : "EMPTY";

        applyOperationalPresets(tenant.getId(), adminUser.id(), template, currency, timezone);

        // 6. Semillar Catálogo Demo si fue solicitado y no es plantilla vacía
        if (request.shouldIncludeDemoCatalog() && !"EMPTY".equals(template)) {
            eventPublisher.publishEvent(new TenantOnboardedEvent(tenant.getId(), branch.getId(), template));
        }

        // 7. Emitir sesión JWT "Ready to Sell"
        Set<String> permissions = new LinkedHashSet<>(SystemPermissions.ALL_PERMISSIONS);
        permissions.add("*");
        Set<UUID> authorizedBranchIds = Set.of(branch.getId());

        String accessToken = jwtService.generateToken(
            adminUser.id(),
            adminUser.username(),
            adminUser.role(),
            tenant.getId(),
            branch.getId(),
            tenant.getSlug(),
            permissions,
            authorizedBranchIds
        );

        String refreshToken = sessionPublicApi.issueRefreshToken(
            tenant.getId(),
            adminUser.id(),
            "127.0.0.1",
            "Onboarding Wizard"
        );

        UserDto userDto = new UserDto(
            adminUser.id(),
            adminUser.username(),
            adminUser.fullName(),
            adminUser.email(),
            adminUser.role(),
            tenant.getId(),
            branch.getId(),
            tenant.getSlug(),
            permissions,
            authorizedBranchIds,
            null,
            false
        );

        log.info("Onboarding completado exitosamente para empresa '{}' (slug={}, tenantId={}, branchId={}, template={})",
            legalName, slug, tenant.getId(), branch.getId(), template);

        return new OnboardingResultDto(
            tenant.getId(),
            branch.getId(),
            adminUser.id(),
            tenant.getSlug(),
            accessToken,
            accessToken,
            refreshToken,
            userDto
        );
    }

    private void applyOperationalPresets(
        UUID tenantId,
        UUID adminUserId,
        String template,
        String currency,
        String timezone
    ) {
        configurationPublicApi.setOverride(
            SystemParameter.COMPANY_CURRENCY,
            ParameterScope.TENANT,
            tenantId,
            null,
            currency,
            adminUserId,
            ONBOARDING_AUDIT_REASON
        );
        configurationPublicApi.setOverride(
            SystemParameter.COMPANY_TIMEZONE,
            ParameterScope.TENANT,
            tenantId,
            null,
            timezone,
            adminUserId,
            ONBOARDING_AUDIT_REASON
        );

        switch (template) {
            case "FAST_FOOD" -> {
                configurationPublicApi.setOverride(
                    SystemParameter.INVENTORY_DEDUCTION_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "AUTOMATIC_PERMISSIVE",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.POS_DEFAULT_ORDER_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "TAKEOUT",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.POS_DISPLAY_DISH_IMAGES,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "true",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "120",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
            }
            case "TRADITIONAL" -> {
                configurationPublicApi.setOverride(
                    SystemParameter.INVENTORY_DEDUCTION_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "AUTOMATIC_STRICT",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.POS_DEFAULT_ORDER_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "DINE_IN",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.POS_DISPLAY_DISH_IMAGES,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "true",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.FINANCE_REQUIRE_SUPERVISOR_PIN_CANCEL,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "true",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "60",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
            }
            case "CAFE", "BAR", "CAFE_BAR" -> {
                configurationPublicApi.setOverride(
                    SystemParameter.INVENTORY_DEDUCTION_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "AUTOMATIC_PERMISSIVE",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.POS_DEFAULT_ORDER_MODE,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "DINE_IN",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.FINANCE_REQUIRE_SUPERVISOR_PIN_CANCEL,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "false",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
                configurationPublicApi.setOverride(
                    SystemParameter.SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES,
                    ParameterScope.TENANT,
                    tenantId,
                    null,
                    "120",
                    adminUserId,
                    ONBOARDING_AUDIT_REASON
                );
            }
            default -> {
                // Plantilla vacía o personalizada: conserva defaults
            }
        }
    }

    private void validateRequest(OnboardingRequest request) {
        if (request == null) {
            throw new ValidationException("La solicitud de onboarding no puede ser nula");
        }
        if (request.company() == null || request.company().legalName() == null || request.company().legalName().isBlank()) {
            throw new ValidationException("El nombre comercial o razón social de la empresa es obligatorio");
        }
        if (request.branch() == null || request.branch().name() == null || request.branch().name().isBlank()) {
            throw new ValidationException("El nombre de la sucursal principal es obligatorio");
        }
        if (request.adminUser() == null || request.adminUser().username() == null || request.adminUser().username().isBlank()) {
            throw new ValidationException("El nombre de usuario del administrador es obligatorio");
        }
        if (request.adminUser().username().trim().length() < 3) {
            throw new ValidationException("El nombre de usuario debe tener al menos 3 caracteres");
        }
        if (request.adminUser().password() == null || request.adminUser().password().length() < 6) {
            throw new ValidationException("La contraseña del administrador debe tener al menos 6 caracteres");
        }
    }

    private String validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        try {
            ZoneId.of(timezone.trim());
            return timezone.trim();
        } catch (Exception e) {
            throw new ValidationException("Zona horaria IANA inválida: " + timezone);
        }
    }
}
