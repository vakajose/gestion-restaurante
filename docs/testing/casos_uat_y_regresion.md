---
title: "Detalle de Casos UAT y Regresión Funcional"
category: "testing"
tags:
  - testing
  - uat
  - test-cases
  - regression
last_updated: "2026-11-13"
status: "active"
---

# 📋 Detalle de Casos UAT y Regresión Funcional

> ⬅️ **Navegación:** [Matriz Maestra (`inventario_de_pruebas.md`)](./inventario_de_pruebas.md) | [Bóveda de Pruebas](./README.md) | [Observaciones y Hallazgos](../observaciones_y_hallazgos.md)

Este documento concentra el detalle paso a paso de los flujos críticos ya catalogados y la **bitácora viva de escenarios UAT** validados manualmente en `testing-local` para su posterior automatización.

---

## 1. Flujos Críticos ERP — Jornada, POS, Catálogo, Finanzas e Inventario

### Caso TC-FLOW-001..005: Ciclo de Vida de Turno y Operación Integral
- **Objetivo:** Asegurar que el ciclo comercial y financiero diario funcione de inicio a fin entre diferentes roles de usuarios sin errores ni discrepancias de gaveta.
- **Precondiciones:** Sucursal activa; usuarios `CASHIER` y `ADMIN_TENANT`.
- **Pasos de Ejecución:**
  1. *Cajero:* Ingresa al sistema. Si no hay turno abierto, abre turno con Bs. 200.00.
  2. *Cajero:* Ingresa a `/pos`. Agrega al carrito productos con modificadores/extras y procesa cobro exacto en efectivo. Verifica emisión de comprobante `T-XXXX`.
  3. *Cajero:* Registra solicitud de egreso de caja menor con marca de gaveta (`paidFromCashDrawer = true`). Verifica que queda en `PENDING_APPROVAL` y que el cajero **no** tiene botón para autorizar su propio gasto.
  4. *Admin:* Ingresa como Administrador. Va a Finanzas y aprueba los gastos en estado pendiente.
  5. *Admin:* Abre el modal de Arqueo y Cierre. Compara el Efectivo Teórico Esperado ($E_{esperado} = F_{inicial} + V_{efectivo} - G_{aprobados}$) e ingresa el conteo físico real exacto. Confirma cierre de turno.
  6. *Verificación Final:* El sistema marca el turno como `CLOSED` y la interfaz queda en estado inicial lista para una nueva jornada (`btn-open-shift-cta`).
- **Automatizado en:** `frontend/e2e/shift-lifecycle.spec.ts`

