package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.company.api.CreateBranchRequest;
import com.restaurant.app.modules.company.api.UpdateBranchRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
class BranchController {

    private final BranchPublicApi branchPublicApi;

    BranchController(BranchPublicApi branchPublicApi) {
        this.branchPublicApi = branchPublicApi;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('branches:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<List<BranchDto>> getBranches() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(branchPublicApi.getBranchesByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('branches:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable("id") UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(branchPublicApi.getBranchById(tenantId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('branches:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<BranchDto> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        BranchDto created = branchPublicApi.createBranch(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('branches:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<BranchDto> updateBranch(
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateBranchRequest request
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(branchPublicApi.updateBranch(tenantId, id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('branches:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<BranchDto> toggleBranchStatus(
        @PathVariable("id") UUID id,
        @RequestBody Map<String, Boolean> body
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        boolean isActive = body.getOrDefault("isActive", true);
        return ResponseEntity.ok(branchPublicApi.toggleBranchStatus(tenantId, id, isActive));
    }
}
