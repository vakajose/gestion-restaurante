package com.restaurant.app.core.entitlements.api;

import java.util.UUID;

/**
 * Contrato público consumido por core.security y los módulos ERP para evaluar accesos y cuotas.
 */
public interface TenantEntitlementService {

    TenantEntitlementView getEntitlementView(UUID tenantId);

    void assertLoginAllowed(UUID tenantId);

    void assertPosAndShiftOperationsAllowed(UUID tenantId);

    void assertQuotaAvailable(UUID tenantId, QuotaResource resource, long targetCountAfterAction);

    void evict(UUID tenantId);
}
