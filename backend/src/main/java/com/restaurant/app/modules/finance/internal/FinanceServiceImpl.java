package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class FinanceServiceImpl implements FinancePublicApi {

    private final CashShiftService cashShiftService;

    FinanceServiceImpl(CashShiftService cashShiftService) {
        this.cashShiftService = cashShiftService;
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
    public boolean hasActiveShift(UUID tenantId, UUID branchId) {
        return findActiveShift(tenantId, branchId).isPresent();
    }

    @Override
    @Transactional
    public void recordSale(UUID tenantId, UUID branchId, UUID cashShiftId, String paymentMethod, BigDecimal amount) {
        cashShiftService.updateRunningSales(tenantId, branchId, cashShiftId, paymentMethod, amount);
    }
}
