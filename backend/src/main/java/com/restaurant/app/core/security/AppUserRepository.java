package com.restaurant.app.core.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM AppUser u WHERE u.username = :login OR (u.email IS NOT NULL AND LOWER(u.email) = LOWER(:login))")
    Optional<AppUser> findByUsernameOrEmail(@Param("login") String login);

    @Query("SELECT u FROM AppUser u WHERE (u.tenantId = :tenantId) AND (u.username = :login OR (u.email IS NOT NULL AND LOWER(u.email) = LOWER(:login)))")
    Optional<AppUser> findByTenantIdAndUsernameOrEmail(@Param("tenantId") UUID tenantId, @Param("login") String login);
}
