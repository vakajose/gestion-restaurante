package com.restaurant.app.core.entitlements.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto SPI implementado por modules.backoffice para cargar suscripción y conteos de uso sin crear ciclos en Modulith.
 */
public interface TenantEntitlementPort {

    Optional<TenantEntitlementSnapshot> loadSnapshot(UUID tenantId);

    Map<QuotaResource, Long> loadUsageCounts(UUID tenantId);
}
