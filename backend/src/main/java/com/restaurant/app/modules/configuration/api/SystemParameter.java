package com.restaurant.app.modules.configuration.api;

import java.util.Arrays;
import java.util.Optional;

public enum SystemParameter {
    INVENTORY_DEDUCTION_MODE("inventory.deduction_mode"),
    INVENTORY_ALLOW_NEGATIVE_STOCK("inventory.allow_negative_stock"),
    POS_DISPLAY_DISH_IMAGES("pos.display_dish_images"),
    POS_DEFAULT_ORDER_MODE("pos.default_order_mode"),
    POS_ALLOW_SCHEDULED_PICKUP("pos.allow_scheduled_pickup"),
    FINANCE_MAX_PETTY_CASH_EXPENSE("finance.max_petty_cash_expense"),
    FINANCE_REQUIRE_SUPERVISOR_PIN_CANCEL("finance.require_supervisor_pin_cancel"),
    FINANCE_SHIFT_OPENING_POLICY("finance.shift_opening_policy"),
    FINANCE_SHIFT_CONCURRENCY_MODE("finance.shift_concurrency_mode"),
    FINANCE_EXPENSE_ALLOW_SELF_APPROVAL("finance.expense_allow_self_approval"),
    SECURITY_REQUIRE_USER_EMAIL("security.require_user_email"),
    SECURITY_SESSION_INACTIVITY_TIMEOUT_MINUTES("security.session_inactivity_timeout_minutes"),
    COMPANY_CURRENCY("company.currency"),
    COMPANY_TIMEZONE("company.timezone"),
    POS_CANCELLATION_PREPARED_WASTE_POLICY("pos.cancellation_prepared_waste_policy");

    private final String key;

    SystemParameter(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<SystemParameter> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(p -> p.key.equalsIgnoreCase(key.trim()))
            .findFirst();
    }
}
