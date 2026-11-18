---
title: "Secuencia de Sincronización Offline-First POS (Dexie.js)"
category: "behavior-flow"
module: "pos"
tags: ["sequence", "offline-first", "dexie", "indexeddb", "idempotency", "pos", "sync"]
last_updated: "2026-11-17"
status: "verified"
---

# Secuencia de Sincronización Offline-First POS (Dexie.js)

Protocolo de resiliencia ante desconexión de red en sucursales usando **Dexie.js v4** (`RestaurantPosDb` `version(2)`) encapsulado tras la capa DAL (`LocalOrderRepository`), garantizando idempotencia estricta mediante `clientTransactionId` y el endpoint batch `POST /api/v1/pos/orders/sync`.

> [!NOTE]
> El almacenamiento local `version(2)` gestiona exclusivamente los stores `cachedCategories`, `cachedProducts`, `pendingOrders` (indexado por `tenantId`, `branchId`, `syncStatus`, `clientTransactionId`, `createdAt`) y `activeShift`. No existen stores separados `cachedShiftMenu` ni `cachedShiftMenuItems`.

## Diagrama de Secuencia Offline y Reconexión (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero
    participant PosUI as PosComponent (Angular)
    participant PosSvc as PosService
    participant DalRepo as LocalOrderRepository (Dexie v2)
    participant Ctrl as OrderController
    participant OrderSvc as OrderServiceImpl
    participant DB as PostgreSQL (orders.client_transaction_id UK)

    %% Fase 1: Emisión Local Garantizada
    Cajero->>PosUI: Ejecutar cobro (Efectivo/Tarjeta/QR)
    PosUI->>PosSvc: checkout(paymentMethod, orderType, ...)
    PosSvc->>PosSvc: Generar UUID v4 (clientTransactionId) y CreateOrderRequest
    PosSvc->>DalRepo: addPendingOrder(tenantId, branchId, syncStatus: PENDING, orderData)
    DalRepo-->>PosSvc: localId persistido en IndexedDB
    PosSvc->>PosSvc: refreshPendingCount() y clearCart()

    alt Con conexión (navigator.onLine == true)
        PosSvc->>Ctrl: POST /api/v1/pos/orders (Header X-Client-Transaction-Id)
        alt Backend confirma (201 Created o 200 OK)
            Ctrl-->>PosSvc: OrderDto (ticket_number oficial)
            PosSvc->>DalRepo: updateSyncStatus(localId, SYNCED)
            PosSvc-->>PosUI: Retornar comprobante oficial
        else Falla de red / timeout durante envío
            Ctrl--xPosSvc: Error de transporte
            PosSvc-->>PosUI: Retornar OrderDto local (ticket offline temporal, mantiene PENDING)
        end
    else Sin conexión (navigator.onLine == false)
        PosSvc-->>PosUI: Emitir ticket local offline (pendingOrders queda PENDING)
    end

    %% Fase 2: Reconexión y Sincronización Batch Idempotente
    Note over PosSvc: Evento window online o clic manual en Sync Pendientes
    PosUI->>PosSvc: syncPendingOrders()
    PosSvc->>DalRepo: getPendingOrders(currentTenantId) (syncStatus == PENDING)
    DalRepo-->>PosSvc: Lista PendingOrderEntity[]

    PosSvc->>Ctrl: POST /api/v1/pos/orders/sync (SyncOrdersRequest)
    Ctrl->>OrderSvc: syncOrders(request)

    loop Por cada CreateOrderRequest en request.orders
        OrderSvc->>DB: findByTenantIdAndClientTransactionId(tenantId, clientTxId)
        alt Ya existe en PostgreSQL
            DB-->>OrderSvc: Order existente
            OrderSvc-->>Ctrl: Retornar OrderDto sin duplicar venta ni Kardex
        else No existe aún
            OrderSvc->>OrderSvc: createOrder(req) (valida turno, promos, cupos, guarda Order + recordSale + OrderPaidEvent)
            OrderSvc-->>Ctrl: OrderDto recién creado
        end
    end

    Ctrl-->>PosSvc: SyncOrdersResponse (syncedOrders, totalReceived, totalProcessed)
    loop Por cada PendingOrderEntity enviado
        PosSvc->>DalRepo: updateSyncStatus(localId, SYNCED)
    end
    PosSvc->>PosSvc: refreshPendingCount() -> 0 pendientes
```

## Garantías de Idempotencia y Multi-Tenancy

1. **Persistencia Local Previa:** `PosService.checkout()` registra el pedido en `appDb.pendingOrders` **antes** de ejecutar cualquier petición HTTP, evitando pérdida de ventas ante cortes de energía o caídas de navegador.
2. **Aislamiento por `tenantId` (`version(2)`):** Durante la migración automática de Dexie a `version(2)`, cualquier registro local huérfano sin `tenantId` se purga para impedir filtraciones al cambiar de empresa en un mismo dispositivo compartido.
3. **Deduplicación en Servidor:** La restricción única `client_transaction_id` en `orders` y la búsqueda previa `findByTenantIdAndClientTransactionId(...)` hacen que reintentos individuales o por lote sean completamente idempotentes.

## Enlaces Relacionados

- [Skill: Estrategia Offline Dexie y Sincronización](../../../../.agents/skills/offline-dexie-sync/SKILL.md)
- [Esquema Local Dexie.js](../02-domain-and-data/dexie_local_schema.md)
- [Flujo Principal de Venta POS](./sequence_pos_order_deduction.md)
- [Ciclo de Vida de la Orden](./state_order_lifecycle.md)
- [ADR-0003: Estrategia Offline-First y Sincronización Dexie](../../../adr/0003_offline_first_dexie_sync.md)
