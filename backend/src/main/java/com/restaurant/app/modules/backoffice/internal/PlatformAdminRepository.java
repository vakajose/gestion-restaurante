package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {

    @Query("""
        SELECT p FROM PlatformAdmin p
        WHERE LOWER(TRIM(p.username)) = LOWER(TRIM(:login))
           OR LOWER(TRIM(p.email)) = LOWER(TRIM(:login))
        """)
    Optional<PlatformAdmin> findByLogin(@Param("login") String login);

    @Query("SELECT p FROM PlatformAdmin p WHERE LOWER(TRIM(p.username)) = LOWER(TRIM(:username))")
    Optional<PlatformAdmin> findByUsernameNormalized(@Param("username") String username);

    @Query("SELECT p FROM PlatformAdmin p WHERE LOWER(TRIM(p.email)) = LOWER(TRIM(:email))")
    Optional<PlatformAdmin> findByEmailNormalized(@Param("email") String email);

    long countByRoleAndIsActiveTrue(PlatformRole role);

    List<PlatformAdmin> findAllByOrderByCreatedAtAsc();
}
