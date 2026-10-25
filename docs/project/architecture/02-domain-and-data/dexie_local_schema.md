---
title: "Esquema de Almacenamiento Local IndexedDB (Dexie.js)"
category: "architecture"
level: "Local-Storage"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - dexie
  - indexeddb
  - offline-first
  - pos
last_updated: "2026-10-25"
status: "verified"
---

# 💾 Esquema de Almacenamiento Local IndexedDB (Dexie.js)

Este documento especifica el **Esquema de Base de Datos Local en el Cliente Web** gestionado a través de **Dexie.js v4** sobre la API nativa de **IndexedDB** del navegador. Ha sido extraído y verificado directamente contra la implementación en el frontend:
- [`frontend/src/app/core/db/app.database.ts`](../../../../frontend/src/app/core/db/app.database.ts)
- [`frontend/src/app/core/models/pos.models.ts`](../../../../frontend/src/app/core/models/pos.models.ts)
- [`frontend/src/app/core/services/pos.service.ts`](../../../../frontend/src/app/core/services/pos.service.ts)

---

## 1. Contexto Arquitectónico y Filosofía Offline-First (ADR-0003)

En un entorno gastronómico de alta rotación (Fast Food / Restobar), **la atención al cliente en mostrador no puede bloquearse jamás** por caídas de internet, latencia de Wi-Fi o indisponibilidad momentánea del backend.

Para materializar este requerimiento de misión crítica:
1. **Catálogo Autónomo en el Navegador:** Las categorías y platos con precios resueltos se cachean en Dexie.js (`cachedCategories`, `cachedProducts`).
2. **Checkout Ininterrumpido:** Toda orden cobrada offline se persiste en la tabla local `pendingOrders` antes de emitir el ticket local.
3. **Idempotencia Garantizada:** Cada orden local genera un UUID universal (`clientTransactionId`), que el backend valida bajo una restricción `UNIQUE` en PostgreSQL, impidiendo cobros dobles al reconectar.
4. **Gobernanza DAL (Regla 2):** Los componentes de UI y servicios de aplicación no acceden directamente a `appDb`, sino a través de una capa de Data Access Layer (DAL) / Repositorios Locales.

---

## 2. Diagrama de Clases del Esquema Dexie.js

```mermaid
classDiagram
    %% Clase Principal de la Base de Datos Local
    class RestaurantPosDb {
        +Table~CachedCategory, string~ cachedCategories
        +Table~CachedProduct, string~ cachedProducts
        +Table~PendingOrderEntity, number~ pendingOrders
        +Table~ActiveShiftEntity, string~ activeShift
        +constructor(options)
    }

    %% Tablas Locales Tipadas
    class CachedCategory {
        +string id
        +string name
        +string icon
        +number sortOrder
    }

    class CachedProduct {
        +string id
        +string categoryId
        +string code
        +string name
        +string description
        +number salePrice
        +boolean isAvailable
        +string categoryName
    }

    class PendingOrderEntity {
        +number localId
        +string clientTransactionId
        +SyncStatus syncStatus
        +string createdAt
        +CreateOrderRequest orderData
        +string ticketNumber
        +number totalAmount
        +string errorMessage
    }

    class ActiveShiftEntity {
        +string id
        +string branchId
        +string openedAt
        +string status
        +string openedBy
    }

    %% Interfaces de Petición y DTOs de POS
    class CreateOrderRequest {
        +string clientTransactionId
        +string branchId
        +string cashShiftId
        +PaymentMethod paymentMethod
        +string notes
        +CreateOrderItemRequest[] items
    }

    class CreateOrderItemRequest {
        +string dishId
        +number quantity
        +number unitPrice
        +string notes
    }

    class CartItem {
        +string dishId
        +string code
        +string name
        +number unitPrice
        +number quantity
        +number subtotal
        +string notes
    }

    class OrderDto {
        +string id
        +string branchId
        +string cashShiftId
        +string ticketNumber
        +string orderStatus
        +PaymentMethod paymentMethod
        +number totalAmount
        +string clientTransactionId
        +string notes
        +string createdAt
        +string closedAt
        +OrderItemDto[] items
    }

    class OrderItemDto {
        +string id
        +string dishId
        +string dishName
        +number quantity
        +number unitPrice
        +number subtotal
        +string notes
    }

    %% Enums Locales
    class SyncStatus {
        <<enumeration>>
        PENDING
        SYNCED
        FAILED
    }

    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        QR
        TRANSFER
    }

    %% Relaciones Estructurales
    RestaurantPosDb *-- CachedCategory : "manages table cachedCategories"
    RestaurantPosDb *-- CachedProduct : "manages table cachedProducts"
    RestaurantPosDb *-- PendingOrderEntity : "manages table pendingOrders"
    RestaurantPosDb *-- ActiveShiftEntity : "manages table activeShift"

    PendingOrderEntity --> CreateOrderRequest : "encapsulates payload"
    CreateOrderRequest *-- CreateOrderItemRequest : "contains items"
    PendingOrderEntity ..> SyncStatus : "tracked by"
    CreateOrderRequest ..> PaymentMethod : "paid via"
    OrderDto *-- OrderItemDto : "contains items"
    OrderDto ..> PaymentMethod : "recorded with"
```

