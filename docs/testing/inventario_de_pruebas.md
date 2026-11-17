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
last_updated: "2026-11-13"
status: "active"
---

# 🧪 Inventario Maestro de Pruebas y Matriz de Regresión

> ⬅️ **Navegación:** [Bóveda de Pruebas](./README.md) | [Detalle de Flujos y UAT](./casos_uat_y_regresion.md) | [Guía E2E Playwright](./e2e-testing-guide.md) | [Observaciones y Hallazgos](../observaciones_y_hallazgos.md)

Este documento es el **índice maestro vivo** de todas las pruebas del sistema, categorizado por **Aplicación / Módulo** y **Nivel (N1–N4 según ADR-0017)**. El detalle paso a paso de cada flujo se encuentra en [`casos_uat_y_regresion.md`](./casos_uat_y_regresion.md).

---

## 1. Protocolo Obligatorio de Calidad (Checklist de Entrega)

1. **Backend 100% Verde:** `cd backend && ./mvnw clean test` (`206` tests + `RestaurantModularityTests`).
2. **ERP Frontend 100% Verde:** `cd frontend && npm run lint && npm test -- --watch=false` (`43` archivos / `381` tests).
3. **Backoffice Frontend 100% Verde:** `cd backoffice-frontend && npm run lint && npm test && npm run build` (`11` archivos / `22` tests).
4. **Regresión E2E 100% Verde:** `./scripts/run-e2e.sh` (Playwright POM).
5. **Sincronización:** Toda prueba manual UAT validada o corregida durante depuración se registra/actualiza aquí y en [`casos_uat_y_regresion.md`](./casos_uat_y_regresion.md) para automatizarse.

---

## 2. Matriz Categorizada por Aplicación y Módulo

### 2.1 ERP — Autenticación, Sesiones, Perfil y Onboarding

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-AUTH-001** | N4 (E2E) | Carga inicial, branding y formulario de login | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-002** | N4 (E2E) | Alternancia de tema Claro / Oscuro (`ThemeService`) | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-003** | N4 (E2E) | Redirección por rol `ADMIN_TENANT` a `/company` | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-004** | N4 (E2E) | Redirección por rol `CASHIER` directo al POS (`/pos`) | ✅ Automatizado | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-005** | N4 (Legacy) | Login calificado, tokens duales y *Silent Refresh* | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-AUTH-006** | N2 / N3 | Perfil autoservicio, humanización de personal y PIN (`TASK-509`) | ✅ Automatizado | `UserIntegrationTests.java`, `profile.component.spec.ts`, `users.component.spec.ts` |
| **TC-AUTH-007** | N2 (Comp) | Ocultar atajos demo y prellenado fuera de `dev` (`TASK-612`) | ✅ Automatizado | `frontend/src/app/features/auth/login/login.component.spec.ts` |
| **TC-AUTH-008** | N2 (Comp) | Confirmación de contraseña en `/onboarding` y limpieza ante 401 (`TASK-615`) | ✅ Automatizado | `frontend/src/app/features/onboarding/onboarding.component.spec.ts` |
| **TC-BRANCH-001** | N2 (Comp) | Cambio reactivo de sucursal sin recargar navegador (`TASK-614`) | ✅ Automatizado | `frontend/src/app/core/services/branch-context.service.spec.ts`, `pos.component.spec.ts` |
| **TC-ONB-001** | N4 (Legacy) | Wizard 3 pasos, slug, preset ciudad y alta atómica | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-TOUR-001** | N4 (Legacy) | Product Tour interactivo post-onboarding | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |

