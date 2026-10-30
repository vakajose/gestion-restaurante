package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotBlank;

public record AdjustQuotaRequest(
    int quantityDelta,

    @NotBlank(message = "El motivo del ajuste es obligatorio")
    String reason
) {}
