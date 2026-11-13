package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.modules.backoffice.internal.PlatformAdminManagementService.CreatePlatformAdminRequest;
import com.restaurant.app.modules.backoffice.internal.PlatformAdminManagementService.PlatformAdminDto;
import com.restaurant.app.modules.backoffice.internal.PlatformAdminManagementService.UpdateAdminStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/admins")
@PreAuthorize("hasRole('SUPERADMIN')")
class PlatformAdminController {

    private final PlatformAdminManagementService adminManagementService;

    PlatformAdminController(PlatformAdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping
    ResponseEntity<List<PlatformAdminDto>> listAll() {
        return ResponseEntity.ok(adminManagementService.listAll());
    }

    @PostMapping
    ResponseEntity<PlatformAdminDto> create(
        @RequestBody CreatePlatformAdminRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        PlatformAdminDto created = adminManagementService.create(
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{adminId}/status")
    ResponseEntity<PlatformAdminDto> updateStatus(
        @PathVariable UUID adminId,
        @RequestBody UpdateAdminStatusRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(
            adminManagementService.updateStatus(
                adminId,
                request,
                actor,
                PlatformAuditService.resolveClientIp(servletRequest)
            )
        );
    }
}
