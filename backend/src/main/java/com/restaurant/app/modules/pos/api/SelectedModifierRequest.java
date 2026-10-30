package com.restaurant.app.modules.pos.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectedModifierRequest(
    UUID modifierId,
    @JsonProperty("name")
    @JsonAlias({"modifierName", "name"})
    @NotNull(message = "El nombre del modificador es obligatorio")
    String name,
    @JsonProperty("unitPrice")
    @JsonAlias({"additionalPrice", "unitPrice"})
    BigDecimal unitPrice,
    UUID ingredientId,
    BigDecimal quantityConsumed
) {
    public SelectedModifierRequest {
        unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        quantityConsumed = quantityConsumed != null ? quantityConsumed : BigDecimal.ZERO;
    }
}
