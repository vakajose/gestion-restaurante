---
title: "Flujo de Jornadas de Servicio, Menús del Día, Combos Dinámicos y Cupos"
category: "behavior-flow"
module: "catalog"
tags: ["sequence", "daily-menu", "service-sessions", "combos", "menu-course", "quotas", "optimistic-lock", "pos"]
last_updated: "2026-11-17"
status: "verified"
---

# Flujo de Jornadas de Servicio, Menús del Día, Combos Dinámicos y Cupos

Secuencia operativa introducida en `V9` y consolidada en `V23`/`V24` para desacoplar la oferta gastronómica del turno individual de cajero mediante **`BranchServiceSession`** (`CONFIGURED -> ACTIVE -> CLOSED`), soportar operación en **`FULL_CATALOG`** o **`DAILY_MENU`**, habilitar combos dinámicos (`MENU_COURSE`) e imponer el control atómico de raciones por venta POS sin sockets/SSE ficticios (refresco estándar por consulta HTTP de catálogo/turno activo).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin / Encargado
    actor Cajero as Cajero
    participant CatalogUI as DailyMenus / CashControl UI
    participant MenuCtrl as DailyMenuController (/api/v1/catalog)
    participant FinCtrl as FinanceController (/api/v1/finance)
    participant CashSvc as CashShiftService
    participant MenuSvc as DailyMenuServiceImpl
    participant OrderSvc as OrderServiceImpl
    participant DB as PostgreSQL (branch_service_sessions, shift_menu_items)

    %% 1. Configuración previa o automática de Jornada Operativa
    Admin->>CatalogUI: Seleccionar modalidad de jornada (FULL_CATALOG o DAILY_MENU + plantilla)
    CatalogUI->>MenuCtrl: POST /api/v1/catalog/service-sessions/configure
    MenuCtrl->>MenuSvc: configureServiceSession(tenantId, userId, request)
    MenuSvc->>DB: Crear/actualizar BranchServiceSession (status: CONFIGURED, service_date local)

    opt menuMode == DAILY_MENU
        MenuSvc->>DB: Borrar snapshot previo si existe e instanciar ShiftMenuAssignment + ShiftMenuItem[]
        Note right of MenuSvc: Preserva isUnlimited, allowIndividualSale y effectivePrice (override o catálogo)
    end
    MenuSvc-->>CatalogUI: BranchServiceSessionDto

    %% 2. Apertura de Turno de Caja acoplada a la Jornada
    Cajero->>FinCtrl: POST /api/v1/finance/shifts/open (initialCash, menuTemplateId opcional)
    FinCtrl->>CashSvc: openShift(request)
    CashSvc->>CashSvc: Leer finance.auto_open_full_catalog_session
    CashSvc->>DB: Persistir CashShift (status: OPEN)
    CashSvc->>MenuSvc: attachCashShiftToServiceSession(..., allowAutoFullCatalog)

    alt Existe jornada CONFIGURED o ACTIVE en la sucursal
        MenuSvc->>DB: Promover CONFIGURED -> ACTIVE (activated_at = now())
    else No existe jornada y menuTemplateId != null
        MenuSvc->>DB: Crear BranchServiceSession DAILY_MENU (ACTIVE) + ShiftMenuAssignment
    else No existe jornada y allowAutoFullCatalog == true
        MenuSvc->>DB: Crear BranchServiceSession FULL_CATALOG (ACTIVE)
    else No existe jornada y allowAutoFullCatalog == false
        MenuSvc-->>CashSvc: throw BusinessRuleException (Admin debe configurar jornada)
    end

    CashSvc->>DB: Enlazar cash_shifts.service_session_id = session.id

    %% 3. Venta POS con Combos Dinámicos y Decremento Atómico de Cupos
    Cajero->>OrderSvc: POST /api/v1/pos/orders (items con dishId o comboId + selections[])
    OrderSvc->>OrderSvc: Agrupar cantidades requeridas por dishId (directos + selections[].selectedDishId)

    loop Por cada dishId consumido
        OrderSvc->>MenuSvc: enforceAndDecrementQuota(branchId, cashShiftId, dishId, qty)
        MenuSvc->>DB: Buscar ShiftMenuItem por cashShiftId o serviceSessionId o sesión abierta
        alt Plato no pertenece al menú activo (carta abierta)
            MenuSvc-->>OrderSvc: Retorno inmediato sin límite de raciones
        else Plato con isUnlimited == true
            MenuSvc->>DB: Incrementar sold_count (sin bloquear por current_quota)
        else Cupo insuficiente o isSoldOut == true
            MenuSvc-->>OrderSvc: throw ItemSoldOutException (HTTP 409)
        else Cupo suficiente (Optimistic Lock con @Version)
            MenuSvc->>DB: UPDATE shift_menu_items SET current_quota = current_quota - qty, sold_count = sold_count + qty
            alt updatedRows == 0 (carrera concurrente)
                MenuSvc->>DB: Recargar y lanzar ItemSoldOutException o ResourceConflictException
            end
        end
    end

    %% 4. Ajustes manuales y cierre de jornada
    opt Ajuste manual de raciones en cocina
        Admin->>MenuCtrl: POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota
        MenuCtrl->>MenuSvc: Registrar ShiftMenuQuotaAdjustment (+delta o -delta)
    end

    Admin->>MenuCtrl: POST /api/v1/catalog/service-sessions/{sessionId}/close
    MenuCtrl->>MenuSvc: closeServiceSession -> status CLOSED
