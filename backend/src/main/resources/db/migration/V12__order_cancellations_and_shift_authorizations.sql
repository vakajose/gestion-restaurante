-- ============================================================================
-- V12: Cancelación de Pedidos, Reintegro de Efectivo y Autorizaciones de Turno
-- Requerimiento: PB-031 / ADR-0019
-- ============================================================================

-- 1. Campos de Cancelación y Trazabilidad en orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_notes TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES app_users(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_authorized_by UUID REFERENCES app_users(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_method VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS waste_policy_applied VARCHAR(30);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_cancellation_reason_check;
ALTER TABLE orders ADD CONSTRAINT orders_cancellation_reason_check CHECK (
    cancellation_reason IS NULL OR cancellation_reason IN (
        'CUSTOMER_RETRACTED',
        'TYPING_ERROR',
        'KITCHEN_ISSUE',
        'QUALITY_COMPLAINT',
        'DUPLICATE_ORDER'
    )
);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_refund_method_check;
ALTER TABLE orders ADD CONSTRAINT orders_refund_method_check CHECK (
    refund_method IS NULL OR refund_method IN ('CASH', 'DIGITAL')
);

CREATE INDEX IF NOT EXISTS idx_orders_cancelled ON orders(tenant_id, branch_id, order_status) WHERE order_status = 'CANCELLED';

-- 2. PIN Numérico de Supervisor en app_users (Hash BCrypt)
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(100);

-- 3. Acumulador de Devoluciones de Efectivo en cash_shifts
ALTER TABLE cash_shifts ADD COLUMN IF NOT EXISTS total_cash_refunds NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- 4. Ampliar tipos de movimiento de kardex con CANCELLATION_REVERSAL
ALTER TABLE kardex_movements DROP CONSTRAINT IF EXISTS kardex_movements_movement_type_check;
ALTER TABLE kardex_movements ADD CONSTRAINT kardex_movements_movement_type_check CHECK (
    movement_type IN (
        'PURCHASE_IN', 
        'SALE_OUT', 
        'SALE_CONSUMPTION',
        'WASTE_ADJUSTMENT', 
        'TRANSFER_IN', 
        'TRANSFER_OUT', 
        'INITIAL_INVENTORY', 
        'INITIAL_STOCK',
        'PHYSICAL_COUNT_ADJUSTMENT',
        'CANCELLATION_REVERSAL'
    )
);

-- 5. Tabla de Autorizaciones de Turno (Motor Unificado de Autorizaciones de Finanzas)
CREATE TABLE IF NOT EXISTS shift_authorizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    cash_shift_id UUID REFERENCES cash_shifts(id) ON DELETE SET NULL,
    request_type VARCHAR(30) NOT NULL CHECK (request_type IN ('EXPENSE', 'ORDER_CANCELLATION')),
    reference_id UUID,
    requested_by UUID NOT NULL REFERENCES app_users(id),
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    justification TEXT,
    decided_by UUID REFERENCES app_users(id),
    decision_notes TEXT,
    decision_channel VARCHAR(30) CHECK (decision_channel IS NULL OR decision_channel IN ('IN_SITU_PIN', 'REMOTE_SESSION')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_shift_authorizations_lookup ON shift_authorizations(tenant_id, branch_id, status);
CREATE INDEX IF NOT EXISTS idx_shift_authorizations_shift ON shift_authorizations(cash_shift_id);
CREATE INDEX IF NOT EXISTS idx_shift_authorizations_ref ON shift_authorizations(reference_id);

-- 6. Tabla Buffer de Platos Preparados (Vitrina de Platos Rescatables)
CREATE TABLE IF NOT EXISTS prepared_dish_buffer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES dishes(id) ON DELETE RESTRICT,
    quantity INT NOT NULL CHECK (quantity > 0),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prepared_dish_buffer_dish ON prepared_dish_buffer(tenant_id, branch_id, dish_id, is_consumed);
