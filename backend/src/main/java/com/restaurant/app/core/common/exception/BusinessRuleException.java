package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando una operación viola una regla de negocio o invariante del dominio (HTTP 422 Unprocessable Entity).
 */
public class BusinessRuleException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://restaurant.com/errors/business-rule-violation");
    private static final String DEFAULT_ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }

    public BusinessRuleException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, message, DEFAULT_TYPE);
    }
}