### TC-POS-006: Invariante Transaccional de Turno en Backend (HTTP 409 RFC 7807)
- **Módulo:** Ventas / POS (`modules.pos`) | **Tarea:** TASK-402 / PB-041 / ADR-0013
- **Precondiciones:** Sucursal sin turno activo de caja (`cash_shifts` abierto).
- **Pasos:** Enviar `POST /api/v1/pos/orders` autenticado con payload válido.
- **Resultado Esperado:** HTTP `409 Conflict` RFC 7807 (`SHIFT_REQUIRED_ERROR`); cero órdenes persistidas.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/pos/internal/PosIntegrationTest.java` (`shouldReturn409WhenNoActiveShiftIsOpen`)

### TC-FIN-005: Control de Apertura de Turno con Política SUPERVISOR_ONLY
- **Módulo:** Finanzas (`modules.finance`) | **Tarea:** TASK-402 / PB-041 / ADR-0013
- **Precondiciones:** Parámetro `finance.shift_opening_policy = SUPERVISOR_ONLY`.
- **Pasos:** Cajero intenta `CashShiftService.openShift(...)`; luego usuario con `finance:shifts_supervisor` abre turno.
- **Resultado Esperado:** Cajero recibe `403` (`UnauthorizedOperationException`); supervisor abre caja en estado `OPEN`.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (`shouldEnforceShiftOpeningPolicyWhenSupervisorOnly`)

### TC-FIN-006: Techo Máximo para Gastos Pagados desde Gaveta de Caja Chica
- **Módulo:** Finanzas (`modules.finance`) | **Tarea:** TASK-402 / PB-041 / ADR-0013
- **Precondiciones:** Turno `OPEN` y `finance.max_petty_cash_expense = 100.00`.
- **Pasos:** Registrar gasto de Bs. 150.00 con `paidFromCashDrawer = true` y luego con `paidFromCashDrawer = false`.
- **Resultado Esperado:** El de gaveta falla con HTTP `400` (`ValidationException`); el bancario/electrónico se registra.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (`shouldRejectPettyCashExpenseExceedingLimitWhenPaidFromDrawer`)

### TC-FIN-007: Política de Control Dual en Autoaprobación de Gastos
- **Módulo:** Finanzas (`modules.finance`) | **Tarea:** TASK-402 / PB-041 / ADR-0013
- **Precondiciones:** `finance.expense_allow_self_approval = false`.
- **Pasos:** `BRANCH_MANAGER` registra gasto propio; `ADMIN_TENANT` registra gasto propio.
- **Resultado Esperado:** El del encargado queda en `PENDING_APPROVAL`; el de `ADMIN_TENANT` pasa a `APPROVED`.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (`shouldEnforceExpenseSelfApprovalPolicy`)

### TC-POS-007: Bloqueo Reactivo de Interfaz POS ante Caja Cerrada y Banner con CTA
- **Módulo:** Ventas / POS (`frontend/src/app/features/pos`) | **Tarea:** TASK-403 / PB-041 / ADR-0013
- **Pasos:** Con `hasOpenShift() == false`, verificar `[data-testid="closed-shift-banner"]`, deshabilitación de catálogo/checkout, interceptación de mutaciones y apertura reactiva vía `[data-testid="open-shift-cta-btn"]`.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts`

### TC-POS-008: Ergonomía Silenciosa en Móviles y Barra Ámbar Condicional Offline
- **Módulo:** Ventas / POS Móvil | **Tarea:** TASK-403 / PB-041 / ADR-0013
- **Pasos:** Verificar header limpio online sin pendientes; desconectar red (`isOnline = false`) y reconectar con pendientes en Dexie.js mostrando `[data-testid="offline-warning-bar"]` y `[data-testid="sync-pending-btn"]`.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts`

### TC-POS-009: Trazabilidad de Cajero Activo en Comprobante Visual de Comanda
- **Módulo:** Ventas / POS | **Tarea:** TASK-403 / PB-041 / ADR-0013
- **Pasos:** Emitir orden y verificar `[data-testid="ticket-cashier-name"]` en `[data-testid="ticket-modal"]`.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts`

### TC-MENU-001: Creación de Plantilla de Menú Diario Reutilizable (PB-034)
- **Módulo:** Catálogo (`modules.catalog`) | **Tarea:** TASK-407 / TASK-408 / ADR-0015
- **Pasos:** `POST /api/v1/catalog/menus` con modalidad `INDIVIDUAL_ITEMS` y tiempos `STARTER`, `MAIN`, `DESSERT`; consultar `GET /api/v1/catalog/menus?templateOnly=true`.
- **Automatizado en:** `DailyMenuServiceTest.java` y `daily-menus.component.spec.ts`

### TC-MENU-002: Instanciación de Menú al Turno con Cupos Iniciales Personalizados (PB-034)
- **Módulo:** Finanzas & Catálogo | **Tarea:** TASK-407 / TASK-408
- **Pasos:** `POST /api/v1/finance/shifts/open` con `menuTemplateId` y `customQuotas`; verificar `shift_menu_items` sin mutar la plantilla base.
- **Automatizado en:** `ShiftMenuInstantiationTest.java` y `pos.component.spec.ts`

