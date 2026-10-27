package com.restaurant.app.modules.company.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    String legalName,

    @NotBlank(message = "El NIT o identificación fiscal es obligatorio")
    @Size(max = 50, message = "El NIT no puede exceder 50 caracteres")
    String nitOrTaxId,

    @NotBlank(message = "El identificador slug es obligatorio")
    @Size(max = 60, message = "El slug no puede exceder 60 caracteres")
    String slug
) {
}
