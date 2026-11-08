package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando el tamaño de un archivo adjunto excede el límite permitido (HTTP 413 Payload Too Large).
 */
public class PayloadTooLargeException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/payload-too-large");
    private static final String DEFAULT_ERROR_CODE = "PAYLOAD_TOO_LARGE";

    public PayloadTooLargeException(String message) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE, "Archivo demasiado grande");
    }
}
