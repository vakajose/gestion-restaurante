package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CreateShiftAuthorizationRequest;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.ShiftAuthorizationDto;
import com.restaurant.app.modules.finance.api.ValidatePinResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class FinanceServiceImpl implements FinancePublicApi {

    private final CashShiftService cashShiftService;
    private final ShiftAuthorizationService shiftAuthorizationService;

    FinanceServiceImpl(CashShiftService cashShiftService, ShiftAuthorizationService shiftAuthorizationService) {
        this.cashShiftService = cashShiftService;
        this.shiftAuthorizationService = shiftAuthorizationService;
    }

    @Override
    @Transactional
    public CashShiftDto openShift(com.restaurant.app.modules.finance.api.OpenShiftRequest request) {
        return cashShiftService.openShift(request);
    }

    @Override
    public Optional<CashShiftDto> findActiveShift(UUID tenantId, UUID branchId) {
        if (tenantId == null || branchId == null) {
            return Optional.empty();
        }
        return cashShiftService.findActiveShift(tenantId, branchId);
    }

    @Override
    public Optional<CashShiftDto> findActiveShiftForUser(UUID tenantId, UUID branchId, UUID userId) {
        if (tenantId == null || branchId == null || userId == null) {
            return Optional.empty();
        }
        return cashShiftService.findActiveShiftForUser(tenantId, branchId, userId);
    }

    @Override
    public java.util.List<CashShiftDto> listActiveShifts(UUID tenantId, UUID branchId) {
        if (tenantId == null || branchId == null) {
            return java.util.List.of();
        }
        return cashShiftService.listActiveShifts(tenantId, branchId);
    }

    @Override
    public boolean hasActiveShift(UUID tenantId, UUID branchId) {
        return findActiveShift(tenantId, branchId).isPresent();
    }

    @Override
    @Transactional
    public void recordSale(UUID tenantId, UUID branchId, UUID cashShiftId, String paymentMethod, BigDecimal amount) {
        cashShiftService.updateRunningSales(tenantId, branchId, cashShiftId, paymentMethod, amount);
    }

    @Override
    @Transactional
    public void recordRefund(UUID tenantId, UUID branchId, UUID cashShiftId, BigDecimal amount) {
        cashShiftService.recordRefund(tenantId, branchId, cashShiftId, amount);
    }

    @Override
    public ValidatePinResponse validateSupervisorPin(UUID tenantId, UUID branchId, String pin) {
        return shiftAuthorizationService.validateSupervisorPin(tenantId, branchId, pin);
    }

    @Override
    @Transactional
    public ShiftAuthorizationDto createAuthorizationRequest(UUID tenantId, UUID userId, CreateShiftAuthorizationRequest request) {
        return shiftAuthorizationService.createRequest(tenantId, userId, request);
    }

    @Override
    @Transactional
    public ShiftAuthorizationDto approveAuthorization(UUID tenantId, UUID supervisorId, UUID authorizationId, String notes, String decisionChannel) {
        return shiftAuthorizationService.approve(tenantId, supervisorId, authorizationId, notes, decisionChannel);
    }

    @Override
    public Optional<ShiftAuthorizationDto> getAuthorization(UUID tenantId, UUID authorizationId) {
        return shiftAuthorizationService.getById(tenantId, authorizationId);
    }
}
