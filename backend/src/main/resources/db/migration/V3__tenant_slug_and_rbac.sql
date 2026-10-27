-- ============================================================================
-- V3__tenant_slug_and_rbac.sql
-- 1. Slug único para tenants (soporte de login calificado por empresa)
-- 2. Tablas para Control de Acceso Basado en Recursos (Granular RBAC multi-tenant)
-- ============================================================================

-- 1. Tenant Slug
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS slug VARCHAR(60);

-- Si existen filas previas, derivar slug de legal_name o asignar valor por defecto
UPDATE tenants SET slug = LOWER(REGEXP_REPLACE(legal_name, '[^a-zA-Z0-9]+', '-', 'g')) WHERE slug IS NULL;
UPDATE tenants SET slug = 'demo-restaurant' WHERE slug IS NULL OR slug = '' OR slug = '-';

ALTER TABLE tenants ALTER COLUMN slug SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_slug ON tenants(slug);

-- 2. Roles por Tenant (permite roles del sistema y roles personalizados)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles(tenant_id);

-- 3. Permisos granulares asignados al rol (recurso:acción)
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_lookup ON role_permissions(tenant_id, role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_perm ON role_permissions(tenant_id, permission);
