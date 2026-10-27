package com.restaurant.app.modules.company.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String name,

    String address,

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    String phone,

    String timezone
) {
}
