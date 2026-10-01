package com.restaurant.app.modules.pos.api;

import jakarta.validation.Valid;

import java.util.List;

public record SyncOrdersRequest(
    List<@Valid CreateOrderRequest> orders
) {
    public SyncOrdersRequest {
        orders = orders != null ? List.copyOf(orders) : List.of();
    }
}