### TC-MENU-003: Decremento Atómico Concurrente y Bloqueo de Agotado en POS (PB-034)
- **Módulo:** Punto de Venta (`modules.pos`) | **Tarea:** TASK-407 / TASK-408 / ADR-0015
- **Pasos:** Cobrar la última unidad (`current_quota = 1`) -> `201 Created` e `is_sold_out = true`; intentar cobrar una segunda -> HTTP `409 Conflict` (`ITEM_SOLD_OUT`).
- **Automatizado en:** `AtomicQuotaEnforcementTest.java` y `pos.component.spec.ts`

### TC-MENU-004: Ampliación Supervisada de Cupos en Vivo con Trazabilidad (PB-034)
- **Módulo:** Catálogo / POS (`modules.catalog`) | **Tarea:** TASK-407 / TASK-408
- **Pasos:** `POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota` con `quantityDelta = 10` y motivo; verificar `is_sold_out = false` y auditoría en `shift_menu_quota_adjustments`.
- **Automatizado en:** `QuotaAdjustmentTest.java`

### TC-INV-005: Deducción Polimórfica según Estrategia de Inventario (PB-044)
- **Módulo:** Inventario & Kardex (`modules.inventory`) | **Tarea:** TASK-407 / TASK-408 / ADR-0015
- **Pasos:** Despachar `OrderPaidEvent` con platos `RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT` y `COMMERCIAL_NO_INVENTORY` y validar movimientos en `branch_stock` y `kardex_movements`.
- **Automatizado en:** `InventoryStrategyFactoryTest.java`

### TC-MOD-001: Venta en POS con Modificadores Físicos y Deducción SALE_CONSUMPTION en Kardex (PB-020)
- **Módulo:** POS & Inventario | **Tarea:** TASK-501 / ADR-0018
- **Pasos:** Registrar venta con modificador ligado a ingrediente (`quantityConsumed * item.quantity`) y otro comercial (`ingredientId == null`); verificar `kardex_movements` (`SALE_CONSUMPTION`).
- **Automatizado en:** `ModifierIntegrationTests.java`

### TC-MOD-002: Selector Interactivo de Modificadores, Notas de Cocina y Líneas Separadas (PB-020)
- **Módulo:** POS UI | **Tarea:** TASK-502 / ADR-0018
- **Pasos:** Abrir `[data-testid="modifier-selection-modal"]`, agregar nota en `[data-testid="item-kitchen-notes-input"]`, validar líneas separadas en carrito/ticket y persistencia offline en Dexie.js (`PendingOrderEntity`).
- **Automatizado en:** `pos.component.spec.ts` y `pos.service.spec.ts`

### TC-POS-013 *(antes TC-POS-008 duplicado)*: Cancelación de Pedidos, Reintegro de Gaveta y Tratamiento de Mermas (PB-031)
- **Módulo:** POS / Finanzas / Inventario / Seguridad | **Tarea:** TASK-504 / ADR-0019
- **Pasos:** Invocar `POST /api/v1/pos/orders/{orderId}/cancel` con PIN de supervisor válido, `wasPrepared = false` (reversión `CANCELLATION_REVERSAL`) y `wasPrepared = true` (`prepared_dish_buffer` o `WASTE_ADJUSTMENT`), validando `total_cash_refunds`.
- **Automatizado en:** `OrderCancellationIntegrationTests.java` y `KardexServiceTest.java`

### TC-POS-014 *(antes TC-POS-009 duplicado)*: Cola de Autorizaciones y UI de Cancelación (PB-031)
- **Módulo:** Finanzas / POS UI | **Tarea:** TASK-504 / ADR-0019
- **Pasos:** Configurar PIN en `/profile`; cancelar desde `/cash-control` con teclado táctil de PIN o envío remoto; aprobar/rechazar en `/finance` con `<app-authorization-queue>`.
- **Automatizado en:** `authorization-queue.component.spec.ts`, `cash-control.component.spec.ts`, `finance.component.spec.ts`, `profile.component.spec.ts`

