package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ModifierItemSummary(
    UUID modifierId,
    String name,
    UUID ingredientId,
    BigDecimal quantityConsumed
) {
    public ModifierItemSummary {
        quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
    }
}
