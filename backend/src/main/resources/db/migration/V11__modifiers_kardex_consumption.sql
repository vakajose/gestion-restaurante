-- ============================================================================
-- V11: Soporte para SALE_CONSUMPTION en kardex_movements
-- Permite registrar deducciones de modificadores con impacto físico en almacén
-- ============================================================================

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
        'PHYSICAL_COUNT_ADJUSTMENT'
    )
);
