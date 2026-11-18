---
title: "Inventario Maestro de Pruebas y Matriz de Regresión"
category: "testing"
tags:
  - testing
  - qa
  - e2e
  - uat
  - regression
  - test-cases
last_updated: "2026-11-17"
status: "active"
---

# 🧪 Inventario Maestro de Pruebas y Matriz de Regresión

> ⬅️ **Navegación:** [Bóveda de Pruebas](./README.md) | [Detalle de Flujos y UAT](./casos_uat_y_regresion.md) | [Guía E2E Playwright](./e2e-testing-guide.md) | [Observaciones y Hallazgos](../observaciones_y_hallazgos.md)

Este documento es el **índice maestro vivo** de todas las pruebas del sistema (`release/sprint-6`, migraciones **`V1`–`V24`**), categorizado por **Aplicación / Módulo**, **Nivel (N1–N4 según ADR-0017)** y con el registro completo de todas las suites ejecutables del repositorio.

---

## 1. Protocolo Obligatorio de Calidad (Checklist de Entrega)

1. **Backend 100% Verde:** `cd backend && ./mvnw clean test` (**`49` clases Surefire / `213` `@Test`**, incluye `RestaurantModularityTests` y `FlywayMigrationTest` con `V1`–`V24`).
2. **ERP Frontend 100% Verde:** `cd frontend && npm run lint && npm test -- --watch=false` (**`43` archivos `.spec.ts` / `390` casos de prueba**).
3. **Backoffice Frontend 100% Verde:** `cd backoffice-frontend && npm run lint && npm test && npm run build` (**`11` archivos `.test.*` / `22` tests**).
4. **Regresión E2E Playwright:** `./scripts/run-e2e.sh` (**`7` archivos `.spec.ts` / `34` tests + `4` Page Objects**; ver nota de refactor pendiente para `testing-local` en [`e2e-testing-guide.md`](./e2e-testing-guide.md#5-requisitos-previos-por-entorno-y-brecha-pendiente-en-testing-local)).
5. **Sincronización:** Toda prueba manual UAT validada o corregida se cataloga aquí y en [`casos_uat_y_regresion.md`](./casos_uat_y_regresion.md).

---

## 2. Matriz Categorizada por Aplicación y Módulo

### 2.1 ERP — Autenticación, Sesiones, Perfil, Sucursales y Onboarding

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-AUTH-001** | N4 (E2E) | Carga inicial, branding y formulario de login | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-002** | N4 (E2E) | Alternancia de tema Claro / Oscuro (`ThemeService`) | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-003** | N4 (E2E) | Redirección por rol `ADMIN_TENANT` a `/company` | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-004** | N4 (E2E) | Redirección por rol `CASHIER` directo al POS (`/pos`) | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-005** | N3 / N4 | Login calificado por tenant, JWT, rotación *Refresh Token* y *Silent Refresh* | ✅ N3 / ⚠️ N4 Legacy | `AuthIntegrationTests.java`, `JwtServiceTest.java`, `SessionLifecycleIntegrationTests.java`, `auth.service.spec.ts`, `auth.interceptor.spec.ts` |
| **TC-AUTH-006** | N2 / N3 | Perfil autoservicio, humanización de personal y PIN (`TASK-509`) | ✅ Automatizado | `UserIntegrationTests.java`, `UserManagementAdminResetTest.java`, `profile.component.spec.ts`, `users.component.spec.ts`, `user.service.spec.ts` |
| **TC-AUTH-007** | N2 (Comp) | Ocultar atajos demo y prellenado fuera de `dev` (`TASK-612`) | ✅ Automatizado | `login.component.spec.ts` |
| **TC-AUTH-008** | N2 / N3 | Confirmación de contraseña en `/onboarding` (`TASK-621`) y alta atómica de empresa | ✅ Automatizado | `OnboardingIntegrationTests.java`, `onboarding.component.spec.ts`, `onboarding.service.spec.ts` |
| **TC-SEC-004** | N2 / N3 | Bloqueo/redirección `mustChangePassword` en rutas ERP y parámetro `security.enforce_password_change_on_reset` (`TASK-628`) | ✅ Automatizado | `UserIntegrationTests.java`, `auth.guard.spec.ts`, `SystemParameterCatalogTest.java` |
| **TC-SEC-005** | N2 / N3 | Políticas condicionales de email en usuarios (`security.require_user_email`), RBAC sidebar y `anyPermissions` (`82ecd5e`) | ✅ Automatizado | `RbacDynamicRolesIntegrationTests.java`, `auth.guard.spec.ts`, `has-privilege.directive.spec.ts`, `app.component.spec.ts`, `users.component.spec.ts` |
| **TC-BRANCH-001** | N2 / N3 | Gestión de empresa/sucursales y cambio reactivo sin recargar navegador (`TASK-202`, `TASK-614`) | ✅ Automatizado | `CompanyIntegrationTests.java`, `company.service.spec.ts`, `company.component.spec.ts`, `branch-context.service.spec.ts` |
| **TC-ONB-001** | N3 / N4 | Wizard 3 pasos, slug, preset ciudad y alta atómica | ✅ N3 / ⚠️ N4 Legacy | `OnboardingIntegrationTests.java`, `onboarding.component.spec.ts` |
| **TC-TOUR-001** | N2 / N4 | Product Tour interactivo post-onboarding | ✅ N2 / ⚠️ N4 Legacy | `product-tour.component.spec.ts`, `onboarding-tour.service.spec.ts` |

### 2.2 ERP — Catálogo, Menús Diarios, Sesiones de Servicio, Emojis, SKU e Imágenes

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-CAT-001** | N4 (E2E) | Navegación entre pestañas Platos, Insumos y Categorías | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-002** | N4 (E2E) | Listado de platos del menú con precios base | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-003** | N4 (E2E) | Insumos base y categorías asociadas | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-004** | N3 (Integr) | Contratos públicos `.api` de catálogo, combos base e inicialización | ✅ Automatizado | `CatalogPublicApiTest.java`, `ComboIntegrationTests.java`, `CatalogDataInitializerTest.java` |
| **TC-CAT-005** | N2 / N3 | Catálogo visual, recorte WebP 4:3, almacenamiento `core.storage` y resolución cross-origin (`TASK-506`, `TASK-629`) | ✅ Automatizado | `MediaStorageServiceTest.java`, `CatalogIntegrationTest.java`, `api-base-url.interceptor.spec.ts`, `image-cropper-modal.component.spec.ts`, `catalog.component.spec.ts` |
| **TC-CAT-006** | N2 / N3 | Asistente SKU secuencial, `<app-emoji-picker>` y prefijo (`TASK-507`) | ✅ Automatizado | `CatalogIntegrationTest.java`, `emoji-picker.component.spec.ts`, `catalog.component.spec.ts`, `catalog.service.spec.ts` |
| **TC-CAT-007** | N2 / N3 | Edición de platos/categorías, aislamiento `catalog.scope_mode` por sucursal, clonado destino y CRUD seguro de insumos (`TASK-631`, `TASK-632`, `TASK-635`) | ✅ Automatizado | `CatalogIntegrationTest.java`, `catalog.component.spec.ts`, `catalog.service.spec.ts` |
| **TC-MENU-001** | N2 / N3 | Plantillas de Menú del Día en `/catalog/menus` con tiempos y múltiples días (`V24`) | ✅ Automatizado | `DailyMenuServiceTest.java`, `DailyMenuAndServiceSessionIntegrationTests.java`, `daily-menus.component.spec.ts` |
| **TC-MENU-002** | N4 (E2E) | Instanciación de Menú Diario al turno con cupos | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts`, `ShiftMenuInstantiationTest.java` |
| **TC-MENU-003** | N4 (E2E) | Decremento atómico de cupos y badge `AGOTADO` en POS | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts`, `AtomicQuotaEnforcementTest.java` |
| **TC-MENU-004** | N3 (Integr) | Ampliación supervisada de cupos en vivo con trazabilidad | ✅ Automatizado | `QuotaAdjustmentTest.java` |
| **TC-MENU-005** | N2 / N3 | Jornada de Sucursal compartida (`branch_service_sessions`), combos `MENU_COURSE`, `is_unlimited`, `allow_individual_sale` y `order_item_selections` (`TASK-634`) | ✅ Automatizado | `DailyMenuAndServiceSessionIntegrationTests.java`, `catalog.component.spec.ts`, `daily-menus.component.spec.ts`, `pos.component.spec.ts` |

### 2.3 ERP — Ventas (POS), Modificadores, Modalidades, Promociones y Offline

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-POS-001** | N4 (E2E) | Conectividad en línea y catálogo visible | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-002** | N4 (E2E) | Adición de plato a comanda y cálculo dinámico | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-003** | N4 (E2E) | Selección de modificadores y extras con recargo (`TASK-210`) | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-004** | N4 (E2E) | Cobro en efectivo con cambio y emisión de ticket `T-` | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-005** | N2 / N4 | Venta en Modo Offline (Dexie.js DAL) y sincronización idempotente | ✅ Automatizado | `frontend/e2e/pos.spec.ts`, `repositories.spec.ts` |
| **TC-POS-006** | N3 (Integr) | Invariante de turno en backend (HTTP 409 `SHIFT_REQUIRED_ERROR`) | ✅ Automatizado | `PosIntegrationTest.java` |
| **TC-POS-007** | N2 (Comp) | Bloqueo reactivo POS ante caja cerrada y CTA apertura | ✅ Automatizado | `pos.component.spec.ts` |
| **TC-POS-008** | N2 (Comp) | Ergonomía silenciosa móvil, Bottom Sheet y barra ámbar condicional offline | ✅ Automatizado | `pos.component.spec.ts`, `bottom-sheet.component.spec.ts` |
| **TC-POS-009** | N2 (Comp) | Trazabilidad de cajero, cambio calculado (`changeAmount`) y hora local en comprobante (`TASK-403`, `82ecd5e`) | ✅ Automatizado | `pos.component.spec.ts` |
| **TC-POS-010** | N4 (E2E) | Bloqueo reactivo de catálogo y carrito sin turno activo | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-011** | N4 (E2E) | Apertura rápida de turno desde modal directo en POS | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-012** | N1 / N2 / N3 | Modalidades (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`) y *delivery fee* | ✅ Automatizado | `PosServiceTest.java`, `pos.component.spec.ts`, `pos.service.spec.ts` |
| **TC-POS-013** | N3 (Integr) | Cancelación de pedidos, reintegro de gaveta y mermas (`TASK-504`) | ✅ Automatizado | `OrderCancellationIntegrationTests.java`, `KardexServiceTest.java` |
| **TC-POS-014** | N2 (Comp) | Cola de autorizaciones (`<app-authorization-queue>`) y PIN táctil | ✅ Automatizado | `authorization-queue.component.spec.ts`, `cash-control.component.spec.ts` |
| **TC-POS-015** | N2 (Comp) | Advertencia y doble confirmación de apertura sin platos (`TASK-627`) | ✅ Automatizado | `pos.component.spec.ts` |
| **TC-POS-016** | N3 (Integr) | Motor de promociones temporales (`Happy Hour`) por huso local de sucursal | ✅ Automatizado | `PromotionIntegrationTests.java` |
| **TC-MOD-001** | N3 (Integr) | Venta con modificadores físicos y deducción `SALE_CONSUMPTION` | ✅ Automatizado | `ModifierIntegrationTests.java` |
| **TC-MOD-002** | N2 (Comp) | Selector de modificadores, notas cocina y líneas separadas | ✅ Automatizado | `pos.component.spec.ts`, `pos.service.spec.ts` |
| **TC-MOB-001** | N2 / N4 | POS móvil 390px, lista compacta y Bottom Sheet | ✅ N2 / ⚠️ N4 Legacy | `bottom-sheet.component.spec.ts`, `pos.component.spec.ts` |

### 2.4 ERP — Control de Caja, Turnos, Gastos y Finanzas

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-FLOW-001..005** | N4 (E2E) | Ciclo completo: apertura $\rightarrow$ venta $\rightarrow$ gasto $\rightarrow$ aprobación $\rightarrow$ arqueo/cierre | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-001..004** | N4 (E2E) | KPIs de turno, desglose, registro de gasto y modal arqueo | ✅ Automatizado | `frontend/e2e/finance.spec.ts` |
| **TC-FIN-005** | N3 (Integr) | Apertura de turno con política `SUPERVISOR_ONLY` | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-006** | N3 (Integr) | Techo máximo de gastos de caja menor (`100.00` Bs.) | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-007** | N3 (Integr) | Control dual en autoaprobación de gastos | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-008..012** | N4 (E2E) | `/cash-control` vivo, filtros comandas, aprobación, arqueo y `/finance` | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-013** | N2 / N3 | Histórico multicriterio, drill-down `<app-shift-detail-modal>` y CSV (`TASK-508`) | ✅ Automatizado | `CashShiftServiceTest.java`, `finance.service.spec.ts`, `finance.component.spec.ts`, `shift-detail-modal.component.spec.ts`, `csv-exporter.spec.ts` |
| **TC-FIN-014** | N3 (Integr) | Historial de arqueos con filtros nulos vía JPA Specification (`TASK-626`) | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-E2E-S5** | N4 (E2E) | Master Business Journey Sprint 5 (Parámetros, SKU, PIN, Delivery, Anulación, CSV) | ✅ Automatizado | `frontend/e2e/sprint5-journey.spec.ts` |

### 2.5 ERP — Inventario y Kardex Polimórfico

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-INV-001..004** | N4 (E2E) | KPIs insumos, existencias, Kardex y ajuste físico | ✅ Automatizado | `frontend/e2e/inventory.spec.ts` |
| **TC-INV-005** | N1 / N3 | Deducción polimórfica (`RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT`, `COMMERCIAL_NO_INVENTORY`) y listener `OrderPaidEvent` | ✅ Automatizado | `InventoryStrategyFactoryTest.java`, `InventoryEventListenerTest.java`, `DailyMenuAndServiceSessionIntegrationTests.java` |
| **TC-INV-006** | N2 (Comp) | Cálculo automático de costo unitario desde Valor Total del Lote y separación visual Stock Inicial vs Ajuste (`TASK-635`) | ✅ Automatizado | `inventory.component.spec.ts`, `inventory.service.spec.ts` |

### 2.6 ERP — Configuración, Core HTTP, Inactividad, RBAC y UI Kit

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-CFG-001** | N1 / N3 | Resolución jerárquica de parámetros, caché, ETags HTTP 304 y API `/settings` | ✅ Automatizado | `CascadingParameterResolverTest.java`, `ConfigurationControllerIntegrationTest.java`, `ConfigurationServiceTest.java`, `ParameterCacheServiceTest.java`, `TenantConfigServiceTest.java`, `configuration.service.spec.ts` |
| **TC-CFG-002** | N2 / N3 | Catálogo JSON humanizado, búsqueda multicriterio y ayuda contextual (`TASK-510`) | ✅ Automatizado | `SystemParameterCatalogTest.java`, `parameters.component.spec.ts`, `context-help.component.spec.ts` |
| **TC-RBAC-001** | N2 / N3 | Roles dinámicos, privilegios agrupados, asignación multi-sucursal y directiva UI | ✅ Automatizado | `RbacDynamicRolesIntegrationTests.java`, `roles.component.spec.ts`, `has-privilege.directive.spec.ts` |
| **TC-CORE-001** | N1 / N2 / N3 | Serialización ISO fechas/horas (`ADR-0006`), `GlobalExceptionHandler` RFC 7807, `IdleTimerService` e interceptores HTTP | ✅ Automatizado | `DateTimeSerializationTests.java`, `GlobalExceptionHandlerTest.java`, `error.interceptor.spec.ts`, `idle-timer.service.spec.ts` |
| **TC-UIKIT-001..004** | N2 (Comp) | `ThemeService`, `AppComponent` mini-rail, 8 componentes UI Kit y cola `z-[9999]` de Toasts | ✅ Automatizado | `app.component.spec.ts`, `theme.service.spec.ts`, `ui-kit.spec.ts`, `notification.service.spec.ts` |

### 2.7 Backoffice SaaS (`backoffice-frontend/` & `modules.backoffice`)

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-BO-AUTH-001** | N1 / N3 | `platform_admins`, JWT por audiencia, bootstrap fail-fast y *Break-Glass* | ✅ Automatizado | `PlatformJwtServiceTest.java`, `PlatformAuthAndBootstrapIntegrationTests.java` |
| **TC-BO-SAAS-001** | N1 / N3 | RBAC plataforma, pagos FIFO/crédito, gracia, bloqueos, cuotas y telemetría | ✅ Automatizado | `SubscriptionPlanAndLedgerServiceTest.java`, `BackofficeSaaSIntegrationTests.java` |
| **TC-BO-FE-001** | N2 (Comp) | Auth `mustChangePassword`, refresh 401, RBAC, crédito, Storage probe y loggers | ✅ Automatizado | `formatters.test.ts`, `httpClient.test.ts`, `AppRoutes.test.tsx`, `TenantManagement.test.tsx` |
| **TC-BO-OBS-001** | N2 / N3 | Búsqueda multicriterio de auditoría plataforma y detalle JSON (`TASK-616`) | ✅ Automatizado | `PlatformAuditSearchTest.java`, `ObservabilityPage.test.tsx` |
| **TC-BO-TENANT-001** | N2 / N3 | Alta tenant con dirección/email opcionales y estado `Sin primer ingreso` (`TASK-617`, `TASK-618`) | ✅ Automatizado | `BackofficeTenantFirstLoginTest.java`, `TenantsPage.test.tsx` |
| **TC-BO-USERS-001** | N2 / N3 | Listado de usuarios de tenant, reset de contraseña temporal y estado (`TASK-625`) | ✅ Automatizado | `BackofficeTenantUsersTest.java`, `TenantUsersTab.test.tsx` |
| **TC-BO-PLANS-001** | N2 / N3 | Planes categorizados (`PUBLIC_ONBOARDING`/`TRIAL`/`CUSTOM_PRIVATE`) e ilimitados gratis (`TASK-623`) | ✅ Automatizado | `SubscriptionPlanCategoryAndUnlimitedTest.java`, `PlansPage.test.tsx` |
| **TC-BO-ADMINS-001** | N2 / N3 | Email opcional en `platform_admins` y política configurable de contraseñas (`TASK-624`) | ✅ Automatizado | `PlatformAdminOptionalEmailAndPasswordPolicyTest.java`, `AdminsPage.test.tsx` |
| **TC-BO-UI-001** | N2 (Comp) | Primitivas UI reutilizables y Dark Mode persistente en Backoffice (`TASK-620`, `TASK-622`) | ✅ Automatizado | `UiPrimitives.test.tsx`, `ThemeProvider.test.tsx` |

### 2.8 Arquitectura, Migraciones, Storage Agnóstico y DevSecOps

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-ARQ-001** | N3 (Arq) | Arranque de contexto Spring Boot, límites Spring Modulith (`verify()`) y migraciones Flyway `V1`–`V24` | ✅ Automatizado | `RestaurantApplicationTests.java`, `RestaurantModularityTests.java`, `FlywayMigrationTest.java` |
| **TC-INFRA-001** | N1 / N3 | Perfiles Twelve-Factor, HikariCP, CORS externalizado y `@Profile` seeders | ✅ Automatizado | `TwelveFactorProfileTests.java` |
| **TC-STORAGE-001** | N1 / N3 | Motor `core.storage` (`LOCAL` + `S3`), *magic bytes* y `platform_settings` | ✅ Automatizado | `MediaStorageServiceTest.java`, `PlatformSettingsServiceImplTest.java`, `CatalogIntegrationTest.java` |
| **TC-INFRA-DEVSECOPS-001** | N3 / Ops | Stack `testing-local`, `env.js`, backup/restore GPG AES-256 y `run-security-scan.sh` | ✅ Automatizado | `api-base-url.interceptor.spec.ts`, `docker-compose.testing-local.yml`, `run-security-scan.sh` |

---

## 3. Catálogo Exhaustivo de Suites Automatizadas en el Repositorio (`release/sprint-6`)

### 3.1 Backend Java / Spring Boot (`49` clases Surefire / `213` `@Test`)

*(Adicionalmente existe `TestH2DbConfig.java` como `@TestConfiguration` auxiliar de soporte H2, completando los `50` archivos Java en `backend/src/test/`).*

| # | Paquete / Ruta relativa en `backend/src/test/java/com/restaurant/app/` | Tests (`@Test`) |
| :---: | :--- | :---: |
| 1 | `FlywayMigrationTest.java` | 3 |
| 2 | `RestaurantApplicationTests.java` | 1 |
| 3 | `RestaurantModularityTests.java` | 2 |
| 4 | `core/common/DateTimeSerializationTests.java` | 1 |
| 5 | `core/common/GlobalExceptionHandlerTest.java` | 9 |
| 6 | `core/config/PlatformSettingsServiceImplTest.java` | 2 |
| 7 | `core/config/TenantConfigServiceTest.java` | 3 |
| 8 | `core/config/TwelveFactorProfileTests.java` | 5 |
| 9 | `core/security/AuthIntegrationTests.java` | 13 |
| 10 | `core/security/JwtServiceTest.java` | 4 |
| 11 | `core/security/RbacDynamicRolesIntegrationTests.java` | 5 |
| 12 | `core/security/SessionLifecycleIntegrationTests.java` | 3 |
| 13 | `core/security/UserIntegrationTests.java` | 10 |
| 14 | `core/security/internal/UserManagementAdminResetTest.java` | 2 |
| 15 | `core/storage/internal/MediaStorageServiceTest.java` | 4 |
| 16 | `modules/backoffice/internal/BackofficeSaaSIntegrationTests.java` | 2 |
| 17 | `modules/backoffice/internal/BackofficeTenantFirstLoginTest.java` | 2 |
| 18 | `modules/backoffice/internal/BackofficeTenantUsersTest.java` | 3 |
| 19 | `modules/backoffice/internal/PlatformAdminOptionalEmailAndPasswordPolicyTest.java` | 3 |
| 20 | `modules/backoffice/internal/PlatformAuditSearchTest.java` | 1 |
| 21 | `modules/backoffice/internal/PlatformAuthAndBootstrapIntegrationTests.java` | 3 |
| 22 | `modules/backoffice/internal/PlatformJwtServiceTest.java` | 2 |
| 23 | `modules/backoffice/internal/SubscriptionPlanAndLedgerServiceTest.java` | 7 |
| 24 | `modules/backoffice/internal/SubscriptionPlanCategoryAndUnlimitedTest.java` | 2 |
| 25 | `modules/catalog/ComboIntegrationTests.java` | 3 |
| 26 | `modules/catalog/DailyMenuAndServiceSessionIntegrationTests.java` | 3 |
| 27 | `modules/catalog/ModifierIntegrationTests.java` | 3 |
| 28 | `modules/catalog/internal/CatalogDataInitializerTest.java` | 1 |
| 29 | `modules/catalog/internal/CatalogIntegrationTest.java` | 13 |
| 30 | `modules/catalog/internal/CatalogPublicApiTest.java` | 7 |
| 31 | `modules/catalog/internal/DailyMenuServiceTest.java` | 1 |
| 32 | `modules/catalog/internal/QuotaAdjustmentTest.java` | 1 |
| 33 | `modules/catalog/internal/ShiftMenuInstantiationTest.java` | 1 |
| 34 | `modules/company/CompanyIntegrationTests.java` | 6 |
| 35 | `modules/company/OnboardingIntegrationTests.java` | 5 |
| 36 | `modules/configuration/internal/CascadingParameterResolverTest.java` | 4 |
| 37 | `modules/configuration/internal/ConfigurationControllerIntegrationTest.java` | 5 |
| 38 | `modules/configuration/internal/ConfigurationServiceTest.java` | 6 |
| 39 | `modules/configuration/internal/ParameterCacheServiceTest.java` | 4 |
| 40 | `modules/configuration/internal/SystemParameterCatalogTest.java` | 7 |
| 41 | `modules/finance/internal/CashShiftServiceTest.java` | 11 |
| 42 | `modules/inventory/internal/InventoryEventListenerTest.java` | 3 |
| 43 | `modules/inventory/internal/InventoryStrategyFactoryTest.java` | 1 |
| 44 | `modules/inventory/internal/KardexServiceTest.java` | 7 |
| 45 | `modules/pos/AtomicQuotaEnforcementTest.java` | 1 |
| 46 | `modules/pos/OrderCancellationIntegrationTests.java` | 5 |
| 47 | `modules/pos/PromotionIntegrationTests.java` | 4 |
| 48 | `modules/pos/internal/PosIntegrationTest.java` | 7 |
| 49 | `modules/pos/internal/PosServiceTest.java` | 12 |
| | **Total Backend (`./mvnw clean test`)** | **213** |

---

### 3.2 ERP Frontend Angular 22 (`43` archivos `.spec.ts` / `390` tests)

| # | Archivo relativo en `frontend/src/app/` | Casos (`it`/`test`) |
| :---: | :--- | :---: |
| 1 | `app.component.spec.ts` | 14 |
| 2 | `core/components/product-tour/product-tour.component.spec.ts` | 3 |
| 3 | `core/directives/has-privilege.directive.spec.ts` | 3 |
| 4 | `core/guards/auth.guard.spec.ts` | 11 |
| 5 | `core/interceptors/api-base-url.interceptor.spec.ts` | 4 |
| 6 | `core/interceptors/auth.interceptor.spec.ts` | 4 |
| 7 | `core/interceptors/error.interceptor.spec.ts` | 6 |
| 8 | `core/repositories/repositories.spec.ts` | 7 |
| 9 | `core/services/auth.service.spec.ts` | 7 |
| 10 | `core/services/branch-context.service.spec.ts` | 6 |
| 11 | `core/services/catalog.service.spec.ts` | 18 |
| 12 | `core/services/company.service.spec.ts` | 5 |
| 13 | `core/services/configuration.service.spec.ts` | 6 |
| 14 | `core/services/finance.service.spec.ts` | 5 |
| 15 | `core/services/idle-timer.service.spec.ts` | 4 |
| 16 | `core/services/inventory.service.spec.ts` | 5 |
| 17 | `core/services/notification.service.spec.ts` | 3 |
| 18 | `core/services/onboarding-tour.service.spec.ts` | 7 |
| 19 | `core/services/onboarding.service.spec.ts` | 3 |
| 20 | `core/services/pos.service.spec.ts` | 26 |
| 21 | `core/services/theme.service.spec.ts` | 6 |
| 22 | `core/services/user.service.spec.ts` | 6 |
| 23 | `features/auth/login/login.component.spec.ts` | 14 |
| 24 | `features/cash-control/cash-control.component.spec.ts` | 13 |
| 25 | `features/catalog/catalog.component.spec.ts` | 29 |
| 26 | `features/catalog/menus/daily-menus.component.spec.ts` | 6 |
| 27 | `features/company/company.component.spec.ts` | 6 |
| 28 | `features/finance/components/shift-detail-modal/shift-detail-modal.component.spec.ts` | 6 |
| 29 | `features/finance/finance.component.spec.ts` | 14 |
| 30 | `features/inventory/inventory.component.spec.ts` | 11 |
| 31 | `features/onboarding/onboarding.component.spec.ts` | 7 |
| 32 | `features/pos/pos.component.spec.ts` | 58 |
| 33 | `features/profile/profile.component.spec.ts` | 6 |
| 34 | `features/settings/parameters/parameters.component.spec.ts` | 13 |
| 35 | `features/users/roles/roles.component.spec.ts` | 4 |
| 36 | `features/users/users.component.spec.ts` | 6 |
| 37 | `shared/components/bottom-sheet/bottom-sheet.component.spec.ts` | 5 |
| 38 | `shared/components/ui/authorization-queue/authorization-queue.component.spec.ts` | 7 |
| 39 | `shared/components/ui/context-help/context-help.component.spec.ts` | 5 |
| 40 | `shared/components/ui/emoji-picker/emoji-picker.component.spec.ts` | 7 |
| 41 | `shared/components/ui/image-cropper-modal/image-cropper-modal.component.spec.ts` | 6 |
| 42 | `shared/components/ui/ui-kit.spec.ts` | 6 |
| 43 | `shared/utils/csv-exporter.spec.ts` | 2 |
| | **Total ERP Frontend (`npm test -- --watch=false`)** | **390** |

---

### 3.3 Backoffice Frontend React 19 (`11` archivos `.test.*` / `22` tests)

| # | Archivo relativo en `backoffice-frontend/src/` | Tests |
| :---: | :--- | :---: |
| 1 | `AppRoutes.test.tsx` | 3 |
| 2 | `components/UiPrimitives.test.tsx` | 2 |
| 3 | `components/tenant/TenantManagement.test.tsx` | 3 |
| 4 | `components/tenant/TenantUsersTab.test.tsx` | 2 |
| 5 | `context/ThemeProvider.test.tsx` | 1 |
| 6 | `pages/AdminsPage.test.tsx` | 1 |
| 7 | `pages/ObservabilityPage.test.tsx` | 2 |
| 8 | `pages/PlansPage.test.tsx` | 1 |
| 9 | `pages/TenantsPage.test.tsx` | 2 |
| 10 | `services/httpClient.test.ts` | 2 |
| 11 | `utils/formatters.test.ts` | 3 |
| | **Total Backoffice React (`npm test`)** | **22** |

---

### 3.4 Playwright E2E (`7` archivos `.spec.ts` / `34` tests + `4` Page Objects)

| # | Archivo en `frontend/e2e/` | Tests |
| :---: | :--- | :---: |
| 1 | `auth.spec.ts` | 4 |
| 2 | `catalog.spec.ts` | 3 |
| 3 | `finance.spec.ts` | 4 |
| 4 | `inventory.spec.ts` | 4 |
| 5 | `pos.spec.ts` | 5 |
| 6 | `shift-lifecycle.spec.ts` | 6 |
| 7 | `sprint5-journey.spec.ts` | 8 |
| | **Total Playwright E2E** *(Page Objects: `auth.page.ts`, `pos.page.ts`, `cash-control.page.ts`, `finance.page.ts`)* | **34** |

---

## 4. Pruebas Manuales UAT Validadas & Backlog de Automatización E2E

| ID Caso | Aplicación / Módulo | Flujo Validado / Por Automatizar | Estado | Referencia |
| :--- | :--- | :--- | :---: | :--- |
| **TC-UAT-001** | Backoffice | Bootstrap limpio + Config inicial Backoffice + Alta Plan + Aprovisionamiento Inquilino | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-001`](./casos_uat_y_regresion.md#tc-uat-001) |
| **TC-UAT-002** | ERP Onboarding + Backoffice | Alta de primer restaurante desde cero + verificación suscripción/telemetría en Backoffice | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-002`](./casos_uat_y_regresion.md#tc-uat-002) |
| **TC-UAT-003** | Backoffice + ERP Auth/Users | `mustChangePassword` obligatorio post-reset y políticas de email en `/users` (`TASK-628` + `82ecd5e`) | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-003`](./casos_uat_y_regresion.md#tc-uat-003) |
| **TC-UAT-004** | ERP Catálogo / POS / Caddy | Subida y visualización WebP `/uploads/dishes/...` en `http://app.localhost` (`TASK-629`) | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-004`](./casos_uat_y_regresion.md#tc-uat-004) |
| **TC-UAT-005** | ERP Catálogo / POS / Caja | Edición platos/categorías, precios sucursal, clonado destino y limpieza jerga (`TASK-630`–`632`) | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-005`](./casos_uat_y_regresion.md#tc-uat-005) |
| **TC-UAT-006** | ERP Catálogo / Menús / POS | Jornada compartida, menús multi-día (`V24`), combos `MENU_COURSE`, cupos ilimitados y cambio en ticket (`TASK-634` + `82ecd5e`) | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-006`](./casos_uat_y_regresion.md#tc-uat-006) |
| **TC-UAT-007** | ERP Insumos / Inventario | CRUD seguro de insumos, Stock Inicial vs Ajuste y costo por Valor Total del Lote (`TASK-635`) | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-007`](./casos_uat_y_regresion.md#tc-uat-007) |
| **TC-FUT-001** | ERP POS | Delivery & Llevar (cubierto en `TC-POS-012` y `TC-E2E-S5`; pendiente cableado de parámetros en `TASK-640`) | 🔄 Parcial en E2E | `frontend/e2e/sprint5-journey.spec.ts` |
| **TC-FUT-002** | ERP Fiscal | Facturación Electrónica (NIT/CI y código QR fiscal, `PB-007`) | ⏳ Futuro | Product Backlog P2 |
| **TC-FUT-003** | ERP Inventario | Mermas directas, pre-elaborados y compras (`TASK-636` / `PB-021`) | 🔄 Parcial en Backend | `OrderCancellationIntegrationTests.java` |
| **TC-FUT-004** | ERP Inventario | Transferencias de insumos entre sucursales (`PB-004`) | ⏳ Futuro | Product Backlog P1 |
| **TC-FUT-005** | ERP Finanzas | Arqueo Ciego (*Blind Count*) — cubierto en `TC-FIN-013` / `TC-E2E-S5` | 🔄 Parcial en E2E | `frontend/e2e/sprint5-journey.spec.ts` |
