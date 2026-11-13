package com.restaurant.app.modules.backoffice.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface PlatformAuditLogRepository
    extends JpaRepository<PlatformAuditLog, UUID>, JpaSpecificationExecutor<PlatformAuditLog> {
}
