package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_refresh_tokens")
class PlatformRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlatformRefreshToken() {
    }

    PlatformRefreshToken(UUID adminId, String tokenHash, String deviceInfo, String ipAddress, Instant expiresAt) {
        this.adminId = adminId;
        this.tokenHash = tokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
    }

    UUID getId() {
        return id;
    }

    UUID getAdminId() {
        return adminId;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }

    void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    void setReplacedById(UUID replacedById) {
        this.replacedById = replacedById;
    }
}