### TC-POS-012: Modalidades de Servicio, Delivery Fee, Validaciones y Distintivos en Tickets (PB-032)
- **Módulo:** POS / Finanzas / UI | **Tarea:** TASK-505 / ADR-0020
- **Pasos:** Alternar `DINE_IN`, `TAKEOUT`, `SCHEDULED_PICKUP`, `DELIVERY`; exigir dirección en `DELIVERY`, sumar `deliveryFee`, verificar banners en ticket, filtros en `/cash-control` y persistencia offline Dexie.js.
- **Automatizado en:** `PosServiceTest.java`, `pos.component.spec.ts`, `pos.service.spec.ts`, `cash-control.component.spec.ts`

### TC-CAT-005: Catálogo Visual con Imágenes, Recorte Nativo WebP, Selector Triple y Ergonomía POS (PB-036)
- **Módulo:** Catálogo / POS | **Tarea:** TASK-506 / ADR-0021
- **Pasos:** Subir imagen en `/catalog`, recortar en `<app-image-cropper-modal>` (4:3 WebP 400x300), validar límite `413`, vistas `grid`/`compact`/`list` en POS y fallback de emoji.
- **Automatizado en:** `LocalFileStorageServiceTest.java`, `CatalogIntegrationTest.java`, `image-cropper-modal.component.spec.ts`, `catalog.component.spec.ts`, `pos.component.spec.ts`

### TC-CAT-006: Asistente de SKU Secuencial, Selector de Emojis y Prefijo de Categoría (PB-048)
- **Módulo:** Catálogo | **Tarea:** TASK-507 / ADR-0022
- **Pasos:** Crear categoría con `sortOrder` autocalculado, `codePrefix` sugerido y `<app-emoji-picker>`; validar `GET /api/v1/catalog/dishes/next-code` y formato `${prefix}-${n}`.
- **Automatizado en:** `CatalogIntegrationTest.java`, `emoji-picker.component.spec.ts`, `catalog.component.spec.ts`

### TC-FIN-013: Histórico Gerencial de Turnos, Filtros Multicriterio, Auditoría Drill-Down y CSV (PB-030)
- **Módulo:** Finanzas / Auditoría | **Tarea:** TASK-508 / ADR-0023
- **Pasos:** Filtrar turnos por fechas/cajero/estado/descuadre con zona horaria de sucursal (`ADR-0006`), exportar CSV con BOM UTF-8 (`;`), inspeccionar `<app-shift-detail-modal>` y descargar detalle CSV.
- **Automatizado en:** `CashShiftServiceTest.java`, `RestaurantModularityTests.java`, `finance.component.spec.ts`, `shift-detail-modal.component.spec.ts`, `csv-exporter.spec.ts`

---

## 2. Plataforma Backoffice SaaS, Storage e Infraestructura (Sprint 6)

### TC-INFRA-001: Perfiles Twelve-Factor, Pools HikariCP Explícitos, CORS Externalizado y Seeders (PB-025)
- **Módulo:** Core Config & Security | **Tarea:** TASK-601 / ADR-0026
- **Pasos:** Validar `application-{dev,testing-local,staging,prod}.yml`, `flyway.clean-disabled: true`, HikariCP, `@Profile({"dev", "test"})` en seeders y CORS externalizado.
- **Automatizado en:** `TwelveFactorProfileTests.java`

### TC-BO-AUTH-001: Identidad `platform_admins`, Aislamiento JWT por Audiencia y Bootstrap (PB-057)
- **Módulo:** `modules.backoffice` & `core.security` | **Tarea:** TASK-602 / ADR-0027
- **Pasos:** Validar bootstrap `superadmin`, fail-fast en perfiles estrictos, rechazo cruzado entre `aud=platform-backoffice` y `aud=erp-tenant`, rotación de refresh tokens y *Break-Glass*.
- **Automatizado en:** `PlatformJwtServiceTest.java`, `PlatformAuthAndBootstrapIntegrationTests.java`, `OnboardingIntegrationTests.java`

