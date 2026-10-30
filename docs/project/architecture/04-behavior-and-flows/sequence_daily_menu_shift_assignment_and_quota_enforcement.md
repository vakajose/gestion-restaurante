---
title: "Diagrama de Secuencia: Asignación de Menú al Turno y Control Atómico de Cupos en POS"
category: "architecture"
level: "Sequence"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - daily-menus
  - pos
  - optimistic-locking
  - acid
last_updated: "2026-10-30"
status: "verified"
---

# ⏱️ Diagrama de Secuencia: Asignación de Menú al Turno y Control Atómico de Cupos en POS (PB-034)

Este documento modela las interacciones transaccionales y de concurrencia entre los módulos `pos`, `catalog`, `finance` y la base de datos PostgreSQL, garantizando el cumplimiento de las propiedades **ACID** y el control de concurrencia mediante **Optimistic Locking** (`@Version`) al vender raciones de menús diarios en el punto de venta.

---

## 🧭 1. Resumen de Flujos Cubiertos

1. **Apertura de Turno con Instanciación de Plantilla:**
   - El cajero/supervisor abre el turno de caja (`cash_shifts`).
   - Selecciona una plantilla de menú diario (`daily_menus`).
   - El sistema clona los ítems a `shift_menu_assignments` y `shift_menu_items` con cupos iniciales independientes, sin mutar la plantilla maestra.
2. **Cobro Concurrente con Verificación Atómica de Cupo:**
   - La orden se procesa en una transacción `@Transactional`.
   - Se ejecuta el decremento condicional en SQL: `WHERE current_quota >= :qty AND version = :version`.
   - Si se detecta venta concurrente o agotamiento, se arroja `ItemSoldOutException` (HTTP 409 `ITEM_SOLD_OUT`) con rollback transaccional total.
3. **Ampliación Supervisada de Cupo en Vivo:**
   - Cocina elabora raciones extra durante el turno.
   - Un usuario con privilegio `FINANCE_SHIFTS` o `BRANCH_MANAGER` amplía el cupo (+N raciones).
   - Se audita el cambio en `shift_menu_quota_adjustments` de forma inmutable.

---

