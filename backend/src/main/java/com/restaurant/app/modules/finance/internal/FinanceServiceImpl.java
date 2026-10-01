package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<CashShiftDto> findActiveShift(UUID tenantId, UUID branchId) {
        if (tenantId == null || branchId == null) {
            return Optional.empty();
        }
        return cashShiftService.getCurrentShift(branchId);
    }

    @Override
    public boolean hasActiveShift(UUID tenantId, UUID branchId) {
        return findActiveShift(tenantId, branchId).isPresent();
    }
}
