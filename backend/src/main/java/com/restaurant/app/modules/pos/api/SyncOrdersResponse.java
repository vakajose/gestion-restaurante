package com.restaurant.app.modules.pos.api;

import java.util.List;

public record SyncOrdersResponse(
    List<OrderDto> syncedOrders,
    int totalReceived,
    int totalProcessed
) {
    public SyncOrdersResponse {
        syncedOrders = syncedOrders != null ? List.copyOf(syncedOrders) : List.of();
    }
}
