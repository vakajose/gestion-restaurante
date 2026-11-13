package com.restaurant.app.core.storage.api;

import java.time.Instant;

/**
 * Resultado del chequeo de conectividad del proveedor de almacenamiento activo.
 */
public record StorageConnectionTestResult(
    StorageProviderType provider,
    boolean reachable,
    String targetDescription,
    String message,
    Instant checkedAt
) {
}
