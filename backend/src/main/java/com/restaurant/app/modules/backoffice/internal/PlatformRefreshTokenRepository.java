package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface PlatformRefreshTokenRepository extends JpaRepository<PlatformRefreshToken, UUID> {

    Optional<PlatformRefreshToken> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    @Query("UPDATE PlatformRefreshToken rt SET rt.revokedAt = :now WHERE rt.adminId = :adminId AND rt.revokedAt IS NULL")
    int revokeAllActiveByAdminId(@Param("adminId") UUID adminId, @Param("now") Instant now);
}