### 2.2 ERP — Catálogo, Menús Diarios, Emojis, SKU e Imágenes

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-CAT-001** | N4 (E2E) | Navegación entre pestañas Platos, Insumos y Categorías | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-002** | N4 (E2E) | Listado de platos del menú con precios base | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-003** | N4 (E2E) | Insumos base y categorías asociadas | ✅ Automatizado | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-005** | N2 / N3 | Catálogo visual, recorte WebP 4:3 (400x300) y vistas (`TASK-506`) | ✅ Automatizado | `LocalFileStorageServiceTest.java`, `image-cropper-modal.component.spec.ts`, `catalog.component.spec.ts` |
| **TC-CAT-006** | N2 / N3 | Asistente SKU secuencial, `<app-emoji-picker>` y prefijo (`TASK-507`) | ✅ Automatizado | `CatalogIntegrationTest.java`, `emoji-picker.component.spec.ts`, `catalog.component.spec.ts` |
| **TC-MENU-001** | N2 / N3 | Plantillas de Menú del Día en `/catalog/menus` con tiempos | ✅ Automatizado | `DailyMenuServiceTest.java`, `daily-menus.component.spec.ts` |
| **TC-MENU-002** | N4 (E2E) | Instanciación de Menú Diario al turno con cupos | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` / `ShiftMenuInstantiationTest.java` |
| **TC-MENU-003** | N4 (E2E) | Decremento atómico de cupos y badge `AGOTADO` en POS | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` / `AtomicQuotaEnforcementTest.java` |
| **TC-MENU-004** | N3 (Integr) | Ampliación supervisada de cupos en vivo con trazabilidad | ✅ Automatizado | `QuotaAdjustmentTest.java` |
| **TC-MENU-005** | N2 / N3 | Jornada de Sucursal compartida (`branch_service_sessions`), combos `MENU_COURSE`, `is_unlimited`, `allow_individual_sale` y `order_item_selections` (`TASK-634`) | ✅ Automatizado | `DailyMenuAndServiceSessionIntegrationTests.java`, `catalog.component.spec.ts`, `daily-menus.component.spec.ts`, `pos.component.spec.ts` |
| **TC-CAT-007** | N2 / N3 | Edición de platos/categorías, aislamiento `catalog.scope_mode` por sucursal, clonado destino y CRUD seguro de insumos (`TASK-631`, `TASK-632`, `TASK-635`) | ✅ Automatizado | `CatalogIntegrationTest.java`, `catalog.component.spec.ts` |
| **TC-SEC-004** | N2 / N3 | Bloqueo/redirección `mustChangePassword` en rutas ERP y parámetro `security.enforce_password_change_on_reset` (`TASK-628`) | ✅ Automatizado | `UserIntegrationTests.java`, `auth.guard.spec.ts`, `SystemParameterCatalogTest.java` |

