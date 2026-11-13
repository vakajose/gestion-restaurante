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
@Table(name = "platform_audit_logs")
class PlatformAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_admin_id")
    private UUID actorAdminId;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlatformAuditLog() {
    }

    PlatformAuditLog(
        UUID actorAdminId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String detailsJson,
        String ipAddress
    ) {
        this.actorAdminId = actorAdminId;
        this.actorUsername = actorUsername != null ? actorUsername : "SYSTEM";
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detailsJson = detailsJson;
        this.ipAddress = ipAddress;
    }

    UUID getId() {
        return id;
    }

    UUID getActorAdminId() {
        return actorAdminId;
    }

    String getActorUsername() {
        return actorUsername;
    }

    String getAction() {
        return action;
    }

    String getTargetType() {
        return targetType;
    }

    String getTargetId() {
        return targetId;
    }

    String getDetailsJson() {
        return detailsJson;
    }

    String getIpAddress() {
        return ipAddress;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
