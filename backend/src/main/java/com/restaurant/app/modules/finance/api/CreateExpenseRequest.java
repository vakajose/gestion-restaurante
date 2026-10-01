package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(
    @NotNull(message = "El branchId es obligatorio")
    UUID branchId,

    UUID cashShiftId,

    @NotBlank(message = "El tipo de gasto es obligatorio (DAILY_OPERATIONAL o MONTHLY_FIXED_PRORATED)")
    String expenseType,

    @NotBlank(message = "La descripción del gasto es obligatoria")
    String description,

    @NotNull(message = "El monto del gasto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    BigDecimal amount,

    boolean paidFromCashDrawer,

    String evidenceUrl,

    LocalDate expenseDate
) {}
