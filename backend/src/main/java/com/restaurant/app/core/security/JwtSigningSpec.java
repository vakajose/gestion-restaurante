package com.restaurant.app.core.security;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Value Object que parametriza firma, emisor, audiencia y TTL de un contexto JWT.
 */
public record JwtSigningSpec(
    byte[] secretKeyBytes,
    String issuer,
    String audience,
    long expirationSeconds
) {
    public JwtSigningSpec {
        Objects.requireNonNull(secretKeyBytes, "secretKeyBytes no puede ser nulo");
        if (secretKeyBytes.length < 32) {
            throw new IllegalArgumentException("El secreto JWT debe tener al menos 32 bytes");
        }
        Objects.requireNonNull(issuer, "issuer no puede ser nulo");
        Objects.requireNonNull(audience, "audience no puede ser nulo");
    }

    public static JwtSigningSpec hmacSha256(String secret, String issuer, String audience, long expirationSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("El secreto JWT no puede estar vacío");
        }
        return new JwtSigningSpec(
            secret.getBytes(StandardCharsets.UTF_8),
            issuer,
            audience,
            expirationSeconds
        );
    }
}
