-- ============================================================================
-- V5__system_parameter_values.sql
-- Sistema Unificado de Parametrización Multinivel (ADR-0008)
-- Almacenamiento disperso (Sparse Storage), Hot-Reload y Auditoría
-- ============================================================================

CREATE TABLE IF NOT EXISTS system_parameter_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_key VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('GLOBAL', 'TENANT', 'BRANCH')),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE CASCADE,
    parameter_value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    audit_reason VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_parameter_scope ON system_parameter_values (
    parameter_key, 
    scope, 
    COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::UUID), 
    COALESCE(branch_id, '00000000-0000-0000-0000-000000000000'::UUID)
);

CREATE INDEX IF NOT EXISTS idx_parameter_values_lookup ON system_parameter_values(parameter_key, scope, tenant_id, branch_id);
