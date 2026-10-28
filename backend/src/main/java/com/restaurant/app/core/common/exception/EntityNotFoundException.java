package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando una entidad requerida no existe en el sistema (HTTP 404 Not Found).
 */
public class EntityNotFoundException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/not-found");
    private static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    public EntityNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }

    public EntityNotFoundException(String entityName, Object id) {
        super(HttpStatus.NOT_FOUND, DEFAULT_ERROR_CODE, entityName + " no encontrado con identificador: " + id, DEFAULT_TYPE);
    }

    public EntityNotFoundException(String entityName, String field, Object value) {
        super(HttpStatus.NOT_FOUND, DEFAULT_ERROR_CODE, entityName + " no encontrado con " + field + ": " + value, DEFAULT_TYPE);
    }
}
