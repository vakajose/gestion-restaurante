package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import com.restaurant.app.modules.company.api.UpdateCompanyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
            .orElseThrow(() -> new EntityNotFoundException("Empresa", tenantId));
        return TenantDto.fromEntity(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantDto> findCompanyById(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(TenantDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantDto> findCompanyByNitOrTaxId(String nitOrTaxId) {
        if (nitOrTaxId == null || nitOrTaxId.isBlank()) {
            return Optional.empty();
        }
        return tenantRepository.findByNitOrTaxId(nitOrTaxId.trim()).map(TenantDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantDto> findCompanyBySlug(String slug) {
        return tenantRepository.findBySlug(slug).map(TenantDto::fromEntity);
    }

    @Override
    @Transactional
    public TenantDto updateCompany(UUID tenantId, UpdateCompanyRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Empresa", tenantId));

        String newSlug = Tenant.slugify(request.slug());
        tenantRepository.findBySlug(newSlug).ifPresent(other -> {
            if (!other.getId().equals(tenantId)) {
                throw new ResourceConflictException("El identificador slug '" + newSlug + "' ya se encuentra registrado por otra empresa.");
            }
        });

        String normalizedNit = (request.nitOrTaxId() != null && !request.nitOrTaxId().isBlank())
            ? request.nitOrTaxId().trim()
            : null;

        if (normalizedNit != null) {
            tenantRepository.findByNitOrTaxId(normalizedNit).ifPresent(other -> {
                if (!other.getId().equals(tenantId)) {
                    throw new ResourceConflictException("El NIT/identificación fiscal '" + normalizedNit + "' ya se encuentra registrado por otra empresa.");
                }
            });
        }

        tenant.setLegalName(request.legalName().trim());
        tenant.setNitOrTaxId(normalizedNit);
        tenant.setSlug(newSlug);
        if (request.logoUrl() != null) {
            tenant.setLogoUrl(request.logoUrl().isBlank() ? null : request.logoUrl().trim());
        }

        Tenant updated = tenantRepository.save(tenant);
        return TenantDto.fromEntity(updated);
    }
}
