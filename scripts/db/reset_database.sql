-- =============================================================================
-- SCRIPT DE LIMPIEZA DE TRANSACCIONES OPERATIVAS (RESET TRANSACCIONAL)
-- Proyecto: Restaurant POS ERP
-- Objetivo: Purgar ventas, comandas, turnos, arqueos, gastos y movimientos
--           preservando parametrización, usuarios, roles y catálogos maestros.
-- =============================================================================

BEGIN;

-- Desactivar temporalmente disparadores de integridad si es necesario
SET CONSTRAINTS ALL DEFERRED;

-- 1. Purgar Comandas y Ventas (POS)
TRUNCATE TABLE 
    order_item_modifiers,
    order_items,
    orders
RESTART IDENTITY CASCADE;

-- 2. Purgar Asignaciones de Menús Diarios y Cupos
TRUNCATE TABLE 
    shift_menu_quota_adjustments,
    shift_menu_items,
    shift_menu_assignments
RESTART IDENTITY CASCADE;

-- 3. Purgar Finanzas, Gastos Menores y Turnos de Caja
TRUNCATE TABLE 
    expenses,
    cash_shifts
RESTART IDENTITY CASCADE;

-- 4. Purgar Movimientos de Kardex y Existencias Físicas
TRUNCATE TABLE 
    kardex_movements,
    branch_stock
RESTART IDENTITY CASCADE;

-- 5. Purgar Eventos Transaccionales Asíncronos (Spring Modulith)
TRUNCATE TABLE 
    event_publication,
    event_publication_archive
RESTART IDENTITY CASCADE;

-- 6. Purgar Auditoría y Sesiones de Usuario
TRUNCATE TABLE 
    audit_logs,
    refresh_tokens
RESTART IDENTITY CASCADE;

COMMIT;

ANALYZE;
