package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada ante colisiones de unicidad o conflictos de estado en el recurso (HTTP 409 Conflict).
 */
public class ResourceConflictException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/conflict");
    private static final String DEFAULT_ERROR_CODE = "RESOURCE_CONFLICT";

    public ResourceConflictException(String message) {
        super(HttpStatus.CONFLICT, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }

    public ResourceConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }
}
