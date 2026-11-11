-- ============================================================================
-- V13: Modalidades de Servicio, Delivery Fee y Contacto de Despacho (PB-032 / TASK-505)
-- ============================================================================

-- 1. Ampliar check constraint para order_type incorporando SCHEDULED_PICKUP
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_type_check;
ALTER TABLE orders ADD CONSTRAINT orders_order_type_check 
    CHECK (order_type IN ('DINE_IN', 'TAKEOUT', 'DELIVERY', 'SCHEDULED_PICKUP'));

-- 2. Columnas contextuales para despacho, contacto y flete
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_notes TEXT;

-- 3. Índice para filtrado ágil por modalidad en la sesión de caja
CREATE INDEX IF NOT EXISTS idx_orders_branch_type ON orders(tenant_id, branch_id, order_type);
