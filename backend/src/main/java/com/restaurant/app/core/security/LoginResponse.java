package com.restaurant.app.core.security;

public record LoginResponse(
    String token,
    String tokenType,
    UserDto user
) {
    public static LoginResponse bearer(String token, UserDto user) {
        return new LoginResponse(token, "Bearer", user);
    }
}
