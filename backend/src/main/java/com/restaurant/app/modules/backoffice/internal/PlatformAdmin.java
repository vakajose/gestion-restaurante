package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_admins")
class PlatformAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    protected PlatformAdmin() {
    }

    PlatformAdmin(String username, String email, String fullName, String passwordHash, boolean mustChangePassword) {
        this.username = username.trim();
        this.email = email.trim().toLowerCase();
        this.fullName = fullName.trim();
        this.passwordHash = passwordHash;
        this.isActive = true;
        this.mustChangePassword = mustChangePassword;
    }

    UUID getId() {
        return id;
    }

    String getUsername() {
        return username;
    }

    String getEmail() {
        return email;
    }

    String getFullName() {
        return fullName;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    boolean isActive() {
        return isActive;
    }

    void setActive(boolean active) {
        isActive = active;
    }

    boolean isMustChangePassword() {
        return mustChangePassword;
    }

    void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    Instant getLastLoginAt() {
        return lastLoginAt;
    }

    void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
