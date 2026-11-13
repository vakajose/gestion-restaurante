package com.restaurant.app.core.entitlements.api;

import java.util.UUID;

/**
 * Evento emitido al cambiar suscripción, gracia, pagos, overrides o suspensión de un inquilino.
 */
public record TenantEntitlementChangedEvent(UUID tenantId) {
}
