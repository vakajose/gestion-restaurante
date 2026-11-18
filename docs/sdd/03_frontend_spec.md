---
title: "03. Especificación Técnica de Frontend y Modo Offline"
category: "sdd"
tags:
  - sdd
  - frontend
  - angular22
  - zoneless
  - dexie
  - signals
last_updated: "2026-11-17"
status: "verified"
---

# 03. Especificación Técnica de Frontend ERP y Modo Offline (`frontend/`)

> [!NOTE]
> Para diagramas C4 y el modelo local IndexedDB consulta [c4_level3_frontend_angular.md](../project/architecture/01-c4-system/c4_level3_frontend_angular.md), [dexie_local_schema.md](../project/architecture/02-domain-and-data/dexie_local_schema.md) y [sequence_pos_offline_sync.md](../project/architecture/04-behavior-and-flows/sequence_pos_offline_sync.md). Para el sistema visual consulta [05_design_system.md](05_design_system.md).

## 1. Arquitectura Angular 22 y Rutas Reales

- **Framework y Detección de Cambios:** Angular 22 Standalone sin `NgModule`, configurado con `provideZonelessChangeDetection()` en `frontend/src/app/app.config.ts` (exportando alias `provideExperimentalZonelessChangeDetection`).
- **Estado y Formularios:** Angular Signals (`signal()`, `computed()`, `effect()`, `input()`, `output()`, `model()`) en servicios singleton (`providedIn: 'root'`) y componentes; formularios construidos con **`ReactiveFormsModule`** (`auth`, `onboarding`, `company`, `users`, `roles`, `profile`) y **`FormsModule`** (`pos`, `catalog`, `daily-menus`, `inventory`, `cash-control`, `finance`, `parameters`) integrados con Signals.
- **Rutas Activas (`frontend/src/app/app.routes.ts`):**
  - Públicas (`guestGuard`): `/auth/login` (redirección desde `/auth`), `/onboarding`.
  - Protegidas (`authGuard` + `permissionGuard` RBAC):
    - `/pos` (`pos:access`): Terminal POS, modalidades, combos `MENU_COURSE`, modificadores y cola offline.
    - `/cash-control` (`finance:shifts`): Turno vivo del cajero, gastos de caja chica, cancelaciones y cola de autorizaciones.
    - `/catalog` (`catalog:read`): Categorías (emoji/prefijo), platos, fotos WebP 4:3, recetas, modificadores, combos e insumos.
    - `/catalog/menus` (`catalog:read`): Plantillas de menú diario y jornadas de servicio (`BranchServiceSession`).
    - `/inventory` (`inventory:read`): Existencias por sucursal, stock inicial, ajustes y Kardex.
    - `/finance` (`finance:reports` | `finance:shifts_supervisor` | `finance:expenses_approve` | `reports:view`): Arqueos, histórico multicriterio, drill-down y exportador CSV (`csv-exporter.ts`).
    - `/company` (`company:read`): Datos de empresa y sucursales.
    - `/users` (`users:read`), `/users/roles` y `/settings/roles` (`users:read`): Personal, reseteo de contraseña, asignación multi-sucursal y roles dinámicos.
    - `/settings/parameters` (`settings:read`): Consola humanizada de 20 parámetros con ayuda contextual.
    - `/profile` (`authGuard`): Perfil autoservicio, contraseña y PIN de supervisor.
  - *Honestidad de alcance:* Las rutas `/kds` (Kitchen Display System, `PB-010`) y `/analytics` (`PB-024`) no existen implementadas actualmente y permanecen en backlog futuro.

---

## 2. Configuración Runtime Twelve-Factor e Interceptores HTTP

`app.config.ts` registra el pipeline funcional en orden estricto:
1. **`apiBaseUrlInterceptor` (`core/interceptors/api-base-url.interceptor.ts` — [ADR-0033](../adr/0033_spa_runtime_env_and_cross_origin_media.md)):** Reescribe peticiones relativas `/api/...` usando `getRuntimeApiBaseUrl()` desde `core/config/runtime-env.ts` (`window.__ENV__.API_BASE_URL` inyectado al arranque en `public/env.js` para Docker/Nginx). Incluye `resolveMediaUrl()` para resolver `/uploads/dishes/...` en orígenes separados (`app.localhost` $\rightarrow$ `api.localhost`).
2. **`authInterceptor` (`core/interceptors/auth.interceptor.ts`):** Adjunta `Authorization: Bearer <token>` y `X-Branch-Id` (`BranchContextService.activeBranchId()`), manejando *Silent Refresh* reactivo con cola RxJS ante HTTP `401` (`ADR-0011`).
3. **`errorInterceptor` (`core/interceptors/error.interceptor.ts`):** Parsea respuestas `ProblemDetail` RFC 7807 y dispara notificaciones en `NotificationService` (`<app-toast-portal>` en `z-[9999]`).

