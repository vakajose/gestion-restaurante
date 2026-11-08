package com.restaurant.app.modules.pos.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemModifierDto(
    UUID modifierId,
    @JsonProperty("modifierName")
    String modifierName,
    @JsonProperty("unitPrice")
    BigDecimal unitPrice,
    UUID ingredientId,
    BigDecimal quantityConsumed
) {
    public OrderItemModifierDto {
        unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
    }

    @JsonProperty("name")
    public String name() {
        return modifierName;
    }

    @JsonProperty("additionalPrice")
    public BigDecimal additionalPrice() {
        return unitPrice;
    }
}
