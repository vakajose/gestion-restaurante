---
title: "Flujo de Venta POS, Validación de Turno, Modalidades, Combos y Kardex"
category: "behavior-flow"
module: "pos"
tags: ["sequence", "pos", "orders", "shift-required", "modifiers", "combos", "kardex", "entitlements", "delivery"]
last_updated: "2026-11-17"
status: "verified"
---

# Flujo de Venta POS: Validación de Turno, Modalidades, Combos y Kardex

Secuencia integral desde el checkout en Angular hasta la persistencia atómica de la orden, verificación de entitlements SaaS, invariante estricto de turno abierto (`409 SHIFT_REQUIRED_ERROR`), descuento de promociones y cupos de menú del día/combos, registro síncrono en caja y deducción asíncrona de inventario en Kardex.

> [!NOTE]
> Este diagrama consolidado unifica la validación de turno abierto (`409`), la gestión de modificadores/selecciones de combos y las modalidades de servicio (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`). Asimismo, reemplaza la referencia obsoleta a `FinanceEventListener` (`@Deprecated`): el registro de venta en caja se ejecuta de forma **síncrona** vía `FinancePublicApi.recordSale(...)`.

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero (POS UI)
    participant PosSvc as PosService (Angular + Dexie)
    participant OrderCtrl as OrderController (/api/v1/pos/orders)
    participant OrderSvc as OrderServiceImpl
    participant EntSvc as TenantEntitlementService
    participant FinApi as FinancePublicApi (CashShiftService)
    participant Promo as PromotionEngine
    participant MenuApi as DailyMenuPublicApi
    participant Outbox as Spring Modulith Outbox
    participant InvListener as InventoryEventListener + KardexService
    participant BoListener as BackofficeTelemetryListener

    Cajero->>PosSvc: Confirmar pago (paymentMethod, orderType, deliveryFee, items, promoId)
    PosSvc->>PosSvc: Persistir en Dexie pendingOrders (syncStatus: PENDING, clientTransactionId)
    PosSvc->>OrderCtrl: POST /api/v1/pos/orders (Header X-Client-Transaction-Id)
    OrderCtrl->>OrderSvc: createOrder(request)

    %% 1. Idempotencia y Entitlements
    OrderSvc->>OrderSvc: Buscar findByTenantIdAndClientTransactionId(tenantId, clientTxId)
    alt Orden ya procesada previamente
        OrderSvc-->>OrderCtrl: CreateOrderResult(existingOrder, isNew: false)
        OrderCtrl-->>PosSvc: HTTP 200 OK (Idempotente)
    else Nueva transacción
        OrderSvc->>EntSvc: assertPosAndShiftOperationsAllowed(tenantId)
        OrderSvc->>EntSvc: assertQuotaAvailable(tenantId, MONTHLY_ORDERS, currentMonth + 1)

        %% 2. Invariante de Turno Abierto
        OrderSvc->>FinApi: findActiveShift(tenantId, branchId)
        alt No existe turno OPEN en la sucursal
            FinApi-->>OrderSvc: Optional.empty() / status != OPEN
            OrderSvc-->>OrderCtrl: throw ShiftRequiredException
            OrderCtrl-->>PosSvc: HTTP 409 Conflict (RFC 7807 code: SHIFT_REQUIRED_ERROR)
            PosSvc-->>Cajero: Bloquear venta y mostrar acceso a Apertura de Caja
        else Turno OPEN verificado
            FinApi-->>OrderSvc: CashShiftDto (id, status: OPEN)

            %% 3. Ítems, Modificadores, Selecciones y Promociones
            OrderSvc->>OrderSvc: Construir OrderItem + OrderItemModifier + OrderItemSelection (combos)
            OrderSvc->>Promo: Evaluar promo manual o AUTOMATIC en zona horaria sucursal (ADR-0006)
            OrderSvc->>OrderSvc: Aplicar discountAmount por ítem y sumar deliveryFee si aplica

            %% 4. Cupos Menú del Día / Jornada
            loop Por cada dishId directo, componentDishIds o selections[].selectedDishId
                OrderSvc->>MenuApi: enforceAndDecrementQuota(branchId, cashShiftId, dishId, qty)
            end

            %% 5. Validación Delivery y Persistencia Atómica
            Note right of OrderSvc: Si orderType == DELIVERY exige deliveryAddress no vacía
            OrderSvc->>OrderSvc: saveAndFlush(Order PAID) con ticket_number secuencial diario

            %% 6. Finanzas Síncrono + Eventos Asíncronos
            OrderSvc->>FinApi: recordSale(tenantId, branchId, cashShiftId, paymentMethod, totalAmount)
            OrderSvc->>Outbox: publishEvent(OrderPaidEvent)
            OrderSvc-->>OrderCtrl: CreateOrderResult(savedOrder, isNew: true)
            OrderCtrl-->>PosSvc: HTTP 201 Created
            PosSvc->>PosSvc: Actualizar pendingOrders -> SYNCED
            PosSvc-->>Cajero: Imprimir / mostrar ticket

            %% 7. Consumidores @ApplicationModuleListener
            par Procesamiento Asíncrono Persistente
                Outbox-)InvListener: OrderPaidEvent (items, componentDishIds, modifiers)
                InvListener->>InvListener: KardexService.processOrderDeduction() según estrategia
            and
                Outbox-)BoListener: OrderPaidEvent
                BoListener->>BoListener: Incrementar tenant_daily_telemetry (orders_count, gross_sales)
            end
        end
    end
```

## Detalles Críticos Verificados

1. **Modalidades de Servicio y Flete (`V10`, `V13`, `ADR-0020`):**
   - `orderType` soporta `DINE_IN` (default), `TAKEOUT`, `DELIVERY` y `SCHEDULED_PICKUP`.
   - En modalidad `DELIVERY`, el backend exige `deliveryAddress` obligatorio y suma `deliveryFee` (`>= 0`) al `totalAmount` final después de aplicar descuentos de promociones.
2. **Modificadores y Combos Dinámicos (`V4`, `V23`, `ADR-0018`, `ADR-0031`):**
   - Cada `OrderItem` puede referenciar un plato individual (`dishId`), un combo (`comboId`) y/o un menú diario (`dailyMenuId`), con lista de `selectedModifiers` (`OrderItemModifier`, que opcionalmente descuentan un ingrediente físico) y `selections` (`OrderItemSelection`, persistiendo el plato real elegido por slot `MENU_COURSE` o `SPECIFIC_OPTIONS`).
   - El decremento atómico de cupos (`DailyMenuPublicApi.enforceAndDecrementQuota`) suma cantidades tanto de `dishId` como de cada `selectedDishId` en `selections` (o `componentDishIds`).
3. **Invariante de Turno y Contrato RFC 7807 (`ADR-0013`):**
   - Si la caja de la sucursal no tiene un turno `OPEN`, `ShiftRequiredException` responde `409 Conflict` con `errorCode: "SHIFT_REQUIRED_ERROR"` y `type: "https://api.restaurant.com/errors/shift-required"`.

## Enlaces Relacionados

- [Sincronización Offline Dexie](./sequence_pos_offline_sync.md)
- [Evaluación de Promociones POS](./sequence_pos_promotions_evaluation.md)
- [Cancelación de Pedidos y Autorizaciones](./sequence_pos_order_cancellation_and_authorizations.md)
- [Ciclo de Vida de la Orden](./state_order_lifecycle.md)
- [Estrategia de Deducción Kardex](./activity_inventory_deduction_strategy.md)
- [Cuotas de Menú Diario y Jornadas](./sequence_daily_menu_shift_assignment_and_quota_enforcement.md)
- [ADR-0013: Invariante de Turno POS y Autorizaciones](../../../adr/0013_pos_shift_invariant_and_expense_authorizations.md)
- [ADR-0018: Modificadores, Extras y Notas de Cocina](../../../adr/0018_pos_modifiers_extras_and_kitchen_notes_suite.md)
- [ADR-0020: Modalidades de Servicio y Delivery](../../../adr/0020_service_modalities_takeout_delivery_and_ticket_badges.md)
- [ADR-0031: Jornadas de Servicio y Combos Dinámicos](../../../adr/0031_branch_service_sessions_and_dynamic_combos.md)
