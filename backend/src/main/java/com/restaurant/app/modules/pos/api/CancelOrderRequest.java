package com.restaurant.app.modules.pos.api;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CancelOrderRequest(
    @NotBlank(message = "El motivo de cancelación es obligatorio")
    String cancellationReason,
    String cancellationNotes,
    @NotBlank(message = "El método de reembolso es obligatorio (CASH o DIGITAL)")
    String refundMethod,
    boolean wasPrepared,
    String supervisorPin,
    UUID authorizationRequestId
) {}
