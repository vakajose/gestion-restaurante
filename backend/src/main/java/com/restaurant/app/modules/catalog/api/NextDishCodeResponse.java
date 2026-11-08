package com.restaurant.app.modules.catalog.api;

/**
 * Respuesta DTO con sugerencia de código correlativo para plato (ADR-0022 / PB-048).
 *
 * @param nextCode Código generado en formato simple ${prefix}-${suggestedSequence} (ej. BEB-1, HAM-2).
 * @param prefix Prefijo alfanumérico en mayúsculas de la categoría asociada.
 * @param suggestedSequence Número correlativo calculado con política MAX + 1.
 */
public record NextDishCodeResponse(
    String nextCode,
    String prefix,
    int suggestedSequence
) {
}
