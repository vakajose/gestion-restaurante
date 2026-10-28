package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando los datos de entrada o parámetros de la petición son semánticamente inválidos (HTTP 400 Bad Request).
 */
public class ValidationException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/bad-request");
    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }

    public ValidationException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }
}
