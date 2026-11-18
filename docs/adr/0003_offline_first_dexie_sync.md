---
title: "ADR 0003: Estrategia Offline-First en POS con Dexie.js e Idempotencia"
category: "adr"
tags:
  - adr
  - architecture
  - frontend
  - pos
  - offline-first
  - dexie
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0003: Estrategia Offline-First en POS con Dexie.js e Idempotencia


## Contexto
En un punto de venta gastronómico (POS), la atención al cliente no puede detenerse bajo ninguna circunstancia por fluctuaciones en la conexión a internet o fallas momentáneas de red local. Los cajeros deben poder cobrar y emitir comandas de forma ininterrumpida. Al mismo tiempo, la reconexión automática posterior no debe duplicar ventas ni descuadrar los cobros financieros.

## Decisión
Se implementa una arquitectura **Offline-First** en el cliente web mediante **Angular 22** y **Dexie.js** (IndexedDB).

### Protocolo de Operación:
1. El catálogo de productos y precios se cachea localmente en IndexedDB.
2. Toda orden creada sin conexión se persiste de inmediato en la tabla local `pendingOrders` con un UUID único generado por el cliente (`client_transaction_id`).
3. El servicio `SyncManagerService` detecta el retorno de la conexión y sincroniza los pedidos mediante peticiones HTTP idempotentes enviando la cabecera `X-Client-Transaction-Id`.
4. El backend impone una restricción única `UNIQUE(client_transaction_id)` en la tabla `orders`. Si la orden ya había ingresado por un reintento anterior, responde `409 Conflict` o `200 OK` confirmado, y el cliente actualiza el estado a `SYNCED` sin generar cobros dobles.
5. Ante déficit de stock en sincronizaciones offline con modo estricto, la venta financiera se registra de forma prioritaria (ya ocurrió físicamente) y se levanta una alerta `STOCK_DEFICIT_WARNING` en auditoría para conciliación administrativa.

## Consecuencias
- **Positivas:** Continuidad operativa total en el restaurante; cero pérdida de ventas; prevención absoluta de duplicidades.
- **Negativas:** La interfaz debe gestionar estados visuales de sincronización y almacenamiento local en el navegador del terminal POS.

## Estado Real de Caché Offline (`appDb` — Corte Sprint 6)
> [!IMPORTANT]
> Actualmente, la base IndexedDB gestionada por Dexie.js (`appDb`) persiste exactamente cuatro tablas reales: **`cachedCategories`**, **`cachedProducts`**, **`pendingOrders`** y **`activeShift`**.
>
> Los **combos armables (`Combo`)**, la sesión de servicio con **menús diarios y cupos (`BranchServiceSession` / `shift_menu_items`)** y las **promociones vigentes (`Promotion`)** operan hoy contra red; su persistencia offline en `appDb` permanece como alcance pendiente planificado (`TASK-640`). La sincronización de `pendingOrders` opera sobre `POST /api/v1/pos/orders/sync` (y `POST /api/v1/pos/orders` con cabecera `X-Client-Transaction-Id`).

---

## Documentos Relacionados
- [ADR-0010: Control de Acceso RBAC Dinámico y Reseteo Delegado](./0010_granular_rbac_roles_and_privileges.md)
- [ADR-0011: Ciclo de Vida de Sesiones y Renovación Silenciosa](./0011_session_lifecycle_and_silent_refresh.md)
- [ADR-0013: Invariante Transaccional de Turno en POS](./0013_pos_shift_invariant_and_expense_authorizations.md)
- [ADR-0018: Modificadores, Extras y Deducción Kardex](./0018_pos_modifiers_extras_and_kitchen_notes_suite.md)
- [ADR-0020: Modalidades de Servicio y Delivery](./0020_service_modalities_takeout_delivery_and_ticket_badges.md)
- [ADR-0021: Catálogo Visual y Fallback de Imágenes Offline](./0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)
- [ADR-0031: Sesiones de Servicio por Sucursal y Combos Dinámicos](./0031_branch_service_sessions_and_dynamic_combos.md)
- [ADR-0032: Motor de Promociones en POS](./0032_pos_promotions_engine.md)
- [Esquema Local IndexedDB (Dexie.js)](../project/architecture/02-domain-and-data/dexie_local_schema.md)
- [Diagrama de Secuencia Offline-First y Reconciliación](../project/architecture/04-behavior-and-flows/sequence_pos_offline_sync.md)
- [Casos de Uso de POS](../project/architecture/03-use-cases/use_cases_pos.md)

