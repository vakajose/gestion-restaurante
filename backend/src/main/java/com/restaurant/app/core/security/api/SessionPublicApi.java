package com.restaurant.app.core.security.api;

import com.restaurant.app.core.security.UserDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionPublicApi {

    record RefreshRequest(String refreshToken) {}

    record LogoutRequest(String refreshToken) {}

    record RefreshResponse(
        String accessToken,
        String token,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user
    ) {
        public RefreshResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
            this(accessToken, accessToken, refreshToken, tokenType, expiresInSeconds, null);
        }
    }

    record ActiveSessionDto(
        UUID id,
        String deviceInfo,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt,
        boolean isCurrent
    ) {}

    String issueRefreshToken(UUID tenantId, UUID userId, String ipAddress, String userAgent);

    RefreshResponse refreshSession(RefreshRequest request, String ipAddress, String userAgent);

    void revokeCurrentSession(String refreshToken);

    void revokeSessionById(UUID userId, UUID sessionId);

    void revokeAllUserSessions(UUID userId);

    List<ActiveSessionDto> listActiveSessions(UUID userId, String currentRefreshToken);
}
