package com.restaurant.app.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
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

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secretKeyBytes;
    private final long expirationSeconds;
    private final String encodedHeader;

    public JwtService(
        ObjectMapper objectMapper,
        @Value("${restaurant.security.jwt.secret:default-dev-secret-key-must-be-at-least-32-characters-long!}") String secret,
        @Value("${restaurant.security.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;

        // Base64URL encoded header: {"alg":"HS256","typ":"JWT"}
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        this.encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AppUser user) {
        return generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId(), user.getBranchId(), null, Collections.emptySet());
    }

    public String generateToken(AppUser user, String tenantSlug, Collection<String> permissions) {
        return generateToken(user.getId(), user.getUsername(), user.getRole(), user.getTenantId(), user.getBranchId(), tenantSlug, permissions);
    }

    public String generateToken(UUID userId, String username, String role, UUID tenantId, UUID branchId) {
        return generateToken(userId, username, role, tenantId, branchId, null, Collections.emptySet());
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
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(expirationSeconds);

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", userId.toString());
            claims.put("username", username);
            claims.put("role", role);
            claims.put("tenant_id", tenantId.toString());
            claims.put("tenant_slug", tenantSlug);
            if (branchId != null) {
                claims.put("branch_id", branchId.toString());
            } else {
                claims.put("branch_id", null);
            }
            claims.put("permissions", permissions != null ? permissions : Collections.emptyList());
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", expiration.getEpochSecond());

            byte[] payloadBytes = objectMapper.writeValueAsBytes(claims);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);

            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signingInput);

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSig = computeHmac(signingInput);
            byte[] actualSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, actualSig)) {
                return false;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            long exp = payload.path("exp").asLong(0);
            return exp > Instant.now().getEpochSecond();
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Optional<TenantContext> extractTenantContext(String token) {
        if (!validateToken(token)) {
            return Optional.empty();
        }

        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

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

            return Optional.of(new TenantContext(tenantId, branchId, userId, username, role, tenantSlug, permissions));
        } catch (Exception e) {
            log.error("Failed to extract TenantContext from valid token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String sign(String signingInput) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] sig = computeHmac(signingInput);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    }

    private byte[] computeHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(keySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
    }
}