### 2.3 ERP — Ventas (POS), Modificadores, Modalidades y Offline

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-POS-001** | N4 (E2E) | Conectividad en línea y catálogo visible | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-002** | N4 (E2E) | Adición de plato a comanda y cálculo dinámico | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-003** | N4 (E2E) | Selección de modificadores y extras con recargo (`TASK-210`) | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-004** | N4 (E2E) | Cobro en efectivo con cambio y emisión de ticket `T-` | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-005** | N4 (E2E) | Venta en Modo Offline (Dexie.js) y sincronización idempotente | ✅ Automatizado | `frontend/e2e/pos.spec.ts` |
| **TC-POS-006** | N3 (Integr) | Invariante de turno en backend (HTTP 409 `SHIFT_REQUIRED_ERROR`) | ✅ Automatizado | `PosIntegrationTest.java` |
| **TC-POS-007** | N2 (Comp) | Bloqueo reactivo POS ante caja cerrada y CTA apertura | ✅ Automatizado | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-POS-008** | N2 (Comp) | Ergonomía silenciosa móvil y barra ámbar condicional offline | ✅ Automatizado | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-POS-009** | N2 (Comp) | Trazabilidad de cajero activo en comprobante visual | ✅ Automatizado | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-POS-010** | N4 (E2E) | Bloqueo reactivo de catálogo y carrito sin turno activo | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-011** | N4 (E2E) | Apertura rápida de turno desde modal directo en POS | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-012** | N1 / N2 / N3 | Modalidades (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`) y *delivery fee* | ✅ Automatizado | `PosServiceTest.java`, `pos.component.spec.ts`, `pos.service.spec.ts` |
| **TC-POS-013** | N3 (Integr) | Cancelación de pedidos, reintegro de gaveta y mermas (`TASK-504`) | ✅ Automatizado | `OrderCancellationIntegrationTests.java`, `KardexServiceTest.java` |
| **TC-POS-014** | N2 (Comp) | Cola de autorizaciones (`<app-authorization-queue>`) y PIN táctil | ✅ Automatizado | `authorization-queue.component.spec.ts`, `cash-control.component.spec.ts` |
| **TC-POS-015** | N2 (Comp) | Advertencia y doble confirmación de apertura sin platos (`TASK-627`) | ✅ Automatizado | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-MOD-001** | N3 (Integr) | Venta con modificadores físicos y deducción `SALE_CONSUMPTION` | ✅ Automatizado | `ModifierIntegrationTests.java` |
| **TC-MOD-002** | N2 (Comp) | Selector de modificadores, notas cocina y líneas separadas | ✅ Automatizado | `pos.component.spec.ts`, `pos.service.spec.ts` |
| **TC-MOB-001** | N4 (Legacy) | POS móvil 390px, lista compacta y Bottom Sheet | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |

### 2.4 ERP — Control de Caja, Turnos, Gastos y Finanzas

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-FLOW-001..005** | N4 (E2E) | Ciclo completo: apertura $\rightarrow$ venta $\rightarrow$ gasto $\rightarrow$ aprobación $\rightarrow$ arqueo/cierre | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-001..004** | N4 (E2E) | KPIs de turno, desglose, registro de gasto y modal arqueo | ✅ Automatizado | `frontend/e2e/finance.spec.ts` |
| **TC-FIN-005** | N3 (Integr) | Apertura de turno con política `SUPERVISOR_ONLY` | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-006** | N3 (Integr) | Techo máximo de gastos de caja menor (`100.00` Bs.) | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-007** | N3 (Integr) | Control dual en autoaprobación de gastos | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-FIN-008..012** | N4 (E2E) | `/cash-control` vivo, filtros comandas, aprobación, arqueo y `/finance` | ✅ Automatizado | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-013** | N2 / N3 | Histórico multicriterio, drill-down `<app-shift-detail-modal>` y CSV (`TASK-508`) | ✅ Automatizado | `CashShiftServiceTest.java`, `finance.component.spec.ts`, `csv-exporter.spec.ts` |
| **TC-FIN-014** | N3 (Integr) | Historial de arqueos con filtros nulos vía JPA Specification (`TASK-626`) | ✅ Automatizado | `CashShiftServiceTest.java` |
| **TC-E2E-S5** | N4 (E2E) | Master Business Journey Sprint 5 (Parámetros, SKU, PIN, Delivery, Anulación, CSV) | ✅ Automatizado | `frontend/e2e/sprint5-journey.spec.ts` |

### 2.5 ERP — Inventario y Kardex Polimórfico

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-INV-001..004** | N4 (E2E) | KPIs insumos, existencias, Kardex y ajuste físico | ✅ Automatizado | `frontend/e2e/inventory.spec.ts` |
| **TC-INV-005** | N3 (Integr) | Deducción polimórfica (`RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT`, `COMMERCIAL_NO_INVENTORY`) | ✅ Automatizado | `InventoryStrategyFactoryTest.java`, `DailyMenuControllerIntegrationTests.java` |
| **TC-INV-006** | N2 (Comp) | Cálculo automático de costo unitario desde Valor Total del Lote y separación visual Stock Inicial vs Ajuste (`TASK-635`) | ✅ Automatizado | `inventory.component.spec.ts` |
| **TC-MOB-002** | N4 (Legacy) | Inventario móvil con tarjetas apilables y acordeón | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |

### 2.6 ERP — Configuración, RBAC y UI Kit

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-CFG-001** | N4 (Legacy) | Búsqueda reactiva y Hot-Reload de parámetros | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-CFG-002** | N2 / N3 | Catálogo JSON humanizado, búsqueda multicriterio y ayuda contextual (`TASK-510`) | ✅ Automatizado | `SystemParameterCatalogTest.java`, `parameters.component.spec.ts`, `context-help.component.spec.ts` |
| **TC-RBAC-001** | N4 (Legacy) | Alta de roles personalizados y matriz de privilegios | ⚠️ Migrar a POM | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-UIKIT-001..003** | N2 (Comp) | `ThemeService`, 8 componentes UI Kit y cola `z-[9999]` de Toasts | ✅ Automatizado | `theme.service.spec.ts`, `ui-kit.spec.ts`, `notification.service.spec.ts` |
| **TC-UI-001..004** | N2 (Comp) | Mini-rail 76px, móvil sin subtítulos, menú `[···]` POS y tipografía monospace | ✅ Automatizado | `theme.service.spec.ts`, `ui-kit.spec.ts`, `pos.component.spec.ts` |

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

### 2.8 Infraestructura, Storage Agnóstico y DevSecOps

| ID Caso | Nivel | Descripción / Flujo | Estado | Archivo Ejecutable / Referencia |
| :--- | :---: | :--- | :---: | :--- |
| **TC-INFRA-001** | N1 / N3 | Perfiles Twelve-Factor, HikariCP, CORS externalizado y `@Profile` seeders | ✅ Automatizado | `TwelveFactorProfileTests.java` |
| **TC-STORAGE-001** | N1 / N3 | Motor `core.storage` (`LOCAL` + `S3`), *magic bytes* y `platform_settings` | ✅ Automatizado | `MediaStorageServiceTest.java`, `PlatformSettingsServiceImplTest.java`, `CatalogIntegrationTest.java` |
| **TC-INFRA-DEVSECOPS-001** | N3 / Ops | Stack `testing-local`, `env.js`, backup/restore GPG AES-256 y `run-security-scan.sh` | ✅ Automatizado | `api-base-url.interceptor.spec.ts`, `docker-compose.testing-local.yml`, `run-security-scan.sh` |

---

## 3. Pruebas Manuales UAT Activas & Backlog de Automatización E2E

| ID Caso | Aplicación / Módulo | Flujo a Validar / Automatizar | Estado | Referencia |
| :--- | :--- | :--- | :---: | :--- |
| **TC-UAT-001** | Backoffice | Bootstrap limpio + Config inicial Backoffice + Alta Plan + Aprovisionamiento Inquilino | ✅ UAT Validada $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-001`](./casos_uat_y_regresion.md#tc-uat-001-bootstrap-limpio--primeras-configuraciones-backoffice--aprovisionamiento-de-inquilino) |
| **TC-UAT-002** | ERP Onboarding + Backoffice | Alta de primer restaurante desde cero + verificación suscripción/telemetría en Backoffice | 🧪 UAT Activa $\rightarrow$ E2E | [`casos_uat_y_regresion.md#tc-uat-002`](./casos_uat_y_regresion.md#tc-uat-002-onboarding-de-primer-restaurante-desde-cero-y-verificación-en-backoffice) |
| **TC-FUT-001** | ERP POS | Delivery & Llevar (cubierto en `TC-POS-012` y `TC-E2E-S5`; pendiente ampliar Edge Cases UAT) | 🔄 Parcial en E2E | `frontend/e2e/sprint5-journey.spec.ts` |
| **TC-FUT-002** | ERP Fiscal | Facturación Electrónica (NIT/CI y código QR fiscal) | ⏳ Futuro | Product Backlog |
| **TC-FUT-003** | ERP Inventario | Mermas y desperdicios directos en Kardex (parcial en cancelaciones `TC-POS-013`) | 🔄 Parcial en Backend | `OrderCancellationIntegrationTests.java` |
| **TC-FUT-004** | ERP Inventario | Transferencias de insumos entre sucursales | ⏳ Futuro | Product Backlog |
| **TC-FUT-005** | ERP Finanzas | Arqueo Ciego (*Blind Count*) — cubierto parcialmente en `TC-FIN-013` / `TC-E2E-S5` | 🔄 Parcial en E2E | `frontend/e2e/sprint5-journey.spec.ts` |

---

## 4. Plantilla Estándar para Nuevas Pruebas

```markdown
### [ID-CASO]: [Título Descriptivo del Flujo]
- **Aplicación / Módulo:** (ERP POS / ERP Catálogo / Backoffice SaaS / etc.)
- **Nivel:** (N1 Dominio / N2 Componente / N3 Integración / N4 E2E)
- **Historia / Tarea:** (ej. TASK-61X / OBS-00X)
- **Roles Involucrados:** (CASHIER / ADMIN_TENANT / SUPERADMIN / etc.)
- **Precondiciones:**
  - Estado inicial requerido.
- **Pasos:**
  1. Acción 1
  2. Acción 2
- **Resultado Esperado:**
  - Criterio verificable.
- **Estado / Archivo:** `🧪 Manual UAT` o `✅ Automatizado en frontend/e2e/<nombre>.spec.ts`
```
