---
title: "UX Móvil Táctil en POS y Flujo de Ajuste de Inventario por Lote"
category: "behavior-flow"
module: "pos"
tags: ["state-machine", "ux", "mobile", "bottom-sheet", "pos", "inventory", "batch-calculator", "adr-0012"]
last_updated: "2026-11-17"
status: "verified"
---

# UX Móvil Táctil en POS y Flujo de Ajuste de Inventario por Lote

Comportamiento de adaptación táctil para dispositivos móviles (`< 1024px`) en el Punto de Venta (`app-bottom-sheet`) y el flujo interactivo de carga inicial y ajuste de stock en [`InventoryComponent`](../../../../frontend/src/app/features/inventory/inventory.component.ts) con calculadora bidireccional costo total de lote $\leftrightarrow$ costo unitario (`POST /api/v1/inventory/stock/initial` y `POST /api/v1/inventory/stock/adjust`).

## 1. Máquina de Estados UX Móvil en POS (`ADR-0012`)

```mermaid
stateDiagram-v2
    [*] --> CatalogBrowse : Viewport móvil (< 1024px) grilla táctil y barra sticky

    CatalogBrowse --> DishCustomizeModal : Tap en plato con modificadores o combo
    DishCustomizeModal --> CatalogBrowse : Agregar ítem al carrito

    CatalogBrowse --> CartBottomSheet : Tap en barra de resumen (app-bottom-sheet)
    CartBottomSheet --> CatalogBrowse : Deslizar hacia abajo / Cerrar sheet

    CartBottomSheet --> CheckoutConfirm : Editar cantidades, notas, modalidad o promo
    CheckoutConfirm --> CatalogBrowse : Pago confirmado -> clearCart() + Ticket
```

## 2. Flujo Interactivo de Stock Inicial y Ajuste con Calculadora de Lote (`InventoryComponent`)

```mermaid
sequenceDiagram
    autonumber
    actor Encargado as Encargado / Admin
    participant InvUI as InventoryComponent (Angular Signals)
    participant InvSvc as InventoryService
    participant InvCtrl as InventoryController (/api/v1/inventory)
    participant Kardex as KardexService + PostgreSQL

    Encargado->>InvUI: Abrir modal de Stock Inicial o Ajuste Físico
    InvUI->>InvUI: Inicializar signals: quantity, unitCost, totalCost (qty * unitCost)

    alt Usuario ingresa Costo Total del Lote (ej. factura de compra)
        Encargado->>InvUI: Editar totalCost (onInitialTotalCostChange / onAdjustTotalCostChange)
        InvUI->>InvUI: Recalcular unitCost = (totalCost / quantity) a 4 decimales
    else Usuario edita Cantidad o Costo Unitario
        Encargado->>InvUI: Editar quantity o unitCost (onInitialQuantityOrUnitCostChange / onAdjustQuantityOrUnitCostChange)
        InvUI->>InvUI: Recalcular totalCost = (quantity * unitCost) a 2 decimales
    end

    alt Alta de Inventario Inicial
        Encargado->>InvUI: Confirmar Stock Inicial (confirmInitialStock)
        InvUI->>InvSvc: registerInitialStock({ingredientId, quantity, unitCost}, branchId)
        InvSvc->>InvCtrl: POST /api/v1/inventory/stock/initial?branchId={id}
        InvCtrl->>Kardex: Registrar BranchStock + KardexMovement (INITIAL_STOCK)
        InvCtrl-->>InvUI: HTTP 201 Created
    else Ajuste o Conteo Físico
        Encargado->>InvUI: Confirmar Ajuste (confirmAdjustment)
        InvUI->>InvSvc: adjustStock({ingredientId, newQuantity, newUnitCost, reason}, branchId)
        InvSvc->>InvCtrl: POST /api/v1/inventory/stock/adjust?branchId={id}
        InvCtrl->>Kardex: Actualizar BranchStock + KardexMovement (PHYSICAL_COUNT_ADJUSTMENT)
        InvCtrl-->>InvUI: HTTP 200 OK (StockBalanceDto)
    end

    InvUI->>InvUI: Cerrar modal y refrescar existencias/Kardex
```

## Precisiones Verificadas

1. **Rutas Reales de Inventario:** Los endpoints correctos en `InventoryController` son `GET /api/v1/inventory/stock`, `GET /api/v1/inventory/kardex`, `POST /api/v1/inventory/stock/adjust` y `POST /api/v1/inventory/stock/initial` (no existe `/api/v1/inventory/adjustments`).
2. **Precisión Numérica de Costos:** El cálculo bidireccional preserva **4 decimales** en el costo unitario promedio (`toFixed(4)`, alineado a `NUMERIC(12, 4)` en PostgreSQL) y **2 decimales** en el monto total del lote (`toFixed(2)`).

## Enlaces Relacionados

- [ADR-0012: Responsividad Móvil y UX Táctil](../../../adr/0012_mobile_responsiveness_and_touch_ux.md)
- [C4 Nivel 3: Frontend Angular](../01-c4-system/c4_level3_frontend_angular.md)
- [Actividad de Deducción Kardex](./activity_inventory_deduction_strategy.md)
- [Casos de Uso de Inventario](../03-use-cases/use_cases_inventory.md)