### TC-STORAGE-001: Motor Agnóstico `core.storage` (Local + S3/OCI), Magic Bytes y `platform_settings` (PB-035)
- **Módulo:** `core.storage`, `core.config`, `modules.catalog` | **Tarea:** TASK-603 / ADR-0028
- **Pasos:** Cambio dinámico `LOCAL`/`S3`, validación de rango (`102400`–`10485760` bytes), validación *magic bytes* reales (WebP/PNG/JPEG) y limpieza al reemplazar imagen.
- **Automatizado en:** `MediaStorageServiceTest.java`, `PlatformSettingsServiceImplTest.java`, `CatalogIntegrationTest.java`

### TC-BO-SAAS-001: Control Plane `modules.backoffice`, `core.entitlements`, Suscripciones, Cuotas y Telemetría (PB-002)
- **Módulo:** `modules.backoffice` & `core.entitlements` | **Tarea:** TASK-604 / ADR-0029
- **Pasos:** RBAC `SUPERADMIN` vs `PLATFORM_OPERATOR`, pagos FIFO/crédito, suspensiones (`POS_AND_SHIFT_ONLY` / `TOTAL`), gracia por días/órdenes, downgrade sin borrado, telemetría CQRS, prueba Storage y loggers en caliente.
- **Automatizado en:** `SubscriptionPlanAndLedgerServiceTest.java`, `BackofficeSaaSIntegrationTests.java`

### TC-BO-FE-001: SPA Backoffice React 19 `backoffice-frontend/` (PB-003)
- **Módulo:** `backoffice-frontend/` | **Tarea:** TASK-605 / ADR-0029
- **Pasos:** Formateadores ADR-0006, `window.__ENV__.API_BASE_URL`, *single-flight refresh* 401, flujo `mustChangePassword`, aislamiento RBAC de `/platform/admins`, planes a crédito, Storage probe y alerta en logger `DEBUG`/`TRACE`.
- **Automatizado en:** `formatters.test.ts`, `httpClient.test.ts`, `AppRoutes.test.tsx`, `TenantManagement.test.tsx`

### TC-BO-UAT-S6: Remediación de Hallazgos UAT Sprint 6 (`TASK-614`–`TASK-627`)
- **Módulo:** ERP + Backoffice SaaS + Plataforma (`V20`–`V22`) | **Tareas:** `TASK-614` a `TASK-627`
- **Cobertura Validada y Automatizada:**
  - **ERP (`TASK-614`, `TASK-615`, `TASK-626`, `TASK-627`):** Cambio reactivo de sucursal; confirmación de contraseña en `/onboarding` y limpieza ante 401; consulta historial `cash_shifts` vía JPA Specification evitando error PostgreSQL de tipo nulo; doble confirmación al abrir turno sin platos registrados en POS.
  - **Backoffice SaaS (`TASK-616`–`TASK-625`):** Búsqueda multicriterio de `platform_audit_logs` + modal JSON; campos opcionales de aprovisionamiento (`address`, `adminEmail`); tracking de `app_users.last_login_at` y badge `Sin primer ingreso`; primitivas UI compartidas (`Modal`, `Card`, `EmptyState`, `Badge`) y Dark Mode persistente (`ThemeProvider`); planes gratuitos e ilimitados (`duration_days = 0`) con `plan_category` (`PUBLIC_ONBOARDING`, `TRIAL`, `CUSTOM_PRIVATE`); email opcional en `platform_admins` con política configurable de contraseñas (`platform_settings`); pestaña "Usuarios" en detalle del tenant con reseteo temporal de contraseña y activación/desactivación.
