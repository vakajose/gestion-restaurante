package com.restaurant.app.core.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Excepción lanzada cuando un plato del menú del turno no cuenta con cupo suficiente o está agotado (HTTP 409 Conflict).
 * Cumple con el estándar RFC 7807 (ADR-0015 / PB-034).
 */
public class ItemSoldOutException extends DomainException {

    private static final URI DEFAULT_TYPE = URI.create("https://api.restaurant.com/errors/item-sold-out");
    private static final String DEFAULT_ERROR_CODE = "ITEM_SOLD_OUT";
    private static final String DEFAULT_TITLE = "Plato Agotado o Cupo Insuficiente";

    public ItemSoldOutException(String message) {
        super(HttpStatus.CONFLICT, DEFAULT_ERROR_CODE, message, DEFAULT_TYPE, DEFAULT_TITLE);
    }
}
