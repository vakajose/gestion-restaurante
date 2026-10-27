package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import com.restaurant.app.modules.company.api.UpdateCompanyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class CompanyServiceImpl implements CompanyPublicApi {

    private final TenantRepository tenantRepository;

    CompanyServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDto getCompany(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + tenantId));
        return TenantDto.fromEntity(tenant);
    }

    @Override
    @Transactional
    public TenantDto updateCompany(UUID tenantId, UpdateCompanyRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada: " + tenantId));

        String newSlug = Tenant.slugify(request.slug());
        tenantRepository.findBySlug(newSlug).ifPresent(other -> {
            if (!other.getId().equals(tenantId)) {
                throw new IllegalArgumentException("El identificador slug '" + newSlug + "' ya se encuentra registrado por otra empresa.");
            }
        });

        tenantRepository.findByNitOrTaxId(request.nitOrTaxId().trim()).ifPresent(other -> {
            if (!other.getId().equals(tenantId)) {
                throw new IllegalArgumentException("El NIT/identificación fiscal '" + request.nitOrTaxId() + "' ya se encuentra registrado por otra empresa.");
            }
        });

        tenant.setLegalName(request.legalName().trim());
        tenant.setNitOrTaxId(request.nitOrTaxId().trim());
        tenant.setSlug(newSlug);

        Tenant updated = tenantRepository.save(tenant);
        return TenantDto.fromEntity(updated);
    }
}
