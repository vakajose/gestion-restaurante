package com.restaurant.app.modules.company.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.company.api.CreateBranchRequest;
import com.restaurant.app.modules.company.api.UpdateBranchRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class BranchServiceImpl implements BranchPublicApi {

    private static final String DEFAULT_TIMEZONE = "America/La_Paz";
    private static final String DEFAULT_CURRENCY = "BOB";

    private final BranchRepository branchRepository;

    BranchServiceImpl(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDto> getBranchesByTenant(UUID tenantId) {
        return branchRepository.findByTenantId(tenantId).stream()
            .map(BranchDto::fromEntity)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDto getBranchById(UUID tenantId, UUID branchId) {
        Branch branch = branchRepository.findByTenantIdAndId(tenantId, branchId)
            .orElseThrow(() -> new EntityNotFoundException("Sucursal", branchId));
        return BranchDto.fromEntity(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchDto> findBranchById(UUID branchId) {
        return branchRepository.findById(branchId).map(BranchDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchDto> findBranchById(UUID tenantId, UUID branchId) {
        return branchRepository.findByTenantIdAndId(tenantId, branchId).map(BranchDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public String getBranchTimezone(UUID branchId) {
        return branchRepository.findById(branchId)
            .map(Branch::getTimezone)
            .filter(tz -> tz != null && !tz.isBlank())
            .orElse("America/La_Paz");
    }

    @Override
    @Transactional
    public BranchDto createBranch(UUID tenantId, CreateBranchRequest request) {
        String name = request.name().trim();
        branchRepository.findByTenantIdAndName(tenantId, name).ifPresent(b -> {
            throw new ResourceConflictException("Ya existe una sucursal con el nombre '" + name + "' en esta empresa.");
        });

        String timezone = validateTimezone(request.timezone());
        String currency = (request.currency() != null && !request.currency().isBlank())
            ? request.currency().trim().toUpperCase()
            : DEFAULT_CURRENCY;

        Branch branch = new Branch(tenantId, name, timezone);
        branch.setAddress(request.address() != null ? request.address().trim() : null);
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setPhone(request.phone() != null ? request.phone().trim() : null);
        branch.setCurrency(currency);

        Branch saved = branchRepository.save(branch);
        return BranchDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public BranchDto updateBranch(UUID tenantId, UUID branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findByTenantIdAndId(tenantId, branchId)
            .orElseThrow(() -> new EntityNotFoundException("Sucursal", branchId));

        String name = request.name().trim();
        branchRepository.findByTenantIdAndName(tenantId, name).ifPresent(other -> {
            if (!other.getId().equals(branchId)) {
                throw new ResourceConflictException("Ya existe otra sucursal con el nombre '" + name + "' en esta empresa.");
            }
        });

        String timezone = validateTimezone(request.timezone());

        branch.setName(name);
        branch.setAddress(request.address() != null ? request.address().trim() : null);
        if (request.latitude() != null) {
            branch.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            branch.setLongitude(request.longitude());
        }
        branch.setPhone(request.phone() != null ? request.phone().trim() : null);
        if (request.currency() != null && !request.currency().isBlank()) {
            branch.setCurrency(request.currency().trim().toUpperCase());
        }
        branch.setTimezone(timezone);
        if (request.isActive() != null) {
            branch.setActive(request.isActive());
        }

        Branch updated = branchRepository.save(branch);
        return BranchDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public BranchDto toggleBranchStatus(UUID tenantId, UUID branchId, boolean isActive) {
        Branch branch = branchRepository.findByTenantIdAndId(tenantId, branchId)
            .orElseThrow(() -> new EntityNotFoundException("Sucursal", branchId));

        branch.setActive(isActive);
        Branch updated = branchRepository.save(branch);
        return BranchDto.fromEntity(updated);
    }

    private String validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        try {
            ZoneId.of(timezone.trim());
            return timezone.trim();
        } catch (Exception e) {
            throw new ValidationException("Zona horaria IANA inválida: " + timezone + ". Debe ser un identificador válido como 'America/La_Paz'.");
        }
    }
}
