package com.restaurant.app.modules.finance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExpenseApprovalRequest(
    @NotBlank(message = "El estado de resolución es obligatorio")
    @Pattern(regexp = "APPROVED|REJECTED", message = "El estado debe ser APPROVED o REJECTED")
    String status,

    String approvalNotes
) {}
