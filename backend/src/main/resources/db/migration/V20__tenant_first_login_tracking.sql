-- ============================================================================
-- V20__tenant_first_login_tracking.sql
-- Registro de primer/último ingreso en app_users para diferenciar inquilinos
-- aprovisionados desde Backoffice que aún no han iniciado sesión en el ERP (TASK-618).
-- ============================================================================

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_app_users_tenant_last_login
    ON app_users (tenant_id, last_login_at)
    WHERE last_login_at IS NOT NULL;
