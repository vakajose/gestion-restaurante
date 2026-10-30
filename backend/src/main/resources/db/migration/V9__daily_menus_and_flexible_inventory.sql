-- ============================================================================
-- V9__daily_menus_and_flexible_inventory.sql
-- Modelo Relacional de Menús Diarios, Plantillas de Turno con Cupos Reactivos
-- y Estrategias Flexibles de Inventario (ADR-0015 & PB-034 / PB-044)
-- ============================================================================

-- 1. Tablas de Plantillas Reutilizables de Menú
CREATE TABLE IF NOT EXISTS daily_menus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    day_of_week VARCHAR(15),
    price_mode VARCHAR(25) NOT NULL DEFAULT 'INDIVIDUAL_ITEMS',
    package_price NUMERIC(12,2),
    is_template BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS daily_menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id UUID NOT NULL REFERENCES daily_menus(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
    course_type VARCHAR(20) NOT NULL,
    suggested_quota INT NOT NULL DEFAULT 0 CHECK (suggested_quota >= 0),
    override_price NUMERIC(12,2),
    is_mandatory_in_package BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);

-- 2. Tablas de Asignación de Turno y Cupos Reactivos en Vivo
CREATE TABLE IF NOT EXISTS shift_menu_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    cash_shift_id UUID NOT NULL REFERENCES cash_shifts(id) ON DELETE CASCADE,
    menu_id UUID REFERENCES daily_menus(id) ON DELETE SET NULL,
    assigned_by UUID NOT NULL REFERENCES app_users(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shift_menu_assignment UNIQUE (cash_shift_id)
);

CREATE TABLE IF NOT EXISTS shift_menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_menu_assignment_id UUID NOT NULL REFERENCES shift_menu_assignments(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
    course_type VARCHAR(20) NOT NULL,
    initial_quota INT NOT NULL CHECK (initial_quota >= 0),
    current_quota INT NOT NULL CHECK (current_quota >= 0),
    sold_count INT NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
    effective_price NUMERIC(12,2) NOT NULL,
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_shift_menu_item UNIQUE (shift_menu_assignment_id, dish_id)
);

CREATE TABLE IF NOT EXISTS shift_menu_quota_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_menu_item_id UUID NOT NULL REFERENCES shift_menu_items(id) ON DELETE CASCADE,
    adjusted_by UUID NOT NULL REFERENCES app_users(id),
    quantity_delta INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Modificaciones a Tablas Maestras para Inventario Flexible (PB-044)
ALTER TABLE dishes
    ADD COLUMN IF NOT EXISTS inventory_strategy VARCHAR(30) NOT NULL DEFAULT 'RECIPE_BOM_STRICT',
    ADD COLUMN IF NOT EXISTS allow_negative_stock_override BOOLEAN;

ALTER TABLE branch_stock
    ALTER COLUMN ingredient_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS dish_id UUID REFERENCES dishes(id) ON DELETE RESTRICT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_branch_stock_target'
    ) THEN
        ALTER TABLE branch_stock
            ADD CONSTRAINT chk_branch_stock_target CHECK (
                (ingredient_id IS NOT NULL AND dish_id IS NULL) OR
                (ingredient_id IS NULL AND dish_id IS NOT NULL)
            );
    END IF;
END $$;

ALTER TABLE kardex_movements
    ALTER COLUMN ingredient_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS dish_id UUID REFERENCES dishes(id) ON DELETE RESTRICT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_kardex_target'
    ) THEN
        ALTER TABLE kardex_movements
            ADD CONSTRAINT chk_kardex_target CHECK (
                (ingredient_id IS NOT NULL AND dish_id IS NULL) OR
                (ingredient_id IS NULL AND dish_id IS NOT NULL)
            );
    END IF;
END $$;

-- 4. Índices de Rendimiento
CREATE INDEX IF NOT EXISTS idx_daily_menus_tenant ON daily_menus(tenant_id, is_active, is_template);
CREATE INDEX IF NOT EXISTS idx_daily_menu_items_menu ON daily_menu_items(menu_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_shift_menu_assignment_shift ON shift_menu_assignments(cash_shift_id);
CREATE INDEX IF NOT EXISTS idx_shift_menu_items_lookup ON shift_menu_items(shift_menu_assignment_id, dish_id);
CREATE INDEX IF NOT EXISTS idx_shift_quota_adjustments_item ON shift_menu_quota_adjustments(shift_menu_item_id, created_at);
CREATE INDEX IF NOT EXISTS idx_branch_stock_dish ON branch_stock(branch_id, dish_id) WHERE dish_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kardex_movements_dish ON kardex_movements(branch_id, dish_id) WHERE dish_id IS NOT NULL;
