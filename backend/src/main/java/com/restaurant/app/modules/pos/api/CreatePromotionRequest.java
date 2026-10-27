package com.restaurant.app.modules.pos.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record CreatePromotionRequest(
    @NotBlank(message = "El nombre de la promoción es obligatorio")
    String name,

    String description,

    @NotBlank(message = "El tipo de descuento es obligatorio")
    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Tipo de descuento inválido (PERCENTAGE, FIXED_AMOUNT)")
    String discountType,

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El descuento debe ser mayor a 0")
    BigDecimal discountValue,

    @Pattern(regexp = "GLOBAL|CATEGORY|DISH", message = "Alcance inválido (GLOBAL, CATEGORY, DISH)")
    String scope,

    UUID targetCategoryId,

    UUID targetDishId,

    @Pattern(regexp = "AUTOMATIC|MANUAL", message = "Modo de aplicación inválido (AUTOMATIC, MANUAL)")
    String applicationMode,

    String daysOfWeek,

    LocalTime startTime,

    LocalTime endTime,

    LocalDate startDate,

    LocalDate endDate,

    BigDecimal minOrderAmount,

    Set<UUID> branchIds
) {
    public CreatePromotionRequest {
        scope = scope != null ? scope : "GLOBAL";
        applicationMode = applicationMode != null ? applicationMode : "AUTOMATIC";
        minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        branchIds = branchIds != null ? Set.copyOf(branchIds) : Set.of();
    }
}
