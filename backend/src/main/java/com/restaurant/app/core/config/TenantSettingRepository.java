package com.restaurant.app.core.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSettingRepository extends JpaRepository<TenantSetting, UUID> {
    Optional<TenantSetting> findByTenantIdAndSettingKey(UUID tenantId, String settingKey);
    List<TenantSetting> findByTenantId(UUID tenantId);
}
