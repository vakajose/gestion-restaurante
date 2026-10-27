-- ============================================================================
-- V4__inventory_combos_promotions_modifiers.sql
-- 1. Soporte para INITIAL_STOCK en kardex_movements
-- 2. Soporte para Combos gastronómicos flexibles (packs con items fijos y opciones)
-- 3. Motor de Promociones Temporales (Happy Hour, días, horarios, modos)
-- 4. Grupos de Modificadores y Extras con impacto en Kardex
-- ============================================================================

-- 1. Actualización de restricción para kardex_movements
ALTER TABLE kardex_movements DROP CONSTRAINT IF EXISTS kardex_movements_movement_type_check;
ALTER TABLE kardex_movements ADD CONSTRAINT kardex_movements_movement_type_check CHECK (
    movement_type IN (
        'PURCHASE_IN', 
        'SALE_OUT', 
        'WASTE_ADJUSTMENT', 
        'TRANSFER_IN', 
        'TRANSFER_OUT', 
        'INITIAL_INVENTORY', 
        'INITIAL_STOCK',
        'PHYSICAL_COUNT_ADJUSTMENT'
    )
);

-- 2. Combos gastronómicos (Packs)
CREATE TABLE IF NOT EXISTS combos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    base_price NUMERIC(12, 2) NOT NULL CHECK (base_price >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_combo_code_scope UNIQUE (tenant_id, branch_id, code)
);

CREATE INDEX IF NOT EXISTS idx_combos_tenant_cat ON combos(tenant_id, category_id);

CREATE TABLE IF NOT EXISTS combo_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_id UUID NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    selection_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED' CHECK (selection_mode IN ('FIXED', 'CHOICE')),
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    default_dish_id UUID REFERENCES dishes(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_combo_items_combo ON combo_items(combo_id);

CREATE TABLE IF NOT EXISTS combo_item_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_item_id UUID NOT NULL REFERENCES combo_items(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    price_adjustment NUMERIC(12, 2) NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_combo_item_options_item ON combo_item_options(combo_item_id);

-- 3. Promociones Temporales Parametrizables
CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value NUMERIC(12, 2) NOT NULL CHECK (discount_value > 0),
    scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' CHECK (scope IN ('GLOBAL', 'CATEGORY', 'DISH')),
    target_category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    target_dish_id UUID REFERENCES dishes(id) ON DELETE SET NULL,
    application_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC' CHECK (application_mode IN ('AUTOMATIC', 'MANUAL')),
    days_of_week VARCHAR(30),
    start_time TIME,
    end_time TIME,
    start_date DATE,
    end_date DATE,
    min_order_amount NUMERIC(12, 2) DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_promotions_tenant ON promotions(tenant_id, is_active);

CREATE TABLE IF NOT EXISTS promotion_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    CONSTRAINT uq_promotion_branch UNIQUE (promotion_id, branch_id)
);

CREATE INDEX IF NOT EXISTS idx_promotion_branches_lookup ON promotion_branches(promotion_id, branch_id);

-- 4. Modificadores y Extras de Platos
CREATE TABLE IF NOT EXISTS modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    min_selectable INT NOT NULL DEFAULT 0 CHECK (min_selectable >= 0),
    max_selectable INT NOT NULL DEFAULT 1 CHECK (max_selectable >= 1),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_modifier_groups_tenant ON modifier_groups(tenant_id);

CREATE TABLE IF NOT EXISTS dish_modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_dish_modifier_group UNIQUE (dish_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_dish_modifier_groups_dish ON dish_modifier_groups(dish_id);

CREATE TABLE IF NOT EXISTS modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    additional_price NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (additional_price >= 0),
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE SET NULL,
    quantity_consumed NUMERIC(12, 4) DEFAULT 0 CHECK (quantity_consumed >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_modifiers_group ON modifiers(group_id, is_active);

-- Soporte en órdenes para modificadores seleccionados y combos
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS combo_id UUID REFERENCES combos(id) ON DELETE SET NULL;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS order_item_modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_id UUID REFERENCES modifiers(id) ON DELETE SET NULL,
    modifier_name VARCHAR(100) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE SET NULL,
    quantity_consumed NUMERIC(12, 4) DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_order_item_modifiers_item ON order_item_modifiers(order_item_id);
