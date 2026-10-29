package com.restaurant.app.modules.company.api;

import java.util.UUID;

public record TenantOnboardedEvent(
    UUID tenantId,
    UUID branchId,
    String businessTemplate
) {
}
