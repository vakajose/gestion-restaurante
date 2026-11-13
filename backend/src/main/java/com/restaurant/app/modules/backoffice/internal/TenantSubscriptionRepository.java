package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

    Optional<TenantSubscription> findByTenantId(UUID tenantId);

    List<TenantSubscription> findByTenantIdIn(Collection<UUID> tenantIds);

    long countByStatus(SubscriptionStatus status);
}
