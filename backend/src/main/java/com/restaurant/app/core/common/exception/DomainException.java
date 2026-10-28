package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Clase base para todas las excepciones de dominio del sistema.
 * Permite tipar y mapear errores a respuestas RFC 7807 ProblemDetail estándar.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final URI typeUri;

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.typeUri = typeUri;
    }

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.typeUri = typeUri;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public URI getTypeUri() {
        return typeUri;
    }
}
