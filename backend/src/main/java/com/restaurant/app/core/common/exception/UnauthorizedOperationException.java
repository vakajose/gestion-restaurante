package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando el usuario no cuenta con autorización o contexto para realizar la operación (HTTP 403 Forbidden).
 */
public class UnauthorizedOperationException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/forbidden");
    private static final String DEFAULT_ERROR_CODE = "FORBIDDEN_OPERATION";

    public UnauthorizedOperationException(String message) {
        super(HttpStatus.FORBIDDEN, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }

    public UnauthorizedOperationException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }
}
