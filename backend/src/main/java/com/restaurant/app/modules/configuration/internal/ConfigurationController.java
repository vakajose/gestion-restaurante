package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.SystemParameterDto;
import com.restaurant.app.modules.configuration.api.UpdateParameterRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parameters")
class ConfigurationController {

    private final ConfigurationServiceImpl configurationService;

    ConfigurationController(ConfigurationServiceImpl configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings:read') or hasAnyRole('ADMIN_TENANT', 'BRANCH_MANAGER', 'SUPERADMIN')")
    public ResponseEntity<List<SystemParameterDto>> getParameters(
        @RequestParam(required = false, defaultValue = "BRANCH") ParameterScope scope,
        @RequestParam(required = false) ParameterCategory category,
        @RequestParam(required = false) UUID branchId,
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        TenantContext context = TenantContextHolder.getContext().orElse(null);
        UUID tenantId = context != null ? context.tenantId() : null;
        UUID effectiveBranchId = branchId != null ? branchId : (context != null ? context.branchId() : null);

        List<SystemParameterDto> parameters = configurationService.getParameters(
            scope,
            category,
            effectiveBranchId,
            tenantId
        );

        String eTag = generateETag(parameters);

        if (ifNoneMatch != null && matchesETag(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(eTag)
                .build();
        }

        return ResponseEntity.ok()
            .eTag(eTag)
            .body(parameters);
    }

    @PatchMapping("/{key}")
    @PreAuthorize("hasAuthority('settings:write') or hasAnyRole('ADMIN_TENANT', 'BRANCH_MANAGER', 'SUPERADMIN')")
    public ResponseEntity<SystemParameterDto> updateParameter(
        @PathVariable String key,
        @RequestBody UpdateParameterRequest request
    ) {
        TenantContext context = TenantContextHolder.getContext().orElse(null);
        UUID tenantId = context != null ? context.tenantId() : null;
        UUID currentUserId = context != null ? context.userId() : null;
        String currentUsername = context != null ? context.username() : null;
        String currentTenantSlug = context != null ? context.tenantSlug() : null;

        UUID targetBranchId = request.branchId() != null
            ? request.branchId()
            : (context != null ? context.branchId() : null);

        UpdateParameterRequest effectiveRequest = new UpdateParameterRequest(
            request.scope() != null ? request.scope() : ParameterScope.BRANCH,
            targetBranchId,
            request.value(),
            request.passwordConfirmation(),
            request.auditReason()
        );

        SystemParameterDto updated = configurationService.updateParameterFromRequest(
            key,
            effectiveRequest,
            tenantId,
            currentUserId,
            currentUsername,
            currentTenantSlug
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('settings:write') or hasAnyRole('ADMIN_TENANT', 'BRANCH_MANAGER', 'SUPERADMIN')")
    public ResponseEntity<Void> resetParameter(
        @PathVariable String key,
        @RequestParam(required = false, defaultValue = "BRANCH") ParameterScope scope,
        @RequestParam(required = false) UUID branchId
    ) {
        TenantContext context = TenantContextHolder.getContext().orElse(null);
        UUID tenantId = context != null ? context.tenantId() : null;
        UUID effectiveBranchId = branchId != null ? branchId : (context != null ? context.branchId() : null);

        configurationService.resetOverride(key, scope, tenantId, effectiveBranchId);
        return ResponseEntity.noContent().build();
    }

    private String generateETag(List<SystemParameterDto> parameters) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (SystemParameterDto param : parameters) {
                md.update(param.key().getBytes(StandardCharsets.UTF_8));
                md.update((param.effectiveValue() != null ? param.effectiveValue() : "").getBytes(StandardCharsets.UTF_8));
                md.update(param.inheritedFrom().name().getBytes(StandardCharsets.UTF_8));
                if (param.updatedAt() != null) {
                    md.update(param.updatedAt().toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            return "\"" + HexFormat.of().formatHex(md.digest()) + "\"";
        } catch (NoSuchAlgorithmException e) {
            return "\"" + Integer.toHexString(parameters.hashCode()) + "\"";
        }
    }

    private boolean matchesETag(String ifNoneMatch, String eTag) {
        if (ifNoneMatch == null || eTag == null) {
            return false;
        }
        String cleanIfNoneMatch = ifNoneMatch.trim();
        String cleanETag = eTag.trim();
        if (cleanIfNoneMatch.startsWith("W/")) {
            cleanIfNoneMatch = cleanIfNoneMatch.substring(2);
        }
        if (cleanETag.startsWith("W/")) {
            cleanETag = cleanETag.substring(2);
        }
        return cleanIfNoneMatch.equals(cleanETag) || cleanIfNoneMatch.equals("*");
    }
}
