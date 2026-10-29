package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.modules.configuration.api.ParameterScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SystemParameterValueRepository extends JpaRepository<SystemParameterValue, UUID> {

    Optional<SystemParameterValue> findByParameterKeyAndScopeAndBranchId(
        String parameterKey,
        ParameterScope scope,
        UUID branchId
    );

    Optional<SystemParameterValue> findByParameterKeyAndScopeAndTenantId(
        String parameterKey,
        ParameterScope scope,
        UUID tenantId
    );

    Optional<SystemParameterValue> findByParameterKeyAndScope(
        String parameterKey,
        ParameterScope scope
    );

    @Query("""
        SELECT v FROM SystemParameterValue v
        WHERE v.parameterKey = :key
          AND (
               (v.scope = 'BRANCH' AND v.branchId = :branchId)
            OR (v.scope = 'TENANT' AND v.tenantId = :tenantId)
            OR (v.scope = 'GLOBAL')
          )
        ORDER BY CASE v.scope
            WHEN 'BRANCH' THEN 1
            WHEN 'TENANT' THEN 2
            WHEN 'GLOBAL' THEN 3
            ELSE 4 END ASC
    """)
    List<SystemParameterValue> findOverridesHierarchical(
        @Param("key") String key,
        @Param("tenantId") UUID tenantId,
        @Param("branchId") UUID branchId
    );

    List<SystemParameterValue> findByScopeAndBranchId(ParameterScope scope, UUID branchId);

    List<SystemParameterValue> findByScopeAndTenantId(ParameterScope scope, UUID tenantId);

    List<SystemParameterValue> findByScope(ParameterScope scope);

    @Modifying
    @Query("DELETE FROM SystemParameterValue v WHERE v.parameterKey = :key AND v.scope = :scope AND v.branchId = :branchId")
    void deleteByBranchScope(
        @Param("key") String key,
        @Param("scope") ParameterScope scope,
        @Param("branchId") UUID branchId
    );

    @Modifying
    @Query("DELETE FROM SystemParameterValue v WHERE v.parameterKey = :key AND v.scope = :scope AND v.tenantId = :tenantId")
    void deleteByTenantScope(
        @Param("key") String key,
        @Param("scope") ParameterScope scope,
        @Param("tenantId") UUID tenantId
    );

    @Modifying
    @Query("DELETE FROM SystemParameterValue v WHERE v.parameterKey = :key AND v.scope = :scope")
    void deleteByGlobalScope(
        @Param("key") String key,
        @Param("scope") ParameterScope scope
    );
}
