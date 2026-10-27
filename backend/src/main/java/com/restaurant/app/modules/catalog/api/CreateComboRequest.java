package com.restaurant.app.modules.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateComboRequest(
    UUID branchId,
    @NotNull(message = "La categoría es obligatoria") UUID categoryId,
    @NotBlank(message = "El código es obligatorio") String code,
    @NotBlank(message = "El nombre es obligatorio") String name,
    String description,
    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio base no puede ser negativo")
    BigDecimal basePrice,
    List<@Valid CreateComboItemRequest> items
) {
    public CreateComboRequest {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
