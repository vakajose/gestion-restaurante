package com.restaurant.app.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TENANT_ISSUER = "restaurant-pos-erp";
    public static final String TENANT_AUDIENCE = "erp-tenant";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtCodec jwtCodec;
    private final JwtSigningSpec signingSpec;

    @Autowired
    public JwtService(
        JwtCodec jwtCodec,
        @Value("${restaurant.security.jwt.secret:default-dev-secret-key-must-be-at-least-32-characters-long!}") String secret,
        @Value("${restaurant.security.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.jwtCodec = jwtCodec;
        this.signingSpec = JwtSigningSpec.hmacSha256(secret, TENANT_ISSUER, TENANT_AUDIENCE, expirationSeconds);
    }

    public JwtService(ObjectMapper objectMapper, String secret, long expirationSeconds) {
        this(new JwtCodec(objectMapper), secret, expirationSeconds);
    }

    public String generateToken(AppUser user) {
        return generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId(), user.getBranchId(), null, Collections.emptySet(), Collections.emptySet());
    }

    public String generateToken(AppUser user, String tenantSlug, Collection<String> permissions) {
        return generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId(), user.getBranchId(), tenantSlug, permissions, Collections.emptySet());
    }

    public String generateToken(
        AppUser user,
        String tenantSlug,
        Collection<String> permissions,
        Collection<UUID> authorizedBranchIds
    ) {
        return generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId(), user.getBranchId(), tenantSlug, permissions, authorizedBranchIds);
    }

    public String generateToken(UUID userId, String username, String role, UUID tenantId, UUID branchId) {
        return generateToken(userId, username, role, tenantId, branchId, null, Collections.emptySet(), Collections.emptySet());
    }

    public String generateToken(
        UUID userId,
        String username,
        String role,
        UUID tenantId,
        UUID branchId,
        String tenantSlug,
        Collection<String> permissions
    ) {
        return generateToken(userId, username, role, tenantId, branchId, tenantSlug, permissions, Collections.emptySet());
    }

    public String generateToken(
        UUID userId,
        String username,
        String role,
        UUID tenantId,
        UUID branchId,
        String tenantSlug,
        Collection<String> permissions,
        Collection<UUID> authorizedBranchIds
    ) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", userId.toString());
        claims.put("username", username);
        claims.put("role", role);
        claims.put("tenant_id", tenantId.toString());
        claims.put("tenant_slug", tenantSlug);
        claims.put("branch_id", branchId != null ? branchId.toString() : null);
        claims.put("permissions", permissions != null ? permissions : Collections.emptyList());
        claims.put(
            "branch_ids",
            authorizedBranchIds != null
                ? authorizedBranchIds.stream().map(UUID::toString).toList()
                : Collections.emptyList()
        );
        return jwtCodec.issue(claims, signingSpec);
    }

    public boolean validateToken(String token) {
        return jwtCodec.verifyAndDecode(token, signingSpec).isPresent();
    }

    public Optional<TenantContext> extractTenantContext(String token) {
        Optional<JsonNode> payloadOpt = jwtCodec.verifyAndDecode(token, signingSpec);
        if (payloadOpt.isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonNode payload = payloadOpt.get();
            UUID userId = UUID.fromString(payload.path("sub").asText());
            String username = payload.path("username").asText();
            String role = payload.path("role").asText();
            UUID tenantId = UUID.fromString(payload.path("tenant_id").asText());

            String tenantSlug = null;
            JsonNode slugNode = payload.path("tenant_slug");
            if (!slugNode.isNull() && !slugNode.isMissingNode() && !slugNode.asText("").isBlank()) {
                tenantSlug = slugNode.asText();
            }

            JsonNode branchIdNode = payload.path("branch_id");
            UUID branchId = null;
            if (!branchIdNode.isNull() && !branchIdNode.isMissingNode() && !branchIdNode.asText("").isBlank()) {
                branchId = UUID.fromString(branchIdNode.asText());
            }

            Set<String> permissions = new HashSet<>();
            JsonNode permsNode = payload.path("permissions");
            if (permsNode != null && permsNode.isArray()) {
                for (JsonNode p : permsNode) {
                    permissions.add(p.asText());
                }
            }

            Set<UUID> authorizedBranchIds = new HashSet<>();
            JsonNode branchIdsNode = payload.path("branch_ids");
            if (branchIdsNode != null && branchIdsNode.isArray()) {
                for (JsonNode b : branchIdsNode) {
                    try {
                        authorizedBranchIds.add(UUID.fromString(b.asText()));
                    } catch (IllegalArgumentException ignored) {
                        // ignore malformed UUID
                    }
                }
            }
            if (branchId != null) {
                authorizedBranchIds.add(branchId);
            }

            return Optional.of(new TenantContext(
                tenantId,
                branchId,
                userId,
                username,
                role,
                tenantSlug,
                permissions,
                authorizedBranchIds
            ));
        } catch (Exception e) {
            log.error("Failed to extract TenantContext from valid token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