---

## 3. Estrategia Offline-First y DAL Dexie.js (`RestaurantPosDb`)

Todo acceso a IndexedDB está aislado en el Data Access Layer (`frontend/src/app/core/repositories/`) mediante el token `APP_DATABASE`, prohibiendo importar `appDb` en componentes o servicios:

- **Base de Datos (`frontend/src/app/core/db/app.database.ts`):** Clase **`RestaurantPosDb`** (instancia exportada `appDb`, versión 2 con purga automática de órdenes huérfanas sin `tenantId`).
- **4 Object Stores reales:**
  1. `cachedCategories`: `'id, name, sortOrder'`
  2. `cachedProducts`: `'id, categoryId, code, name, salePrice, isAvailable'` (incluye `imageUrl`, `modifierGroups`, `quota`, `isSoldOut`, `isUnlimited`, `allowIndividualSale`, `courseType`)
  3. `pendingOrders`: `'++localId, clientTransactionId, tenantId, branchId, syncStatus, createdAt, orderData'`
  4. `activeShift`: `'id, branchId, openedAt, status'`
  *(Nota: la información de cupos/menú de jornada y promociones opera en memoria contra el servidor; su caché local en `appDb` forma parte del alcance pendiente en `TASK-640` / `PB-063`).*
- **Repositorios DAL:** `LocalCatalogRepository`, `LocalOrderRepository` y `LocalShiftRepository`.
- **Estados y Protocolo de Sincronización (`PosService` & `pos.models.ts`):**
  - `SyncStatus = 'PENDING' | 'SYNCED' | 'FAILED'`.
  - Toda venta en POS genera `clientTransactionId` (UUID v4) y se guarda primero en `pendingOrders` como `'PENDING'`.
  - Si hay conexión, envía `POST /api/v1/pos/orders` con cabecera `X-Client-Transaction-Id`; al recuperar conexión (`online`), `PosService` lee las órdenes `'PENDING'` de `LocalOrderRepository` y ejecuta reconciliación por lotes en `POST /api/v1/pos/orders/sync` (`SyncOrdersRequest`). Ante `200`/`201`/`409` pasa a `'SYNCED'`.

---

## 4. Ergonomía Móvil, Seguridad de Sesiones y Capacidades UI

- **TailwindAdmin v3.0 & UI Kit:** 13 componentes standalone en `shared/components/ui/` + `BottomSheetComponent` (`app-bottom-sheet`) para comanda móvil y tarjetas de inventario (`ADR-0012`, [05_design_system.md](05_design_system.md)).
- **Modo Offline Silencioso:** En estado normal online sin pendientes no ocupa espacio vertical; si `!isOnline()` o `pendingCount() > 0`, muestra la barra ámbar con el contador local.
- **Vistas POS y Catálogo Visual:** Selector triple (`'grid'` | `'compact'` | `'list'`) inicializado desde `pos.display_dish_images` con persistencia en `localStorage`, recorte WebP 4:3 (`<app-image-cropper-modal>`), `<app-emoji-picker>`, asistente de SKU secuencial y selección dinámica de bloques `MENU_COURSE` en combos y menús diarios.
- **Módulo de Inactividad (`IdleTimerService`):** Monitorea actividad (`mousemove`, `keydown`, `click`, `scroll`, `touchstart`) y dispara `InactivityWarningModalComponent` con cuenta regresiva de `120` segundos.
- **Honestidad de Alcance (Pendientes en Backlog `TASK-640` / `PB-063` y Post-MVP):**
  - `IdleTimerService` opera actualmente con `DEFAULT_IDLE_TIMEOUT_MINUTES = 30` (el método `setTimeoutMinutes()` existe, pero aún falta conectarlo dinámicamente al parámetro `security.session_inactivity_timeout_minutes`, `TASK-640`).
  - `PosComponent` expone las modalidades (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`) y `deliveryFee`, pero aún falta cablear condicionalmente los parámetros `pos.allow_scheduled_pickup`, `pos.delivery_fee_enabled` y `pos.delivery_fee_default` (`TASK-640`).
  - No existe interfaz de recepción de compras (`purchases` / `purchase_items`) ni vistas de KDS o analítica financiera avanzada (`PB-021` / `TASK-636`, `PB-010`, `PB-024`).
