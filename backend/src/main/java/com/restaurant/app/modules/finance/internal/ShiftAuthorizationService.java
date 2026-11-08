package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.modules.finance.api.CreateShiftAuthorizationRequest;
import com.restaurant.app.modules.finance.api.ShiftAuthorizationDto;
import com.restaurant.app.modules.finance.api.ValidatePinResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class ShiftAuthorizationService {

    private final ShiftAuthorizationRepository repository;
    private final UserPublicApi userPublicApi;

    ShiftAuthorizationService(ShiftAuthorizationRepository repository, UserPublicApi userPublicApi) {
        this.repository = repository;
        this.userPublicApi = userPublicApi;
    }

    @Transactional
    public ShiftAuthorizationDto createRequest(UUID tenantId, UUID userId, CreateShiftAuthorizationRequest request) {
        ShiftAuthorization auth = new ShiftAuthorization(
            tenantId,
            request.branchId(),
            request.cashShiftId(),
            request.requestType(),
            request.referenceId(),
            userId,
            request.amount(),
            request.justification()
        );
        ShiftAuthorization saved = repository.save(auth);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ValidatePinResponse validateSupervisorPin(UUID tenantId, UUID branchId, String pin) {
        Optional<UserSummaryDto> supervisorOpt = userPublicApi.validateSupervisorPin(tenantId, branchId, pin);
        if (supervisorOpt.isPresent()) {
            UserSummaryDto supervisor = supervisorOpt.get();
            String name = supervisor.fullName() != null && !supervisor.fullName().isBlank()
                ? supervisor.fullName()
                : supervisor.username();
            return new ValidatePinResponse(true, supervisor.id(), name);
        }
        return new ValidatePinResponse(false, null, null);
    }

    @Transactional
    public ShiftAuthorizationDto approve(UUID tenantId, UUID supervisorId, UUID authorizationId, String notes, String decisionChannel) {
        ShiftAuthorization auth = repository.findByTenantIdAndId(tenantId, authorizationId)
            .orElseThrow(() -> new EntityNotFoundException("ShiftAuthorization", authorizationId));

        if (!"PENDING".equalsIgnoreCase(auth.getStatus())) {
            throw new ValidationException("La solicitud ya se encuentra en estado " + auth.getStatus());
        }

        String channel = decisionChannel != null ? decisionChannel : "REMOTE_SESSION";
        auth.approve(supervisorId, notes, channel);
        ShiftAuthorization saved = repository.save(auth);
        return toDto(saved);
    }

    @Transactional
    public ShiftAuthorizationDto reject(UUID tenantId, UUID supervisorId, UUID authorizationId, String notes, String decisionChannel) {
        ShiftAuthorization auth = repository.findByTenantIdAndId(tenantId, authorizationId)
            .orElseThrow(() -> new EntityNotFoundException("ShiftAuthorization", authorizationId));

        if (!"PENDING".equalsIgnoreCase(auth.getStatus())) {
            throw new ValidationException("La solicitud ya se encuentra en estado " + auth.getStatus());
        }

        String channel = decisionChannel != null ? decisionChannel : "REMOTE_SESSION";
        auth.reject(supervisorId, notes, channel);
        ShiftAuthorization saved = repository.save(auth);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ShiftAuthorizationDto> list(UUID tenantId, UUID branchId, String status) {
        List<ShiftAuthorization> list;
        boolean hasStatus = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);

        if (branchId != null) {
            if (hasStatus) {
                list = repository.findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(tenantId, branchId, status.toUpperCase());
            } else {
                list = repository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId);
            }
        } else {
            if (hasStatus) {
                list = repository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.toUpperCase());
            } else {
                list = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
            }
        }

        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ShiftAuthorizationDto> getById(UUID tenantId, UUID id) {
        return repository.findByTenantIdAndId(tenantId, id).map(this::toDto);
    }

    private ShiftAuthorizationDto toDto(ShiftAuthorization auth) {
        String requestedByName = resolveUserName(auth.getRequestedBy());
        String decidedByName = auth.getDecidedBy() != null ? resolveUserName(auth.getDecidedBy()) : null;

        return new ShiftAuthorizationDto(
            auth.getId(),
            auth.getTenantId(),
            auth.getBranchId(),
            auth.getCashShiftId(),
            auth.getRequestType(),
            auth.getReferenceId(),
            auth.getRequestedBy(),
            requestedByName,
            auth.getAmount(),
            auth.getStatus(),
            auth.getJustification(),
            auth.getDecidedBy(),
            decidedByName,
            auth.getDecisionNotes(),
            auth.getDecisionChannel(),
            auth.getCreatedAt(),
            auth.getDecidedAt()
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userPublicApi.findUserSummary(userId)
            .map(u -> (u.fullName() != null && !u.fullName().isBlank()) ? u.fullName() : u.username())
            .orElse(userId.toString());
    }
}