## 📊 2. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero / POS (Angular 22)
    actor Supervisor as Supervisor de Sucursal
    participant POSCtrl as PosOrderController / ShiftController
    participant OrderSvc as OrderServiceImpl (@Transactional)
    participant MenuApi as DailyMenuPublicApi
    participant QuotaRepo as ShiftMenuItemRepository
    participant AuditRepo as ShiftMenuQuotaAdjustmentRepository
    participant EventPub as ApplicationEventPublisher
    participant DB as PostgreSQL (ACID Engine)

    %% -------------------------------------------------------------
    %% SECCIÓN 1: APERTURA DE TURNO E INSTANCIACIÓN DE MENÚ
    %% -------------------------------------------------------------
    rect rgb(240, 248, 255)
        note over Cajero, DB: FASE 1: Apertura de Turno e Instanciación de Menú Diario
        Cajero->>POSCtrl: POST /api/v1/finance/shifts/open<br/>{initialCash: 100.00, menuTemplateId: UUID, customQuotas: [{dishId, quota: 25}]}
        POSCtrl->>OrderSvc: openShiftWithMenu(shiftData)
        OrderSvc->>DB: INSERT INTO cash_shifts (status = 'OPEN', opened_at, initial_cash)
        OrderSvc->>MenuApi: instantiateTemplateForShift(shiftId, menuTemplateId, customQuotas)
        MenuApi->>DB: SELECT * FROM daily_menus WHERE id = :menuTemplateId
        MenuApi->>DB: INSERT INTO shift_menu_assignments (cash_shift_id, menu_id, assigned_by)
        MenuApi->>DB: INSERT INTO shift_menu_items (shift_menu_assignment_id, dish_id, course_type, initial_quota, current_quota, effective_price, version=0)
        DB-->>OrderSvc: shift & shift_menu created
        OrderSvc-->>POSCtrl: ShiftResponseDto (Turno Abierto + Menú Cargado)
        POSCtrl-->>Cajero: 200 OK: Turno Activo (Cupos: Milanesa=25, Sopa=30)
    end

    %% -------------------------------------------------------------
    %% SECCIÓN 2: VENTA EN POS CON DECREMENTO ATÓMICO (ACID)
    %% -------------------------------------------------------------
    rect rgb(245, 255, 245)
        note over Cajero, DB: FASE 2: Venta en POS con Verificación Atómica de Cupos (Optimistic Locking)
        Cajero->>POSCtrl: POST /api/v1/pos/orders<br/>{shiftId, items: [{dishId: Milanesa, qty: 2}], paymentMethod: 'CASH'}
        POSCtrl->>OrderSvc: createAndPayOrder(cmd)
        note over OrderSvc, DB: TX BEGIN (Isolation: READ_COMMITTED)

        OrderSvc->>QuotaRepo: executeConditionalDecrement(dishId, qty=2, expectedVersion)
        QuotaRepo->>DB: UPDATE shift_menu_items<br/>SET current_quota = current_quota - 2,<br/>    sold_count = sold_count + 2,<br/>    is_sold_out = CASE WHEN (current_quota - 2) = 0 THEN TRUE ELSE FALSE END,<br/>    version = version + 1<br/>WHERE shift_assignment_id = :assignmentId<br/>  AND dish_id = :dishId<br/>  AND current_quota >= 2<br/>  AND version = :version

        alt Cupo Suficiente y Versión Coincidente (Rows Updated == 1)
            DB-->>QuotaRepo: 1 row affected
            QuotaRepo-->>OrderSvc: Decremento Exitoso (Nuevo cupo: 23)
            OrderSvc->>DB: INSERT INTO orders (order_status = 'PAID', total_amount = 70.00)
            OrderSvc->>DB: INSERT INTO order_items (...)
            OrderSvc->>EventPub: publishEvent(OrderPaidEvent)
            note over OrderSvc, DB: TX COMMIT (ACID Guaranteed)
            OrderSvc-->>POSCtrl: OrderResponseDto (Ticket #1042 emitido)
            POSCtrl-->>Cajero: 201 Created (Comanda pagada, cupo restante: 23)
        else Cupo Insuficiente o Conflicto Concurrente (Rows Updated == 0)
            DB-->>QuotaRepo: 0 rows affected
            QuotaRepo-->>OrderSvc: Decremento Rechazado
            note over OrderSvc, DB: TX ROLLBACK (Sin alteraciones en BD)
            OrderSvc-->>POSCtrl: throw ItemSoldOutException("Plato agotado o cupo insuficiente")
            POSCtrl-->>Cajero: 409 Conflict (ITEM_SOLD_OUT: "El plato Milanesa no cuenta con cupo suficiente")
            Cajero->>Cajero: UI actualiza botón del plato a "AGOTADO" (disabled)
        end
    end

    %% -------------------------------------------------------------
    %% SECCIÓN 3: AMPLIACIÓN DE CUPO EN VIVO POR SUPERVISOR
    %% -------------------------------------------------------------
    rect rgb(255, 250, 240)
        note over Supervisor, DB: FASE 3: Ampliación de Cupos en Vivo (Live Quota Expansion)
        Supervisor->>POSCtrl: POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota<br/>{quantityDelta: +10, reason: "Nueva tanda horneada en cocina"}
        POSCtrl->>OrderSvc: adjustShiftItemQuota(itemId, delta=+10, reason, supervisorId)
        note over OrderSvc, DB: TX BEGIN
        OrderSvc->>DB: UPDATE shift_menu_items<br/>SET current_quota = current_quota + 10,<br/>    is_sold_out = FALSE,<br/>    version = version + 1<br/>WHERE id = :itemId
        OrderSvc->>AuditRepo: save(new ShiftMenuQuotaAdjustment(itemId, +10, reason, supervisorId))
        AuditRepo->>DB: INSERT INTO shift_menu_quota_adjustments (...)
        note over OrderSvc, DB: TX COMMIT
        OrderSvc-->>POSCtrl: UpdatedQuotaDto (Nuevo cupo: 33)
        POSCtrl-->>Supervisor: 200 OK: Cupo ampliado exitosamente (+10 raciones)
        POSCtrl--)Cajero: SSE/WebSocket Broadcast: Cupo Actualizado (Milanesa: 33 disponible)
    end
```

---

## 🛡️ 3. Invariantes y Reglas ACID Garantizadas

1. **Invariante de Balance de Raciones:**
   $$\text{current\_quota} + \text{sold\_count} = \text{initial\_quota} + \sum \text{quantity\_delta}$$
2. **Atomicidad estricta:**
   Si la orden incluye múltiples platos del menú (ej. 1 Sopa y 1 Milanesa) y uno de ellos falla por cupo (`0 rows affected`), la transacción completa sufre `ROLLBACK`. Ni la sopa ni la milanesa son descontadas, evitando comandas a medio cobrar.
3. **No-Negatividad Inviolable:**
   Restricción de integridad en base de datos:
   ```sql
   CONSTRAINT chk_shift_menu_current_quota CHECK (current_quota >= 0)
   ```
4. **Trazabilidad Inmutable:**
   Toda variación de cupo posterior a la apertura exige registro obligatorio en `shift_menu_quota_adjustments` con el `user_id` del supervisor y motivo textual.
