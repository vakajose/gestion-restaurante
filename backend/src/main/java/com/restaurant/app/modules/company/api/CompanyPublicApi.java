package com.restaurant.app.modules.company.api;

import java.util.UUID;

public interface CompanyPublicApi {

    TenantDto getCompany(UUID tenantId);

    TenantDto updateCompany(UUID tenantId, UpdateCompanyRequest request);
}
