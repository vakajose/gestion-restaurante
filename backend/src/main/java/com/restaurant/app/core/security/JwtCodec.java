package com.restaurant.app.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Motor criptográfico stateless para emisión y verificación de JWT HS256 con validación de emisor y audiencia.
 */
@Component
public class JwtCodec {

    private static final Logger log = LoggerFactory.getLogger(JwtCodec.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String ENCODED_HEADER = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;

    public JwtCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String issue(Map<String, Object> customClaims, JwtSigningSpec spec) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(spec.expirationSeconds());

            Map<String, Object> claims = new LinkedHashMap<>(customClaims);
            claims.put("iss", spec.issuer());
            claims.put("aud", spec.audience());
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", expiration.getEpochSecond());

            byte[] payloadBytes = objectMapper.writeValueAsBytes(claims);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes);

            String signingInput = ENCODED_HEADER + "." + encodedPayload;
            String signature = sign(signingInput, spec.secretKeyBytes());

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Error al emitir token JWT", e);
        }
    }

    public Optional<JsonNode> verifyAndDecode(String token, JwtSigningSpec spec) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSig = computeHmac(signingInput, spec.secretKeyBytes());
            byte[] actualSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, actualSig)) {
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            long exp = payload.path("exp").asLong(0);
            if (exp <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            String issuer = payload.path("iss").asText("");
            String audience = payload.path("aud").asText("");
            if (!spec.issuer().equals(issuer) || !spec.audience().equals(audience)) {
                return Optional.empty();
            }

            return Optional.of(payload);
        } catch (Exception e) {
            log.debug("Validación JWT fallida: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String sign(String signingInput, byte[] secretKeyBytes)
        throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] sig = computeHmac(signingInput, secretKeyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    }

    private byte[] computeHmac(String data, byte[] secretKeyBytes)
        throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(keySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.US_ASCII));
    }
}
