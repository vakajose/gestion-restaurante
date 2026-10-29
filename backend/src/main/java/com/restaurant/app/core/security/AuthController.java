package com.restaurant.app.core.security;

import com.restaurant.app.core.security.api.RolePublicApi;
import com.restaurant.app.core.security.api.RolePublicApi.ChangePasswordRequest;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.core.security.api.SessionPublicApi.ActiveSessionDto;
import com.restaurant.app.core.security.api.SessionPublicApi.LogoutRequest;
import com.restaurant.app.core.security.api.SessionPublicApi.RefreshRequest;
import com.restaurant.app.core.security.api.SessionPublicApi.RefreshResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionPublicApi sessionPublicApi;
    private final RolePublicApi rolePublicApi;

    public AuthController(
        AuthService authService,
        SessionPublicApi sessionPublicApi,
        RolePublicApi rolePublicApi
    ) {
        this.authService = authService;
        this.sessionPublicApi = sessionPublicApi;
        this.rolePublicApi = rolePublicApi;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest
    ) {
        String ipAddress = resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
        @Valid @RequestBody RefreshRequest request,
        HttpServletRequest servletRequest
    ) {
        String ipAddress = resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        RefreshResponse response = sessionPublicApi.refreshSession(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
        @RequestBody(required = false) LogoutRequest request
    ) {
        String refreshToken = request != null ? request.refreshToken() : null;
        sessionPublicApi.revokeCurrentSession(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, String>> revokeAllSessions() {
        UUID userId = TenantContextHolder.getUserId();
        sessionPublicApi.revokeAllUserSessions(userId);
        return ResponseEntity.ok(Map.of("message", "Todas las sesiones activas han sido revocadas"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ActiveSessionDto>> listActiveSessions(
        @RequestParam(value = "currentRefreshToken", required = false) String currentRefreshToken,
        @RequestHeader(value = "X-Refresh-Token", required = false) String headerRefreshToken
    ) {
        UUID userId = TenantContextHolder.getUserId();
        String tokenToMatch = currentRefreshToken != null ? currentRefreshToken : headerRefreshToken;
        return ResponseEntity.ok(sessionPublicApi.listActiveSessions(userId, tokenToMatch));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable("sessionId") UUID sessionId) {
        UUID userId = TenantContextHolder.getUserId();
        sessionPublicApi.revokeSessionById(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changeOwnPassword(
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = TenantContextHolder.getUserId();
        rolePublicApi.changeOwnPassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        problemDetail.setTitle("Authentication Failure");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
