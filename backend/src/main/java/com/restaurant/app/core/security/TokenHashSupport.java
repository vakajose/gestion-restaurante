package com.restaurant.app.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utilidad compartida para generación de refresh tokens opacos y su huella SHA-256.
 */
public final class TokenHashSupport {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashSupport() {
    }

    public static String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String computeSha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en JVM", e);
        }
    }

    public static String truncate(String value, String fallback, int maxLength) {
        String effective = (value == null || value.isBlank()) ? fallback : value.trim();
        return effective.length() > maxLength ? effective.substring(0, maxLength) : effective;
    }
}
