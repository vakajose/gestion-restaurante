package com.restaurant.app.modules.company.api;

import java.util.Optional;
import java.util.UUID;

public interface CompanyPublicApi {

    TenantDto getCompany(UUID tenantId);

    Optional<TenantDto> findCompanyById(UUID tenantId);

    Optional<TenantDto> findCompanyByNitOrTaxId(String nitOrTaxId);

    Optional<TenantDto> findCompanyBySlug(String slug);

    TenantDto updateCompany(UUID tenantId, UpdateCompanyRequest request);
}
