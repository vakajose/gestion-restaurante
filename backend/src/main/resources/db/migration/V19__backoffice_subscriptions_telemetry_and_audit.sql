-- ============================================================================
-- V19__backoffice_subscriptions_telemetry_and_audit.sql
-- Motor SaaS del Backoffice: roles en platform_admins, planes de suscripción,
-- suscripciones por inquilino con gracia condicional y overrides de cuota,
-- cargos/mora (uso por adelantado), pagos, read-model CQRS de telemetría y auditoría (ADR-0029).
-- ============================================================================

ALTER TABLE platform_admins
    ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'SUPERADMIN';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_platform_admin_role'
    ) THEN
        ALTER TABLE platform_admins
            ADD CONSTRAINT chk_platform_admin_role CHECK (role IN ('SUPERADMIN', 'PLATFORM_OPERATOR'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('WEEKLY', 'MONTHLY', 'ANNUAL', 'CUSTOM')),
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'BOB',
    max_branches INTEGER CHECK (max_branches IS NULL OR max_branches >= 0),
    max_users INTEGER CHECK (max_users IS NULL OR max_users >= 0),
    max_dishes INTEGER CHECK (max_dishes IS NULL OR max_dishes >= 0),
    max_inventory_items INTEGER CHECK (max_inventory_items IS NULL OR max_inventory_items >= 0),
    max_monthly_orders INTEGER CHECK (max_monthly_orders IS NULL OR max_monthly_orders >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id UUID REFERENCES subscription_plans(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('TRIAL', 'ACTIVE', 'GRACE_PERIOD', 'SUSPENDED', 'CANCELLED')),
    suspension_mode VARCHAR(30) NOT NULL DEFAULT 'NONE' CHECK (suspension_mode IN ('NONE', 'POS_AND_SHIFT_ONLY', 'TOTAL')),
    auto_suspend_mode_on_expire VARCHAR(30) NOT NULL DEFAULT 'POS_AND_SHIFT_ONLY' CHECK (auto_suspend_mode_on_expire IN ('POS_AND_SHIFT_ONLY', 'TOTAL')),
    suspension_reason TEXT,
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    grace_until TIMESTAMP WITH TIME ZONE,
    grace_max_orders INTEGER CHECK (grace_max_orders IS NULL OR grace_max_orders > 0),
    grace_orders_used INTEGER NOT NULL DEFAULT 0 CHECK (grace_orders_used >= 0),
    grace_notes TEXT,
    override_max_branches INTEGER CHECK (override_max_branches IS NULL OR override_max_branches >= 0),
    override_max_users INTEGER CHECK (override_max_users IS NULL OR override_max_users >= 0),
    override_max_dishes INTEGER CHECK (override_max_dishes IS NULL OR override_max_dishes >= 0),
    override_max_inventory_items INTEGER CHECK (override_max_inventory_items IS NULL OR override_max_inventory_items >= 0),
    override_max_monthly_orders INTEGER CHECK (override_max_monthly_orders IS NULL OR override_max_monthly_orders >= 0),
    updated_by_admin_id UUID REFERENCES platform_admins(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_status
    ON tenant_subscriptions (status, current_period_end);

CREATE TABLE IF NOT EXISTS subscription_charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id UUID REFERENCES subscription_plans(id) ON DELETE SET NULL,
    plan_name_snapshot VARCHAR(120) NOT NULL,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    amount_due NUMERIC(12, 2) NOT NULL CHECK (amount_due >= 0),
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'BOB',
    charge_status VARCHAR(20) NOT NULL CHECK (charge_status IN ('PENDING', 'PARTIAL', 'PAID', 'WAIVED')),
    due_date DATE,
    notes TEXT,
    created_by_admin_id UUID REFERENCES platform_admins(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_subscription_charges_tenant_status
    ON subscription_charges (tenant_id, charge_status, created_at);

CREATE TABLE IF NOT EXISTS subscription_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    charge_id UUID REFERENCES subscription_charges(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'BOB',
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('TRANSFER', 'QR', 'CASH', 'CARD', 'ADJUSTMENT')),
    reference_code VARCHAR(120),
    receipt_url TEXT,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by_admin_id UUID REFERENCES platform_admins(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_tenant_date
    ON subscription_payments (tenant_id, paid_at DESC);

CREATE TABLE IF NOT EXISTS tenant_daily_telemetry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    paid_orders_count INTEGER NOT NULL DEFAULT 0,
    gross_sales_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    cancelled_orders_count INTEGER NOT NULL DEFAULT 0,
    last_event_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tenant_daily_telemetry UNIQUE (tenant_id, branch_id, stat_date)
);

CREATE INDEX IF NOT EXISTS idx_tenant_daily_telemetry_lookup
    ON tenant_daily_telemetry (tenant_id, stat_date DESC);

CREATE TABLE IF NOT EXISTS platform_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_admin_id UUID REFERENCES platform_admins(id) ON DELETE SET NULL,
    actor_username VARCHAR(100) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50),
    target_id VARCHAR(100),
    details_json TEXT,
    ip_address VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_audit_logs_created
    ON platform_audit_logs (created_at DESC);

-- Semilla idempotente de planes base editables desde Backoffice
INSERT INTO subscription_plans (
    code, name, description, billing_cycle, duration_days, price, currency,
    max_branches, max_users, max_dishes, max_inventory_items, max_monthly_orders, is_active
) VALUES
    ('TRIAL_14D', 'Prueba Inicial 14 Días', 'Plan inicial automático para onboarding de nuevos restaurantes', 'CUSTOM', 14, 0.00, 'BOB', 3, 15, 100, 200, 2000, TRUE),
    ('WEEKLY_FLEX', 'Plan Semanal Flexible', 'Ciclo semanal ideal para operación por adelantado o crédito corto', 'WEEKLY', 7, 75.00, 'BOB', 1, 8, 60, 120, 1200, TRUE),
    ('MONTHLY_STANDARD', 'Plan Mensual Estándar', 'Plan mensual para restaurantes con 1-2 sucursales', 'MONTHLY', 30, 250.00, 'BOB', 2, 15, 150, 300, 5000, TRUE),
    ('MONTHLY_PRO', 'Plan Mensual Multi-Sucursal', 'Plan ampliado para cadenas y alta demanda POS', 'MONTHLY', 30, 550.00, 'BOB', 10, 60, NULL, NULL, NULL, TRUE)
ON CONFLICT (code) DO NOTHING;
