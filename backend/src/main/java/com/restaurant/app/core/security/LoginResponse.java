package com.restaurant.app.core.security;

public record LoginResponse(
    String token,
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserDto user
) {
    public LoginResponse(String token, String tokenType, UserDto user) {
        this(token, token, null, tokenType, 900L, user);
    }

    public static LoginResponse bearer(String token, UserDto user) {
        return new LoginResponse(token, token, null, "Bearer", 900L, user);
    }

    public static LoginResponse bearer(String token, String refreshToken, long expiresInSeconds, UserDto user) {
        return new LoginResponse(token, token, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
