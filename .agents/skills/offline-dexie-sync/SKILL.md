---
name: offline-dexie-sync
description: >-
  Estrategia offline-first para Angular 22 Zoneless con Dexie.js e idempotencia.
  Usar al implementar componentes POS, servicios de sincronización o almacenamiento local IndexedDB.
---

# Patrones Offline-First con Dexie.js (ADR-0003)

## 1. Regla de Oro en POS
La atención y cobro en mostrador **nunca se bloquean por falta de red**.
- Toda orden cobrada offline se almacena primero en Dexie.js con un UUID `clientTransactionId`.
- El ticket se emite de inmediato con advertencia visual no invasiva de guardado local.

## 2. Protocolo de Sincronización Idempotente
- Cabecera HTTP obligatoria en la petición de sincronización:
  `X-Client-Transaction-Id: <uuid>`
- Si la conexión falla, se programa reintento con backoff exponencial.
- Si el backend responde `200/201` o `409 Conflict`, la orden local pasa a estado `SYNCED`.
- En caso de déficit de stock con política `STRICT`, el backend registra la venta y alerta en auditoría; nunca rechaza una venta ya cobrada físicamente.

## 3. Estado Reactivo en Angular 22
- Usar Angular Signals (`signal<boolean>`, `computed()`) para exponer el estado de red (`isOnline`) y la cantidad de órdenes pendientes de sincronización (`pendingOrdersCount`).
- Configurar detección Zoneless en `app.config.ts` (`provideExperimentalZonelessChangeDetection()`).
