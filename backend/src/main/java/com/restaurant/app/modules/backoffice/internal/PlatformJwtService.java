package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.security.JwtCodec;
import com.restaurant.app.core.security.JwtSigningSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
class PlatformJwtService {

    static final String PLATFORM_ISSUER = "restaurant-platform";
    static final String PLATFORM_AUDIENCE = "platform-backoffice";

    private static final Logger log = LoggerFactory.getLogger(PlatformJwtService.class);

    private final JwtCodec jwtCodec;
    private final JwtSigningSpec signingSpec;
    private final long expirationSeconds;

    @Autowired
    PlatformJwtService(
        JwtCodec jwtCodec,
        @Value("${restaurant.security.platform-jwt.secret:${restaurant.security.jwt.secret:default-dev-secret-key-must-be-at-least-32-characters-long!}}") String secret,
        @Value("${restaurant.security.platform-jwt.expiration-seconds:900}") long expirationSeconds
    ) {
        this.jwtCodec = jwtCodec;
        this.signingSpec = JwtSigningSpec.hmacSha256(secret, PLATFORM_ISSUER, PLATFORM_AUDIENCE, expirationSeconds);
        this.expirationSeconds = expirationSeconds;
    }

    PlatformJwtService(ObjectMapper objectMapper, String secret, long expirationSeconds) {
        this(new JwtCodec(objectMapper), secret, expirationSeconds);
    }

    String generateToken(PlatformAdmin admin) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", admin.getId().toString());
        claims.put("username", admin.getUsername());
        claims.put("email", admin.getEmail());
        claims.put("full_name", admin.getFullName());
        claims.put("role", admin.getRole().name());
        claims.put("scope", "PLATFORM");
        claims.put("must_change_password", admin.isMustChangePassword());
        return jwtCodec.issue(claims, signingSpec);
    }

    Optional<PlatformPrincipal> extractPrincipal(String token) {
        Optional<JsonNode> payloadOpt = jwtCodec.verifyAndDecode(token, signingSpec);
        if (payloadOpt.isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonNode payload = payloadOpt.get();
            if (!"PLATFORM".equals(payload.path("scope").asText(""))) {
                return Optional.empty();
            }

            String roleText = payload.path("role").asText("");
            PlatformRole role;
            if ("SUPERADMIN".equals(roleText)) {
                role = PlatformRole.SUPERADMIN;
            } else if ("PLATFORM_OPERATOR".equals(roleText)) {
                role = PlatformRole.PLATFORM_OPERATOR;
            } else {
                return Optional.empty();
            }

            UUID adminId = UUID.fromString(payload.path("sub").asText());
            String username = payload.path("username").asText();
            String email = payload.path("email").asText();
            String fullName = payload.path("full_name").asText();
            boolean mustChangePassword = payload.path("must_change_password").asBoolean(false);

            return Optional.of(new PlatformPrincipal(adminId, username, email, fullName, role, mustChangePassword));
        } catch (Exception e) {
            log.debug("No se pudo extraer PlatformPrincipal del token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    long getExpirationSeconds() {
        return expirationSeconds;
    }
}
