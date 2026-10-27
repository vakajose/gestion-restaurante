package com.restaurant.app.modules.pos.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderItemRequest(
    @NotNull(message = "El ID del plato es obligatorio")
    UUID dishId,

    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    int quantity,

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio unitario no puede ser negativo")
    BigDecimal unitPrice,

    String notes,

    UUID comboId,

    List<UUID> componentDishIds,

    List<@Valid SelectedModifierRequest> selectedModifiers
) {
    public CreateOrderItemRequest {
        componentDishIds = componentDishIds != null ? List.copyOf(componentDishIds) : List.of();
        selectedModifiers = selectedModifiers != null ? List.copyOf(selectedModifiers) : List.of();
    }

    public CreateOrderItemRequest(UUID dishId, int quantity, BigDecimal unitPrice, String notes) {
        this(dishId, quantity, unitPrice, notes, null, List.of(), List.of());
    }

    public CreateOrderItemRequest(UUID dishId, int quantity, BigDecimal unitPrice, String notes, UUID comboId) {
        this(dishId, quantity, unitPrice, notes, comboId, List.of(), List.of());
    }

    public CreateOrderItemRequest(UUID dishId, int quantity, BigDecimal unitPrice, String notes, UUID comboId, List<UUID> componentDishIds) {
        this(dishId, quantity, unitPrice, notes, comboId, componentDishIds, List.of());
    }
}
