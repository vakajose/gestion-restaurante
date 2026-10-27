package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import com.restaurant.app.modules.company.api.UpdateCompanyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company")
class CompanyController {

    private final CompanyPublicApi companyPublicApi;

    CompanyController(CompanyPublicApi companyPublicApi) {
        this.companyPublicApi = companyPublicApi;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('company:read', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<TenantDto> getCompany() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(companyPublicApi.getCompany(tenantId));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('company:write', 'ROLE_ADMIN_TENANT', 'ROLE_SUPERADMIN')")
    public ResponseEntity<TenantDto> updateCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return ResponseEntity.ok(companyPublicApi.updateCompany(tenantId, request));
    }
}