```

## Capacidades Clave del Modelo `V23`/`V24`

1. **Desacoplamiento Multi-Cajero (`branch_service_sessions`):** Varios turnos `cash_shifts` simultáneos (`INDIVIDUAL_PER_CASHIER`) comparten una única `BranchServiceSession` activa en la sucursal y consumen el mismo pool de `shift_menu_items` (`service_session_id`).
2. **Modos `FULL_CATALOG` vs `DAILY_MENU`:** Con el parámetro `finance.auto_open_full_catalog_session` activado (`true`), abrir caja sin jornada previa inicia automáticamente en modo `FULL_CATALOG`; en modo estricto (`false`), exige configuración previa del administrador.
3. **Combos Dinámicos y Restricciones por Plato (`V23`):**
   - `daily_menu_combos` habilita combos completos dentro de una plantilla de Menú del Día.
   - `combo_items.selection_source = 'MENU_COURSE'` vincula un bloque del combo a un tiempo (`course_type`) del menú activo (ej. Sopas o Segundos del día).
   - `allow_individual_sale = false` permite ofrecer un plato solo dentro de combos/paquetes sin habilitar su venta suelta en el catálogo individual POS.
   - `is_unlimited = true` excluye al ítem de bloqueo por agotamiento de cupo (`ItemSoldOutException`), manteniendo conteo de `sold_count`.
4. **Sincronización de Cupos en Cliente:** El frontend consulta el menú y la jornada actual mediante llamadas HTTP estándar (`GET /api/v1/catalog/service-sessions/current`, `GET /api/v1/catalog/shifts/{shiftId}/menu`) y tras cada cobro o error `ItemSoldOutException`; no existe infraestructura WebSocket ni SSE activa.

## Enlaces Relacionados

- [Flujo de Venta POS y Deducción](./sequence_pos_order_deduction.md)
- [Flujo de Turno de Caja y Gastos](./state_shift_and_expense_workflow.md)
- [ERD Catálogo y Jornadas (`branch_service_sessions`, `daily_menu_combos`)](../02-domain-and-data/database_erd.md)
- [Casos de Uso de Catálogo](../03-use-cases/use_cases_catalog.md)
- [ADR-0015: Menús Diarios e Inventario Flexible](../../../adr/0015_daily_menus_and_flexible_dish_inventory.md)
- [ADR-0031: Jornadas de Servicio y Combos Dinámicos](../../../adr/0031_branch_service_sessions_and_dynamic_combos.md)
