package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.AssignPlanRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.ExpirePolicyRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.GrantGraceRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.PaymentRecordResultDto;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.ReactivateRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.RecordPaymentRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.SubscriptionChargeDto;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.SubscriptionPaymentDto;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.SuspendTenantRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.TenantChargesSummaryDto;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.UpdateQuotasRequest;
import com.restaurant.app.modules.backoffice.internal.TenantSubscriptionService.WaiveChargeRequest;
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
@RequestMapping("/api/v1/backoffice")
class TenantSubscriptionController {

    private final TenantSubscriptionService subscriptionService;

    TenantSubscriptionController(TenantSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/tenants/{tenantId}/subscription/assign-plan")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<TenantEntitlementView> assignPlan(
        @PathVariable UUID tenantId,
        @RequestBody AssignPlanRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.assignPlan(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PutMapping("/tenants/{tenantId}/subscription/quotas")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<TenantEntitlementView> updateQuotas(
        @PathVariable UUID tenantId,
        @RequestBody UpdateQuotasRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.updateQuotas(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PostMapping("/tenants/{tenantId}/subscription/grace")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<TenantEntitlementView> grantGrace(
        @PathVariable UUID tenantId,
        @RequestBody GrantGraceRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.grantGrace(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PostMapping("/tenants/{tenantId}/subscription/suspend")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<TenantEntitlementView> suspend(
        @PathVariable UUID tenantId,
        @RequestBody SuspendTenantRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.suspend(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PostMapping("/tenants/{tenantId}/subscription/reactivate")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<TenantEntitlementView> reactivate(
        @PathVariable UUID tenantId,
        @RequestBody(required = false) ReactivateRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.reactivate(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @PatchMapping("/tenants/{tenantId}/subscription/expire-policy")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<TenantEntitlementView> updateExpirePolicy(
        @PathVariable UUID tenantId,
        @RequestBody ExpirePolicyRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.updateExpirePolicy(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }

    @GetMapping("/tenants/{tenantId}/charges")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<TenantChargesSummaryDto> listCharges(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(subscriptionService.listCharges(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/payments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<List<SubscriptionPaymentDto>> listPayments(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(subscriptionService.listPayments(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/payments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'PLATFORM_OPERATOR')")
    ResponseEntity<PaymentRecordResultDto> recordPayment(
        @PathVariable UUID tenantId,
        @RequestBody RecordPaymentRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        PaymentRecordResultDto result = subscriptionService.recordPayment(
            tenantId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/charges/{chargeId}/waive")
    @PreAuthorize("hasRole('SUPERADMIN')")
    ResponseEntity<SubscriptionChargeDto> waiveCharge(
        @PathVariable UUID chargeId,
        @RequestBody(required = false) WaiveChargeRequest request,
        @AuthenticationPrincipal PlatformPrincipal actor,
        HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(subscriptionService.waiveCharge(
            chargeId,
            request,
            actor,
            PlatformAuditService.resolveClientIp(servletRequest)
        ));
    }
}
