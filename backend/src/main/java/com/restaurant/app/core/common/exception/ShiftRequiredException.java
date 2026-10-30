package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando se intenta registrar una comanda o venta en POS sin un turno de caja abierto (HTTP 409 Conflict).
 * Cumple con el estándar RFC 7807 (ADR-0013 / PB-041).
 */
public class ShiftRequiredException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://api.restaurant.com/errors/shift-required");
    private static final String DEFAULT_ERROR_CODE = "SHIFT_REQUIRED_ERROR";
    private static final String DEFAULT_TITLE = "Turno de Caja Requerido";
    private static final String DEFAULT_DETAIL = "No es posible registrar ventas: la caja de la sucursal se encuentra cerrada.";

    public ShiftRequiredException() {
        this(DEFAULT_DETAIL);
    }

    public ShiftRequiredException(String message) {
        super(HttpStatus.CONFLICT, DEFAULT_ERROR_CODE, message != null ? message : DEFAULT_DETAIL, DEFAULT_TYPE, DEFAULT_TITLE);
    }
}