- **Automatizado en:** `CashShiftServiceTest.java`, `PlatformAuditSearchTest.java`, `BackofficeTenantFirstLoginTest.java`, `BackofficeTenantUsersTest.java`, `SubscriptionPlanCategoryAndUnlimitedTest.java`, `PlatformAdminOptionalEmailAndPasswordPolicyTest.java`, `onboarding.component.spec.ts`, `pos.component.spec.ts`, `ObservabilityPage.test.tsx`, `TenantsPage.test.tsx`, `TenantUsersTab.test.tsx`, `PlansPage.test.tsx`, `AdminsPage.test.tsx`, `UiPrimitives.test.tsx`, `ThemeProvider.test.tsx`.

### TC-INFRA-DEVSECOPS-001: Stack Docker `testing-local`, `env.js`, Backup GPG y DevSecOps (PB-025 / PB-058)
- **Módulo:** `docker/`, `scripts/` | **Tarea:** TASK-606, TASK-607, TASK-608
- **Pasos:** Stack 6 contenedores, rutas Caddy, usuarios non-root, restore drill GPG AES-256 + SHA-256 y `./scripts/security/run-security-scan.sh --images`.
- **Automatizado en:** `api-base-url.interceptor.spec.ts`, `docker-compose.testing-local.yml`, `run-security-scan.sh`, `backup-postgres-s3.sh`, `restore-postgres-s3.sh`

---

## 3. Bitácora Viva de Pruebas de Usuario (UAT Sprint 5 + Sprint 6 en `testing-local`)

> Esta sección se alimentará y actualizará en tiempo real durante la sesión de pruebas manuales (`release/sprint-6`). Cada flujo validado aquí servirá como especificación directa para automatización E2E en Playwright.

### TC-UAT-001: Bootstrap Limpio + Primeras Configuraciones Backoffice + Aprovisionamiento de Inquilino
- **Aplicación:** Backoffice (`http://admin.localhost`)
- **Estado:** `✅ UAT BACKOFFICE VALIDADA (Pendiente Automatización E2E)`
- **Precondiciones:** Stack `testing-local` levantado desde cero (BD efímera vacía, sin seeds demo).
- **Pasos:**
  1. Ingresar a `http://admin.localhost` con `superadmin` / `SuperAdminLocal123!`.
  2. Revisar `/profile`, validar `/platform/settings` (`LOCAL`, `1048576`) y ejecutar "Probar Conexión" (`/app/uploads/dishes` mapeado a `./uploads/dishes`).
  3. Verificar los 4 planes base en `/plans` (`TRIAL_14D`, `WEEKLY_FLEX`, `MONTHLY_STANDARD`, `MONTHLY_PRO`) y crear plan personalizado ilimitado gratis (`FREE_SPECIAL`).
  4. Revisar `/platform/observability` (loggers en caliente y auditoría) y `/platform/admins`.
  5. Aprovisionar un nuevo inquilino desde `/tenants` ("Nuevo Inquilino").
- **Resultado Esperado:** Backoffice operativo, prueba de Storage exitosa, creación de plan e inquilino funcionales (mejoras UX/estado registradas en `OBS-004`–`OBS-010`).

### TC-UAT-002: Onboarding de Primer Restaurante desde Cero y Verificación en Backoffice
- **Aplicación:** ERP (`http://app.localhost/onboarding`) $\rightarrow$ Backoffice (`http://admin.localhost/tenants`)
- **Estado:** `🧪 EN PRUEBA MANUAL (Pendiente E2E)`
- **Pasos:**
  1. En `http://app.localhost/login`, navegar a `/onboarding` y registrar empresa, sucursal y administrador.
  2. Iniciar sesión en el ERP con la nueva empresa.
  3. En Backoffice (`/tenants` y `/tenants/:id`), verificar creación del tenant con plan `TRIAL_14D`, probar suspensión/gracia/pagos y confirmar auditoría en `/platform/observability`.
- **Resultado Esperado:** Alta atómica funcional y reflejo inmediato en el panel SaaS del Backoffice.
