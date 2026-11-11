package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.finance.api.CreateShiftAuthorizationRequest;
import com.restaurant.app.modules.finance.api.DecideAuthorizationRequest;
import com.restaurant.app.modules.finance.api.ShiftAuthorizationDto;
import com.restaurant.app.modules.finance.api.ValidatePinRequest;
import com.restaurant.app.modules.finance.api.ValidatePinResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/authorizations")
class AuthorizationController {

    private final ShiftAuthorizationService shiftAuthorizationService;

    AuthorizationController(ShiftAuthorizationService shiftAuthorizationService) {
        this.shiftAuthorizationService = shiftAuthorizationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('pos:access', 'finance:shifts', 'finance:expenses_create') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER', 'CASHIER')")
    public ResponseEntity<ShiftAuthorizationDto> createRequest(@Valid @RequestBody CreateShiftAuthorizationRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = TenantContextHolder.getUserId();
        ShiftAuthorizationDto dto = shiftAuthorizationService.createRequest(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/validate-pin")
    @PreAuthorize("hasAnyAuthority('pos:access', 'finance:shifts') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER', 'CASHIER')")
    public ResponseEntity<ValidatePinResponse> validatePin(@Valid @RequestBody ValidatePinRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        ValidatePinResponse response = shiftAuthorizationService.validateSupervisorPin(tenantId, request.branchId(), request.pin());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'pos:access') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER', 'CASHIER')")
    public ResponseEntity<List<ShiftAuthorizationDto>> listAuthorizations(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) String status
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        List<ShiftAuthorizationDto> list = shiftAuthorizationService.list(tenantId, branchId, status);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'pos:access') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER', 'CASHIER')")
    public ResponseEntity<ShiftAuthorizationDto> getById(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return shiftAuthorizationService.getById(tenantId, id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('finance:shifts_supervisor', 'finance:expenses_approve') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ShiftAuthorizationDto> approve(
        @PathVariable UUID id,
        @RequestBody(required = false) DecideAuthorizationRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID supervisorId = TenantContextHolder.getUserId();
        String notes = request != null ? request.notes() : null;
        ShiftAuthorizationDto dto = shiftAuthorizationService.approve(tenantId, supervisorId, id, notes, "REMOTE_SESSION");
        return ResponseEntity.ok(dto);
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    @PreAuthorize("hasAnyAuthority('finance:shifts_supervisor', 'finance:expenses_approve') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ShiftAuthorizationDto> reject(
        @PathVariable UUID id,
        @RequestBody(required = false) DecideAuthorizationRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID supervisorId = TenantContextHolder.getUserId();
        String notes = request != null ? request.notes() : null;
        ShiftAuthorizationDto dto = shiftAuthorizationService.reject(tenantId, supervisorId, id, notes, "REMOTE_SESSION");
        return ResponseEntity.ok(dto);
    }
}
