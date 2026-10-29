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
    private final String title;

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri) {
        this(status, errorCode, message, typeUri, (String) null);
    }

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri, String title) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.typeUri = typeUri;
        this.title = title;
    }

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri, Throwable cause) {
        this(status, errorCode, message, typeUri, null, cause);
    }

    protected DomainException(HttpStatus status, String errorCode, String message, URI typeUri, String title, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.typeUri = typeUri;
        this.title = title;
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

    public String getTitle() {
        return title;
    }
}
