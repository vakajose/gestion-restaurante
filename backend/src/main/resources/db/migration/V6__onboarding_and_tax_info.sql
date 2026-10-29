-- ============================================================================
-- V6__onboarding_and_tax_info.sql
-- Onboarding Autónomo, NIT opcional con índice único parcial,
-- georreferenciación de sucursal, nombre completo de usuario y preferencias (ADR-0009)
-- ============================================================================

-- 1. Modificar tenants (NIT opcional + logo_url + índice único parcial)
ALTER TABLE tenants ALTER COLUMN nit_or_tax_id DROP NOT NULL;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_url TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_nit ON tenants(nit_or_tax_id) WHERE nit_or_tax_id IS NOT NULL AND nit_or_tax_id != '';

-- 2. Modificar branches (georreferenciación y moneda local)
ALTER TABLE branches ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'BOB';

-- 3. Modificar app_users (nombre completo del propietario/colaborador)
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);

-- 4. Modificar user_preferences (estado del tour de bienvenida)
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS onboarding_tour_completed BOOLEAN NOT NULL DEFAULT FALSE;
