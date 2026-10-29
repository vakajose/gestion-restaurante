package com.restaurant.app.modules.company.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBranchRequest(
    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String name,

    String address,

    BigDecimal latitude,

    BigDecimal longitude,

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    String phone,

    @Size(max = 3, message = "La moneda debe ser un código ISO de 3 caracteres")
    String currency,

    String timezone
) {
    public CreateBranchRequest(String name, String address, String phone, String timezone) {
        this(name, address, null, null, phone, "BOB", timezone);
    }
}
