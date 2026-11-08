package com.restaurant.app.modules.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    String name,

    @Size(max = 50, message = "El icono no puede superar 50 caracteres")
    String icon,

    Integer sortOrder,

    @NotBlank(message = "El prefijo es obligatorio")
    @Size(min = 1, max = 10, message = "El prefijo no puede superar 10 caracteres")
    String codePrefix
) {
    public int resolvedSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }

    public CreateCategoryRequest(String name, String icon, Integer sortOrder) {
        this(name, icon, sortOrder, name != null && name.trim().replaceAll("[^a-zA-Z0-9]", "").length() >= 3
            ? name.trim().replaceAll("[^a-zA-Z0-9]", "").substring(0, 3).toUpperCase()
            : "CAT");
    }
}
