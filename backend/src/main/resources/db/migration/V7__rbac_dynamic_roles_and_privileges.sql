-- ============================================================================
-- V7__rbac_dynamic_roles_and_privileges.sql
-- Control de Acceso RBAC Dinámico, Asignación Multi-Sucursal,
-- Vigencia Temporal de Roles y Reseteo Delegado de Contraseña (ADR-0010)
-- ============================================================================

-- 1. Flexibilizar columna role en app_users para permitir roles personalizados por tenant
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;
ALTER TABLE app_users ALTER COLUMN role TYPE VARCHAR(50);

-- 2. Columnas para rol dinámico, expiración temporal y cambio obligatorio de contraseña
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS role_id UUID REFERENCES roles(id) ON DELETE SET NULL;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS role_valid_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_role ON app_users(tenant_id, role_id);

-- 3. Catálogo de permisos del sistema agrupados por módulo
CREATE TABLE IF NOT EXISTS permissions (
    code VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    module VARCHAR(50) NOT NULL
);

-- 4. Asignación multi-sucursal para personal rotativo (ADR-0010)
CREATE TABLE IF NOT EXISTS user_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_branch UNIQUE (user_id, branch_id)
);

CREATE INDEX IF NOT EXISTS idx_user_branches_user ON user_branches(user_id);
CREATE INDEX IF NOT EXISTS idx_user_branches_tenant_user ON user_branches(tenant_id, user_id);
