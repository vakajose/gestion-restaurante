package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class FinanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(FinanceEventListener.class);

    private final CashShiftService cashShiftService;

    FinanceEventListener(CashShiftService cashShiftService) {
        this.cashShiftService = cashShiftService;
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        log.info("FinanceEventListener: Processing OrderPaidEvent for order: {}, shift: {}, amount: {}",
            event.orderId(), event.cashShiftId(), event.totalAmount());

        cashShiftService.updateRunningSales(
            event.tenantId(),
            event.branchId(),
            event.cashShiftId(),
            event.paymentMethod(),
            event.totalAmount()
        );
    }
}
