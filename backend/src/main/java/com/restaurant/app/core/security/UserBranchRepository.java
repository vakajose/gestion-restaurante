package com.restaurant.app.core.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserBranchRepository extends JpaRepository<UserBranch, UUID> {

    List<UserBranch> findByUserId(UUID userId);

    List<UserBranch> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Modifying
    @Query("DELETE FROM UserBranch ub WHERE ub.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
