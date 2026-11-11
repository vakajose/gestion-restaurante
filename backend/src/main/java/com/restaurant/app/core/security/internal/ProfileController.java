package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.security.AuthService;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.UserDto;
import com.restaurant.app.core.security.api.ChangePasswordRequest;
import com.restaurant.app.core.security.api.UpdateProfileRequest;
import com.restaurant.app.core.security.api.UserPublicApi;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController {

    private final AuthService authService;
    private final UserPublicApi userPublicApi;

    ProfileController(AuthService authService, UserPublicApi userPublicApi) {
        this.authService = authService;
        this.userPublicApi = userPublicApi;
    }

    @GetMapping
    public ResponseEntity<UserDto> getProfile() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PutMapping
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = TenantContextHolder.getUserId();
        userPublicApi.updateProfile(tenantId, userId, request);
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PutMapping("/password")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = TenantContextHolder.getUserId();
        userPublicApi.changePassword(tenantId, userId, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }

    @PutMapping("/pin")
    @PostMapping("/pin")
    public ResponseEntity<Map<String, String>> updatePin(@Valid @RequestBody com.restaurant.app.core.security.api.UpdatePinRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = TenantContextHolder.getUserId();
        userPublicApi.updatePin(tenantId, userId, request.pin());
        return ResponseEntity.ok(Map.of("message", "PIN de supervisor actualizado exitosamente"));
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
}
