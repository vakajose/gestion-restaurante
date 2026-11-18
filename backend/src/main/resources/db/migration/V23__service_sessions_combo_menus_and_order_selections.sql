-- ============================================================================
-- V23__service_sessions_combo_menus_and_order_selections.sql
-- Jornada Operativa de Sucursal (desacoplada de cash_shifts),
-- Combos Fijos y Dinámicos (MENU_COURSE), Menús del Día con cupos ilimitados
-- o restringidos a combo, y registro de selecciones reales en order_items.
-- ============================================================================

-- 1. Jornada Operativa / Servicio del Día por Sucursal
CREATE TABLE IF NOT EXISTS branch_service_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    menu_mode VARCHAR(25) NOT NULL DEFAULT 'FULL_CATALOG'
        CHECK (menu_mode IN ('FULL_CATALOG', 'DAILY_MENU')),
    daily_menu_id UUID REFERENCES daily_menus(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIGURED'
        CHECK (status IN ('CONFIGURED', 'ACTIVE', 'CLOSED')),
    configured_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_branch_active_service_session
    ON branch_service_sessions(branch_id)
    WHERE status IN ('CONFIGURED', 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_branch_service_sessions_lookup
    ON branch_service_sessions(tenant_id, branch_id, service_date, status);

-- 2. Enlace opcional de cash_shifts a su jornada padre
ALTER TABLE cash_shifts
    ADD COLUMN IF NOT EXISTS service_session_id UUID REFERENCES branch_service_sessions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_cash_shifts_service_session
    ON cash_shifts(service_session_id);

-- 3. Desacoplar shift_menu_assignments de un cajero único para compartir por jornada
ALTER TABLE shift_menu_assignments
    ADD COLUMN IF NOT EXISTS service_session_id UUID REFERENCES branch_service_sessions(id) ON DELETE CASCADE;

ALTER TABLE shift_menu_assignments
    ALTER COLUMN cash_shift_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_shift_menu_assignment_session
    ON shift_menu_assignments(service_session_id)
    WHERE service_session_id IS NOT NULL;

-- 4. Cupos ilimitados y bandera de venta individual en ítems de Menú del Día
ALTER TABLE daily_menu_items
    ADD COLUMN IF NOT EXISTS allow_individual_sale BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_unlimited BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shift_menu_items
    ADD COLUMN IF NOT EXISTS allow_individual_sale BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_unlimited BOOLEAN NOT NULL DEFAULT FALSE;

-- 5. Combos habilitados dentro de cada plantilla de Menú del Día
CREATE TABLE IF NOT EXISTS daily_menu_combos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id UUID NOT NULL REFERENCES daily_menus(id) ON DELETE CASCADE,
    combo_id UUID NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_menu_combo UNIQUE (menu_id, combo_id)
);

CREATE INDEX IF NOT EXISTS idx_daily_menu_combos_menu
    ON daily_menu_combos(menu_id);

-- 6. Bloques dinámicos basados en tiempos de menú (MENU_COURSE) en combos
ALTER TABLE combo_items
    ADD COLUMN IF NOT EXISTS selection_source VARCHAR(25) NOT NULL DEFAULT 'FIXED'
        CHECK (selection_source IN ('FIXED', 'SPECIFIC_OPTIONS', 'MENU_COURSE')),
    ADD COLUMN IF NOT EXISTS course_type VARCHAR(20);

UPDATE combo_items
SET selection_source = CASE
    WHEN UPPER(selection_mode) = 'CHOICE' THEN 'SPECIFIC_OPTIONS'
    ELSE 'FIXED'
END
WHERE selection_source = 'FIXED';

-- 7. Soporte de detalle real seleccionado dentro de combos y menús armables en órdenes
ALTER TABLE order_items
    ALTER COLUMN dish_id DROP NOT NULL;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS daily_menu_id UUID REFERENCES daily_menus(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS order_item_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    slot_label VARCHAR(100) NOT NULL,
    selected_dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price_adjustment NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_item_selections_item
    ON order_item_selections(order_item_id);
