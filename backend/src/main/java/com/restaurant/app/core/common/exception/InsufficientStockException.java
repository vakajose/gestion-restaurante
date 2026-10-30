package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando el stock en almacén es insuficiente para una deducción estricta (HTTP 409 Conflict).
 * Cumple con el estándar RFC 7807 (ADR-0015 / PB-044).
 */
public class InsufficientStockException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://api.restaurant.com/errors/insufficient-stock");
    private static final String DEFAULT_ERROR_CODE = "INSUFFICIENT_STOCK_ERROR";
    private static final String DEFAULT_TITLE = "Stock Insuficiente en Almacén";

    public InsufficientStockException(String message) {
        super(HttpStatus.CONFLICT, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE, DEFAULT_TITLE);
    }
}
