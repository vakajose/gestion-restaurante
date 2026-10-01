package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    String name,

    @Size(max = 50, message = "El icono no puede superar 50 caracteres")
    String icon,

    Integer sortOrder
) {
    public int resolvedSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
