---
name: offline-dexie-sync
description: >-
  Estrategia offline-first para Angular 22 Zoneless con Dexie.js e idempotencia.
  Usar al implementar componentes POS, servicios de sincronización o almacenamiento local IndexedDB.
---

# Patrones Offline-First con Dexie.js (ADR-0003)

## 1. Regla de Oro en POS y Aislamiento DAL
La atención y cobro en mostrador **nunca se bloquean por falta de red**.
- La base local en `frontend/src/app/core/db/app.database.ts` es `RestaurantPosDb` (instancia `appDb`, versión 2) con 4 stores: `cachedCategories`, `cachedProducts`, `pendingOrders` y `activeShift`.
- Todo acceso a Dexie debe aislarse mediante el Data Access Layer (`frontend/src/app/core/repositories/`: `LocalOrderRepository`, `LocalCatalogRepository`, `LocalShiftRepository` vía token `APP_DATABASE`), nunca importando `appDb` desde componentes o servicios de negocio.
- Toda orden cobrada offline se guarda en `pendingOrders` con `clientTransactionId` (UUID v4), `tenantId`, `branchId` y `syncStatus: 'PENDING'`.

## 2. Protocolo de Sincronización Idempotente
- Envío individual en línea: `POST /api/v1/pos/orders` con cabecera `X-Client-Transaction-Id: <uuid>`.
- Reconciliación por lotes al recuperar red: `PosService` lee órdenes en estado `'PENDING'` de `LocalOrderRepository` y ejecuta `POST /api/v1/pos/orders/sync` (`SyncOrdersRequest`).
- Estados reales de sincronización (`SyncStatus`): `'PENDING' | 'SYNCED' | 'FAILED'`.
- Si el backend responde `200 OK`, `201 Created` o `409 Conflict` (orden ya procesada por idempotencia), la orden local pasa a estado `'SYNCED'`.

## 3. Estado Reactivo en Angular 22 Zoneless
- Usar Angular Signals (`signal()`, `computed()`) para exponer conectividad (`isOnline`) y cola local (`pendingCount`).
- Configurar detección Zoneless en `frontend/src/app/app.config.ts` mediante `provideZonelessChangeDetection()`.
