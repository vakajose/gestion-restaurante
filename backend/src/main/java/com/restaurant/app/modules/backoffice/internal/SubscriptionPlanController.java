package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.modules.backoffice.internal.SubscriptionPlanService.CreatePlanRequest;
import com.restaurant.app.modules.backoffice.internal.SubscriptionPlanService.PlanStatusRequest;
import com.restaurant.app.modules.backoffice.internal.SubscriptionPlanService.SubscriptionPlanDto;
import com.restaurant.app.modules.backoffice.internal.SubscriptionPlanService.UpdatePlanRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/plans")
class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<List<SubscriptionPlanDto>> listAll() {
        return ResponseEntity.ok(planService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<SubscriptionPlanDto> create(
        @RequestBody CreatePlanRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        SubscriptionPlanDto created = planService.create(
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<SubscriptionPlanDto> update(
        @PathVariable UUID planId,
        @RequestBody UpdatePlanRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(
            planService.update(planId, request, actor, PlatformAuditService.resolveClientIp(servletRequest))
        );
    }

    @PatchMapping("/{planId}/status")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<SubscriptionPlanDto> updateStatus(
        @PathVariable UUID planId,
        @RequestBody PlanStatusRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(
            planService.updateStatus(planId, request, actor, PlatformAuditService.resolveClientIp(servletRequest))
        );
    }
}
