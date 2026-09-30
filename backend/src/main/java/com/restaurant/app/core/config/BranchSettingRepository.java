package com.restaurant.app.core.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchSettingRepository extends JpaRepository<BranchSetting, UUID> {
    Optional<BranchSetting> findByBranchIdAndSettingKey(UUID branchId, String settingKey);
    List<BranchSetting> findByBranchId(UUID branchId);
}
