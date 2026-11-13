-- ============================================================================
-- V17__platform_admins_and_sessions.sql
-- Identidad Supra-Tenant de Plataforma (Backoffice) físicamente separada
-- de app_users (tenants), más sesiones con refresh tokens opacos (ADR-0027).
-- ============================================================================

CREATE TABLE IF NOT EXISTS platform_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_admin_username
    ON platform_admins (LOWER(TRIM(username)));

CREATE UNIQUE INDEX IF NOT EXISTS uq_platform_admin_email
    ON platform_admins (LOWER(TRIM(email)));

CREATE TABLE IF NOT EXISTS platform_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES platform_admins(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID REFERENCES platform_refresh_tokens(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_refresh_tokens_admin
    ON platform_refresh_tokens (admin_id, revoked_at);

CREATE INDEX IF NOT EXISTS idx_platform_refresh_tokens_hash
    ON platform_refresh_tokens (token_hash);
