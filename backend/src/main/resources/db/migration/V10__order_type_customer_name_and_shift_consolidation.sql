-- ============================================================================
-- V10: Modalidad de Pedido, Nombre de Cliente y Consolidación de Turnos
-- ============================================================================

-- 1. Ampliar estados de cash_shifts para supervisión y auditoría gerencial
ALTER TABLE cash_shifts DROP CONSTRAINT IF EXISTS cash_shifts_status_check;
ALTER TABLE cash_shifts ADD CONSTRAINT cash_shifts_status_check 
    CHECK (status IN ('OPEN', 'CLOSED', 'CONSOLIDATED'));

ALTER TABLE cash_shifts ADD COLUMN IF NOT EXISTS consolidated_by UUID REFERENCES app_users(id);
ALTER TABLE cash_shifts ADD COLUMN IF NOT EXISTS consolidated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE cash_shifts ADD COLUMN IF NOT EXISTS consolidation_notes TEXT;

-- 2. Modalidad de pedido y cliente en orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) DEFAULT 'DINE_IN' 
    CHECK (order_type IN ('DINE_IN', 'TAKEOUT', 'DELIVERY'));
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_orders_type ON orders(tenant_id, branch_id, order_type);