---

## 3. Especificación Técnica de Tiendas (Stores) e Índices

La versión 1 de la base de datos `RestaurantPosDb` define sus almacenes de objetos e índices en Dexie.js de la siguiente forma:

```typescript
this.version(1).stores({
  cachedCategories: 'id, name, sortOrder',
  cachedProducts:   'id, categoryId, code, name, salePrice, isAvailable',
  pendingOrders:    '++localId, clientTransactionId, syncStatus, createdAt, orderData',
  activeShift:      'id, branchId, openedAt, status',
});
```

### Detalle de Índices por Tabla

| Tienda (Table) | Clave Primaria | Índices Secundarios | Propósito Operativo |
| :--- | :--- | :--- | :--- |
| `cachedCategories` | `id` (string UUID) | `name`, `sortOrder` | Ordenar las pestañas de categorías en la botonera táctil del POS. |
| `cachedProducts` | `id` (string UUID) | `categoryId`, `code`, `name`, `salePrice`, `isAvailable` | Filtrar platos por categoría seleccionada, buscar por código de barra/teclado y verificar disponibilidad. |
| `pendingOrders` | `++localId` (auto-incremental) | `clientTransactionId`, `syncStatus`, `createdAt`, `orderData` | Bitácora de pedidos creados localmente. Permite recuperar órdenes `PENDING` para la sincronización por lotes. |
| `activeShift` | `id` (string UUID) | `branchId`, `openedAt`, `status` | Recordar el turno de caja abierto en la sucursal actual para vincular las órdenes offline. |

---

## 4. Ciclo de Vida del Almacenamiento Offline-First

El siguiente diagrama de flujo resume la interacción del almacenamiento local durante el ciclo de vida de una venta:

```mermaid
graph TD
    Start["🛒 Venta en Mostrador (POS)"] --> AddCart["Añadir ítems al Cart reactivo (Signals)"]
    AddCart --> Checkout["Pulsar Cobrar (Checkout Modal)"]

    Checkout --> GenTx["Generar clientTransactionId (UUID v4)<br/>y Ticket Local: LOCAL-XXXX"]
    GenTx --> PersistLocal["💾 Persistir en Dexie.js pendingOrders<br/>syncStatus: 'PENDING'"]
    PersistLocal --> ClearCart["Limpiar Carrito en Memoria<br/>Emitir Ticket Impreso / Comprobante"]

    ClearCart --> CheckNet{"¿Terminal en Línea? (navigator.onLine)"}

    CheckNet -- SÍ --> SendServer["POST /api/v1/pos/orders<br/>Header: X-Client-Transaction-Id: clientTxId"]
    CheckNet -- NO --> KeepOffline["Permanecer en Dexie como 'PENDING'<br/>Incrementar pendingCount signal"]

    SendServer --> ServerResp{"Respuesta Backend"}
    ServerResp -- "201 Created / 200 OK" --> MarkSynced["Actualizar Dexie pendingOrders<br/>syncStatus: 'SYNCED'"]
    ServerResp -- "409 Conflict (Idempotente)" --> MarkSynced
    ServerResp -- "Fallo de Red / Timeout" --> KeepOffline

    KeepOffline --> NetReturn["Evento 'online' / Reintento Manual"]
    NetReturn --> BatchSync["POST /api/v1/pos/orders/sync (Batch)<br/>Reconciliación de pendientes"]
    BatchSync --> MarkSynced
```

---

## 5. Protocolo de Sincronización y Reconciliación Idempotente

1. **Reintento Automático ante Retorno de Conexión:**
   - La aplicación escucha `window.addEventListener('online')` y dispara `syncPendingOrders()` en segundo plano.
2. **Idempotencia Transaccional:**
   - Cada llamada HTTP envía la cabecera `X-Client-Transaction-Id: <uuid>`.
   - Si una petición anterior ya fue recibida por el backend pero la respuesta se perdió por corte de conexión, el backend devuelve el registro existente sin reinsertar ni cobrar doblemente.
3. **Manejo de Variaciones de Stock (Strategy Mode):**
   - Si la política de inventario configurada es `AUTOMATIC_STRICT` y no hay existencias físicas en el backend para una orden vendida offline, el backend prioriza la validez financiera de la venta (el dinero físico ya fue recibido por el cajero) y emite una advertencia de déficit de stock para auditoría administrativa.

---

## 6. Referencias y Artefactos Relacionados

- **Modelo de Dominio Conceptual:** Ver [domain_model.md](./domain_model.md).
- **Diagrama Entidad-Relación PostgreSQL:** Ver [database_erd.md](./database_erd.md).
- **Decisión Arquitectónica Offline-First:** Ver [ADR-0003](../../../adr/0003_offline_first_dexie_sync.md).
- **Secuencia Detallada de Sincronización:** Ver [sequence_pos_offline_sync.md](../04-behavior-and-flows/sequence_pos_offline_sync.md).
- **Índice de la Bóveda de Arquitectura:** Ver [README.md](../README.md).
