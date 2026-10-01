package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateIngredientRequest(
    @NotBlank(message = "El nombre del ingrediente es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String name,

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Pattern(regexp = "KG|GRAM|LITER|ML|UNIT", message = "Unidad de medida inválida (permitidas: KG, GRAM, LITER, ML, UNIT)")
    String unitOfMeasure,

    @NotNull(message = "El stock mínimo de alerta es obligatorio")
    @DecimalMin(value = "0.0000", message = "El stock mínimo no puede ser negativo")
    BigDecimal minStockAlert
) {
}
