package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.modules.backoffice.internal.PlatformAuthService.ChangePasswordRequest;
import com.restaurant.app.modules.backoffice.internal.PlatformAuthService.LoginRequest;
import com.restaurant.app.modules.backoffice.internal.PlatformAuthService.LogoutRequest;
import com.restaurant.app.modules.backoffice.internal.PlatformAuthService.PlatformAuthResponse;
import com.restaurant.app.modules.backoffice.internal.PlatformAuthService.RefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/auth")
class PlatformAuthController {

    private final PlatformAuthService platformAuthService;

    PlatformAuthController(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    @PostMapping("/login")
    ResponseEntity<PlatformAuthResponse> login(
        @RequestBody LoginRequest request,
        HttpServletRequest servletRequest
    ) {
        PlatformAuthResponse response = platformAuthService.login(
            request,
            PlatformAuditService.resolveClientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    ResponseEntity<PlatformAuthResponse> refresh(
        @RequestBody RefreshRequest request,
        HttpServletRequest servletRequest
    ) {
        PlatformAuthResponse response = platformAuthService.refresh(
            request,
            PlatformAuditService.resolveClientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) LogoutRequest request) {
        platformAuthService.logout(request != null ? request.refreshToken() : null);
        return ResponseEntity.ok(Map.of("message", "Sesión de plataforma cerrada exitosamente"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<PlatformPrincipal> changeOwnPassword(
        @AuthenticationPrincipal PlatformPrincipal principal,
        @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(platformAuthService.changeOwnPassword(principal.adminId(), request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<PlatformPrincipal> me(@AuthenticationPrincipal PlatformPrincipal principal) {
        return ResponseEntity.ok(platformAuthService.getProfile(principal.adminId()));
    }
}
