---
title: "Inventario Maestro de Pruebas y Matriz de Regresión"
category: "testing"
tags:
  - testing
  - qa
  - e2e
  - regression
  - test-cases
last_updated: "2026-10-29"
status: "active"
---

# 🧪 Inventario Maestro de Pruebas y Matriz de Regresión

Este documento es el **artefacto vivo** que inventaría la totalidad de pruebas del sistema (funcionales, transaccionales, de seguridad y de regresión). **Debe ser actualizado y ampliado cada vez que se implemente una nueva funcionalidad** en el producto.

---

## 1. Protocolo Obligatorio de Calidad (Checklist de Entrega)

Antes de dar por concluida cualquier tarea (`TASK-xxx`) o mezclar a la rama principal (`main`), es **estrictamente obligatorio** ejecutar la siguiente secuencia sin excepciones:

```mermaid
flowchart LR
    Step1["1. Backend Unit/Modulith<br/>./backend/mvnw clean test"] --> Step2["2. Frontend Unit & Lint<br/>npm run lint && npm test"]
    Step2 --> Step3["3. Suite E2E Completa<br/>./scripts/run-e2e.sh"]
    Step3 --> Step4["4. Verificación de Artefactos<br/>Sincronizar Docs y Diagramas"]
```

- [ ] **Backend 100% Verde:** `./backend/mvnw clean test` (Spring Boot + Spring Modulith verification + ArchUnit).
- [ ] **Frontend 100% Verde:** `cd frontend && npm run lint && npm test -- --watch=false` (Vitest y ESLint sin warnings).
- [ ] **Regresión E2E 100% Verde:** `./scripts/run-e2e.sh` (25/25 pruebas en Playwright pasando).
- [ ] **Inventario Actualizado:** Si la tarea agrega un nuevo flujo o altera uno existente, este documento debe reflejar el nuevo caso de prueba y su script asociado.

---

## 2. Matriz de Casos de Prueba Automatizados

| ID Caso | Módulo | Descripción / Flujo | Tipo | Automatizado | Archivo Ejecutable |
|---|---|---|---|---|---|
| **TC-AUTH-001** | Seguridad | Carga inicial, branding y formulario de login | E2E UI | ✅ Sí | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-002** | Seguridad | Alternancia de tema Claro / Oscuro (`ThemeService`) | E2E UI | ✅ Sí | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-003** | Seguridad | Redirección por rol `ADMIN_TENANT` a `/company` | E2E / RBAC | ✅ Sí | `frontend/e2e/auth.spec.ts` |
| **TC-AUTH-004** | Seguridad | Redirección por rol `CASHIER` directo al POS (`/pos`) | E2E / RBAC | ✅ Sí | `frontend/e2e/auth.spec.ts` |
| **TC-POS-001** | Ventas / POS | Indicador de conectividad En Línea y catálogo visible | E2E | ✅ Sí | `frontend/e2e/pos.spec.ts` |
| **TC-POS-002** | Ventas / POS | Adición de plato a comanda y cálculo dinámico de total | E2E | ✅ Sí | `frontend/e2e/pos.spec.ts` |
| **TC-POS-003** | Ventas / POS | Selección de modificadores y extras con recargo (`TASK-210`) | E2E / Pricing | ✅ Sí | `frontend/e2e/pos.spec.ts` |
| **TC-POS-004** | Ventas / POS | Cobro en efectivo con cálculo de cambio y emisión de ticket `T-` | E2E Transaccional | ✅ Sí | `frontend/e2e/pos.spec.ts` |
| **TC-POS-005** | Ventas / POS | Venta en Modo Offline (Dexie.js) y sincronización automática | E2E Offline-First | ✅ Sí | `frontend/e2e/pos.spec.ts` |
| **TC-FIN-001** | Finanzas | Visualización de KPIs ejecutivos de turno y saldo de caja | E2E UI | ✅ Sí | `frontend/e2e/finance.spec.ts` |
| **TC-FIN-002** | Finanzas | Desglose de estado de turno activo y fórmulas de arqueo | E2E | ✅ Sí | `frontend/e2e/finance.spec.ts` |
| **TC-FIN-003** | Finanzas | Registro de nuevo gasto de caja chica y reflejo en tabla | E2E Transaccional | ✅ Sí | `frontend/e2e/finance.spec.ts` |
| **TC-FIN-004** | Finanzas | Modal de arqueo y cálculo de diferencia en tiempo real | E2E | ✅ Sí | `frontend/e2e/finance.spec.ts` |
| **TC-INV-001** | Inventario | KPIs de insumos, valorización y alertas de stock mínimo | E2E UI | ✅ Sí | `frontend/e2e/inventory.spec.ts` |
| **TC-INV-002** | Inventario | Tabla de existencias físicas y stock actual por insumo | E2E | ✅ Sí | `frontend/e2e/inventory.spec.ts` |
| **TC-INV-003** | Inventario | Historial de auditoría en Kardex de movimientos | E2E Audit | ✅ Sí | `frontend/e2e/inventory.spec.ts` |
| **TC-INV-004** | Inventario | Ajuste físico de stock con actualización reactiva | E2E Transaccional | ✅ Sí | `frontend/e2e/inventory.spec.ts` |
| **TC-CAT-001** | Catálogo | Navegación entre pestañas Platos, Insumos y Categorías | E2E UI | ✅ Sí | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-002** | Catálogo | Listado de platos del menú con precios base | E2E | ✅ Sí | `frontend/e2e/catalog.spec.ts` |
| **TC-CAT-003** | Catálogo | Insumos base y categorías asociadas | E2E | ✅ Sí | `frontend/e2e/catalog.spec.ts` |
| **TC-ONB-001** | Onboarding / Tenant | Wizard de 3 pasos, verificación de slug, preset ciudad y alta atómica de empresa | E2E Transaccional | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-TOUR-001** | Onboarding / UX | Product Tour guiado interactivo post-onboarding con navegación y salto | E2E UI | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-CFG-001** | Configuración | Búsqueda reactiva, edición en línea y Hot-Reload de parámetros en caliente | E2E Config | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-RBAC-001** | Seguridad / RBAC | Alta de roles personalizados y matriz de privilegios modulares | E2E RBAC | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-MOB-001** | Ventas / POS Móvil | Viewport smartphone (390px), lista compacta, barra flotante inferior y Bottom Sheet | E2E Touch UX | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-MOB-002** | Inventario Móvil | Ocultamiento de tabla, tarjetas apilables con acordeón expandible y bordes de estado | E2E Touch UX | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-AUTH-005** | Seguridad / Sesiones | Login calificado, emisión de tokens duales y renovación silenciosa (Silent Refresh) | E2E Seguridad | ✅ Sí | `scripts/e2e/run_sprint3_e2e_battery.js` |
| **TC-FLOW-001** | End-to-End | 1. Apertura o verificación de turno con fondo inicial (Cajero) | E2E Jornada | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FLOW-002** | End-to-End | 2. Venta POS multilínea con extras, Happy Hour y ticket | E2E Jornada | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FLOW-003** | End-to-End | 3. Solicitud de gastos en gaveta (Cajero) con estado Pendiente | E2E Jornada | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FLOW-004** | End-to-End | 4. Autorización y aprobación de gastos pendientes (Admin) | E2E Jornada | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FLOW-005** | End-to-End | 5. Arqueo físico de gaveta, cuadre exacto y cierre de jornada | E2E Jornada | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-006** | Ventas / POS | Invariante de turno en backend (HTTP 409 Conflict RFC 7807 SHIFT_REQUIRED_ERROR) | Integración / API | ✅ Sí | `backend/src/test/java/com/restaurant/app/modules/pos/internal/PosIntegrationTest.java` |
| **TC-FIN-005** | Finanzas | Apertura de turno con política SUPERVISOR_ONLY (`finance:shifts_supervisor`) | Integración / RBAC | ✅ Sí | `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` |
| **TC-FIN-006** | Finanzas | Validación de techo máximo de gastos de caja menor (100.00 Bs.) pagados desde gaveta | Integración / Regla | ✅ Sí | `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` |
| **TC-FIN-007** | Finanzas | Política de control dual en autoaprobación de gastos (`finance.expense_allow_self_approval`) | Integración / RBAC | ✅ Sí | `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` |
| **TC-POS-007** | Ventas / POS | Bloqueo reactivo de interfaz POS ante caja cerrada y banner accesible con CTA | Unitario / UI | ✅ Sí | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-POS-008** | Ventas / POS Móvil | Ergonomía silenciosa de Dexie en móviles y despliegue condicional de barra ámbar offline | Unitario / UX Móvil | ✅ Sí | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-POS-009** | Ventas / POS | Trazabilidad de cajero activo en comprobante visual de comanda | Unitario / UI | ✅ Sí | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-UIKIT-001** | Design System / UI Kit | Motor de Temas en Caliente (`ThemeService`): cambio instantáneo de `surfacePreset`, `brandPalette` y colapso de Sidebar (`mini-rail` / `full-hide`) | Unit / Vitest | ✅ Sí | `frontend/src/app/core/services/theme.service.spec.ts` |
| **TC-UIKIT-002** | Design System / UI Kit | Suite de 8 Componentes Standalone (`<app-icon>`, `<app-button>`, `<app-badge>`, `<app-form-field>`, `<app-modal>`, `<app-toast-portal>`) | Unit / Vitest | ✅ Sí | `frontend/src/app/shared/components/ui/ui-kit.spec.ts` |
| **TC-UIKIT-003** | Notificaciones / RFC 7807 | Cola reactiva de Toasts en `z-[9999]`, deduplicación e integración automática con `errorInterceptor` sobre modales activos | Unit / Vitest | ✅ Sí | `frontend/src/app/core/services/notification.service.spec.ts` |
| **TC-POS-010** | Blindaje POS | Bloqueo reactivo de catálogo y carrito sin turno de caja activo | E2E Bloqueo | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-011** | Blindaje POS | Apertura rápida de turno desde modal directo en POS con fondo inicial | E2E Transaccional | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-POS-012** | Ventas / POS | Modalidades de servicio (DINE_IN, TAKEOUT, DELIVERY, SCHEDULED_PICKUP), delivery fee y distintivos en tickets | Unit / Integration | ✅ Sí | `PosServiceTest.java`, `pos.component.spec.ts`, `pos.service.spec.ts`, `cash-control.component.spec.ts` |
| **TC-MENU-001** | Catálogo / Menús | Creación y edición de plantillas de Menú del Día en `/catalog/menus` con tiempos de servicio | Unit & Component | ✅ Sí | `frontend/src/app/features/catalog/menus/daily-menus.component.spec.ts` |
| **TC-MENU-002** | Menús / Apertura | Asignación de Menú Diario al turno de caja con cupos pactados | E2E Transaccional | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-MENU-003** | POS / Menús | Visualización reactiva de Menú Diario en POS, decremento de cupos y badge AGOTADO | E2E Reactivo | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-INV-005** | Inventario / Kardex | Deducción polimórfica en Kardex según estrategia (BOM estricto, unidad de stock o venta libre) | Integración Backend | ✅ Sí | `backend/src/test/java/.../DailyMenuControllerIntegrationTests.java` |
| **TC-FIN-008** | Control de Caja | Panel operativo `/cash-control`: KPIs limpios de gaveta y cálculo en tiempo real | E2E UI | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-009** | Control de Caja | Comandas en vivo cronológicas, filtros ("Mis Ventas", "Todas") y modal de detalle | E2E Auditoría | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-010** | Finanzas / Gastos | Registro de gasto menor por cajero y aprobación por administrador | E2E Workflow | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-011** | Control de Caja | Cuadre exacto de efectivo en gaveta, arqueo y cierre formal de turno | E2E Finanzas | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-FIN-012** | Finanzas / Auditoría | Historial gerencial en `/finance` con auditoría de turnos y consolidación formal | E2E Auditoría | ✅ Sí | `frontend/e2e/shift-lifecycle.spec.ts` |
| **TC-CAT-005** | Catálogo / POS | Catálogo visual con imágenes, recorte nativo WebP (400x300), selector triple de vistas y ergonomía móvil (`TASK-506`) | Unit & Integration | ✅ Sí | `LocalFileStorageServiceTest.java`, `image-cropper-modal.component.spec.ts`, `catalog.component.spec.ts`, `pos.component.spec.ts` |
| **TC-CAT-006** | Catálogo | Asistente de SKU secuencial (BEB-1), selector nativo de emojis gastronómicos y prefijo de categoría (Flyway V15 / `TASK-507`) | Unit & Integration | ✅ Sí | `CatalogIntegrationTest.java`, `emoji-picker.component.spec.ts`, `catalog.component.spec.ts` |
| **TC-UI-001** | UI / Ergonomía | Colapso universal de Sidebar a Mini-Rail (76px) con centrado de iconos y tooltips | Componente / UI | ✅ Sí | `frontend/src/app/core/services/theme.service.spec.ts` |
| **TC-UI-002** | UI / Ergonomía | Supresión de subtítulos estáticos en móvil ($< 640\text{px}$) liberando $\ge 35\text{px}$ | Componente / UI | ✅ Sí | `frontend/src/app/shared/components/ui/ui-kit.spec.ts` |
| **TC-UI-003** | UI / Ergonomía | Menú contextual táctil `[···]` en cabecera de POS con apertura y acciones rápidas | Componente / UI | ✅ Sí | `frontend/src/app/features/pos/pos.component.spec.ts` |
| **TC-UI-004** | UI / Tipografía | Estandarización numérica monospace (`font-mono text-right`) y tap targets $\ge 44\times 44\text{px}$ | Componente / UI | ✅ Sí | `frontend/src/app/shared/components/ui/ui-kit.spec.ts` |
| **TC-AUTH-006** | Seguridad / Perfil | Perfil de usuario autoservicio, humanización de personal, nombres reales y gestión de PIN (Flyway V16 / `TASK-509`) | Unit & Integration | ✅ Sí | `UserIntegrationTests.java`, `profile.component.spec.ts`, `users.component.spec.ts` |
| **TC-CFG-002** | Configuración | Catálogo JSON desacoplado, consola humanizada, búsqueda multicriterio y ayuda contextual (ADR-0025 / `TASK-510`) | Unit & Integration | ✅ Sí | `SystemParameterCatalogTest.java`, `context-help.component.spec.ts`, `parameters.component.spec.ts` |

---

## 3. Detalle de Casos Críticos de Flujo de Negocio

### Caso TC-FLOW-001..005: Ciclo de Vida de Turno y Operación Integral
- **Objetivo:** Asegurar que el ciclo comercial y financiero diario funcione de inicio a fin entre diferentes roles de usuarios sin errores ni discrepancias de gaveta.
- **Precondiciones:**
  - Base de datos sembrada con datos de demostración (`demo-restaurant`, sucursal Central).
  - Usuarios disponibles: `cajero1` (rol `CASHIER`) y `admin` (rol `ADMIN_TENANT`).
- **Pasos de Ejecución:**
  1. *Cajero:* Ingresa al sistema. Si no hay turno abierto, abre turno con Bs. 200.00.
  2. *Cajero:* Ingresa a `/pos`. Agrega al carrito un Combo Burger con Happy Hour (-15%) y una Hamburguesa Clásica con extras de Queso Cheddar (+Bs. 5.00). Procesa cobro exacto en efectivo. Verifica emisión de comprobante `T-XXXX`.
  3. *Cajero:* Ingresa a `/finance` -> Gastos. Registra solicitud de egreso de caja menor con marca de gaveta (`paidFromCashDrawer = true`). Verifica que queda en `PENDING_APPROVAL` y que el cajero **no** tiene botón para autorizar su propio gasto.
  4. *Admin:* Cierra sesión de cajero e ingresa como Administrador. Va a Finanzas y aprueba los gastos en estado pendiente.
  5. *Admin:* Abre el modal de Arqueo y Cierre. Compara el Efectivo Teórico Esperado ($E_{esperado} = F_{inicial} + V_{efectivo} - G_{aprobados}$) e ingresa el conteo físico real exacto. Confirma cierre de turno.
  6. *Verificación Final:* El sistema marca el turno como `CLOSED` y la interfaz queda en estado inicial lista para una nueva jornada (`btn-open-shift-cta`).

### TC-POS-006: Invariante Transaccional de Turno en Backend (HTTP 409 RFC 7807)
- **Módulo:** Ventas / POS (`modules.pos`)
- **Historia / Tarea:** TASK-402 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER / ADMIN_TENANT
- **Precondiciones:**
  - Sucursal registrada en el sistema sin turno activo de caja (`cash_shifts` abierto).
- **Pasos:**
  1. Enviar petición `POST /api/v1/pos/orders` autenticada con token JWT.
  2. Enviar payload de comanda válido con ítems de menú y método de pago en efectivo.
- **Resultado Esperado:**
  - Retorno HTTP 409 Conflict con estructura RFC 7807 (`type: "https://api.restaurant.com/errors/shift-required"`, `title: "Turno de Caja Requerido"`, `status: 409`, `detail: "No es posible registrar ventas: la caja de la sucursal se encuentra cerrada."`).
  - Cero órdenes o cobros persistidos en base de datos.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/pos/internal/PosIntegrationTest.java` (método `shouldReturn409WhenNoActiveShiftIsOpen`)

### TC-FIN-005: Control de Apertura de Turno con Política SUPERVISOR_ONLY
- **Módulo:** Finanzas (`modules.finance`)
- **Historia / Tarea:** TASK-402 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER / SUPERVISOR / ADMIN_TENANT
- **Precondiciones:**
  - Parámetro de sucursal `finance.shift_opening_policy` configurado en `SUPERVISOR_ONLY`.
- **Pasos:**
  1. Un usuario con rol regular `CASHIER` intenta abrir turno invocando `CashShiftService.openShift(...)`.
  2. Un usuario con privilegio `finance:shifts_supervisor` intenta abrir turno con fondo inicial.
- **Resultado Esperado:**
  1. Intento del cajero genera `UnauthorizedOperationException` (HTTP 403) con mensaje descriptivo ("Caja Cerrada — Esperando apertura de turno por parte de un supervisor").
  2. Intento del supervisor abre la caja exitosamente en estado `OPEN` con fondo inicial registrado.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (método `shouldEnforceShiftOpeningPolicyWhenSupervisorOnly`)

### TC-FIN-006: Techo Máximo para Gastos Pagados desde Gaveta de Caja Chica
- **Módulo:** Finanzas (`modules.finance`)
- **Historia / Tarea:** TASK-402 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER / BRANCH_MANAGER
- **Precondiciones:**
  - Turno de caja en estado `OPEN` y parámetro `finance.max_petty_cash_expense` fijado en 100.00 Bs.
- **Pasos:**
  1. Registrar solicitud de egreso menor por Bs. 150.00 con marca `paidFromCashDrawer = true`.
  2. Registrar solicitud de egreso por Bs. 150.00 con marca `paidFromCashDrawer = false` (pago electrónico/bancario).
- **Resultado Esperado:**
  1. Solicitud pagada de gaveta es rechazada con `ValidationException` (HTTP 400) por superar el techo de caja menor permitido.
  2. Solicitud que no compromete efectivo físico de gaveta se registra exitosamente.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (método `shouldRejectPettyCashExpenseExceedingLimitWhenPaidFromDrawer`)

### TC-FIN-007: Política de Control Dual en Autoaprobación de Gastos
- **Módulo:** Finanzas (`modules.finance`)
- **Historia / Tarea:** TASK-402 / PB-041 / ADR-0013
- **Roles Involucrados:** BRANCH_MANAGER / ADMIN_TENANT
- **Precondiciones:**
  - Parámetro del sistema `finance.expense_allow_self_approval` en `false` (default dual-control).
- **Pasos:**
  1. Encargado de sucursal con permiso `finance:expenses_approve` genera solicitud de gasto propia.
  2. Administrador general (`ADMIN_TENANT`) genera solicitud de gasto propia.
- **Resultado Esperado:**
  1. Solicitud del encargado permanece en `PENDING_APPROVAL` (requiere aprobación cruzada o de admin).
  2. Solicitud de `ADMIN_TENANT` se aprueba de forma automática (`APPROVED`) por jerarquía de gobierno.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java` (método `shouldEnforceExpenseSelfApprovalPolicy`)

### TC-POS-007: Bloqueo Reactivo de Interfaz POS ante Caja Cerrada y Banner con CTA
- **Módulo:** Ventas / POS (`frontend/src/app/features/pos`)
- **Historia / Tarea:** TASK-403 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER / BRANCH_MANAGER
- **Precondiciones:**
  - `FinanceService.hasOpenShift()` retorna `false` para la sucursal activa seleccionada.
- **Pasos:**
  1. Acceder a la interfaz del punto de venta (`/pos`).
  2. Verificar visualización de banner accesible `data-testid="closed-shift-banner"`.
  3. Verificar estado inactivo/deshabilitado de catálogo de platos, combos y botones de checkout.
  4. Intentar ejecutar mutaciones programáticas (`addToCart`, `onComboClicked`, `confirmCheckout`).
  5. En caso de usuario facultado, hacer clic en botón `[Abrir Turno]` (`data-testid="open-shift-cta-btn"`), ingresar fondo en modal y confirmar.
- **Resultado Esperado:**
  - Botones deshabilitados en DOM, métodos TypeScript interceptados con toasts informativos sin mutación de carrito, y modal permite apertura reactiva directa actualizando el estado a turno abierto.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts` (suite `Blindaje Reactivo de Turno de Caja (TASK-403 / PB-041 / ADR-0013)`)

### TC-POS-008: Ergonomía Silenciosa en Móviles y Barra Ámbar Condicional Offline
- **Módulo:** Ventas / POS Móvil (`frontend/src/app/features/pos`)
- **Historia / Tarea:** TASK-403 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER
- **Precondiciones:**
  - Terminal de punto de venta corriendo en viewport móvil o desktop.
- **Pasos:**
  1. Operación normal con conexión activa y cero órdenes pendientes.
  2. Desconexión de red (`isOnline = false`).
  3. Reconexión con órdenes pendientes por sincronizar en Dexie.js (`pendingCount > 0`).
- **Resultado Esperado:**
  1. Header limpio sin badges ruidosos de estado ("En línea" suprimido).
  2 y 3. Despliegue de barra ámbar superior `data-testid="offline-warning-bar"` con mensaje no intrusivo y botón de sincronización `data-testid="sync-pending-btn"`.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts` (suite `Ergonomía Móvil y Barra de Advertencia Offline (ADR-0013)`)

### TC-POS-009: Trazabilidad de Cajero Activo en Comprobante Visual de Comanda
- **Módulo:** Ventas / POS (`frontend/src/app/features/pos`)
- **Historia / Tarea:** TASK-403 / PB-041 / ADR-0013
- **Roles Involucrados:** CASHIER
- **Precondiciones:**
  - Cajero con sesión activa en `AuthService` y orden procesada con éxito.
- **Pasos:**
  1. Culminar cobro y desplegar modal de ticket térmico/comanda (`data-testid="ticket-modal"`).
  2. Verificar elemento `data-testid="ticket-cashier-name"`.
- **Resultado Esperado:**
  - El ticket visual expone claramente el nombre del colaborador o usuario en sesión activa (`CAJERO: Carlos Cajero`), garantizando trazabilidad operacional física.
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts` (suite `Ticket Visual de Comanda con Identificación de Cajero (ADR-0013)`)

---

### TC-MENU-001: Creación de Plantilla de Menú Diario Reutilizable (PB-034)
- **Módulo:** Catálogo (`modules.catalog`)
- **Historia / Tarea:** TASK-407 / TASK-408 / PB-034 / ADR-0015
- **Roles Involucrados:** `ADMIN_TENANT`, `BRANCH_MANAGER`
- **Precondiciones:** Platos maestros activos en el catálogo.
- **Pasos:**
  1. Enviar `POST /api/v1/catalog/menus` con nombre "Lunes Ejecutivo", modalidad `INDIVIDUAL_ITEMS` y platos para `STARTER`, `MAIN`, `DESSERT`.
  2. Consultar `GET /api/v1/catalog/menus?templateOnly=true`.
- **Resultado Esperado:**
  - Código HTTP `201 Created` y listado de plantillas incluye el menú con cupos sugeridos y clasificación de tiempos.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/catalog/internal/DailyMenuServiceTest.java` y `frontend/src/app/features/catalog/menus/daily-menus.component.spec.ts`

---

### TC-MENU-002: Instanciación de Menú al Turno con Cupos Iniciales Personalizados (PB-034)
- **Módulo:** Finanzas & Turnos / Catálogo (`modules.finance`, `modules.catalog`)
- **Historia / Tarea:** TASK-407 / TASK-408 / PB-034
- **Roles Involucrados:** `CASHIER`, `BRANCH_MANAGER`
- **Precondiciones:** Plantilla de menú existente en tenant.
- **Pasos:**
  1. Abrir turno enviando `POST /api/v1/finance/shifts/open` con `menuTemplateId` y mapa de cupos ajustados (`customQuotas: {Milanesa: 25}`).
  2. Consultar `GET /api/v1/catalog/shifts/{shiftId}/menu`.
- **Resultado Esperado:**
  - Se crea `shift_menu_assignments` y `shift_menu_items` con `initial_quota = 25`, `current_quota = 25`, `sold_count = 0`, `is_sold_out = false`. La plantilla base no sufre mutaciones.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/catalog/internal/ShiftMenuInstantiationTest.java` y `frontend/src/app/features/pos/pos.component.spec.ts`

---

### TC-MENU-003: Decremento Atómico Concurrente y Bloqueo de Agotado en POS (PB-034)
- **Módulo:** Punto de Venta (`modules.pos`)
- **Historia / Tarea:** TASK-407 / TASK-408 / PB-034 / ADR-0015
- **Roles Involucrados:** `CASHIER`
- **Precondiciones:** Turno abierto con plato con cupo `current_quota = 1`.
- **Pasos:**
  1. Ejecutar cobro de comanda solicitando 1 ración del plato.
  2. Verificar decremento atómico y cambio de estado a `is_sold_out = true`.
  3. Intentar cobrar una segunda comanda inmediatamente para el mismo plato.
- **Resultado Esperado:**
  - La primera orden se procesa con éxito (`201 Created`).
  - La segunda orden es rechazada con HTTP `409 Conflict` (`ITEM_SOLD_OUT`), previniendo sobreventas físicas.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/pos/AtomicQuotaEnforcementTest.java` y `frontend/src/app/features/pos/pos.component.spec.ts`

---

### TC-MENU-004: Ampliación Supervisada de Cupos en Vivo con Trazabilidad (PB-034)
- **Módulo:** Catálogo / POS (`modules.catalog`)
- **Historia / Tarea:** TASK-407 / TASK-408 / PB-034
- **Roles Involucrados:** `BRANCH_MANAGER`, `ADMIN_TENANT`
- **Precondiciones:** Plato en turno marcado como agotado (`current_quota = 0`, `is_sold_out = true`).
- **Pasos:**
  1. Enviar `POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota` con `quantityDelta = 10` y motivo "Tanda horneada extra".
- **Resultado Esperado:**
  - `current_quota` se incrementa a 10, `is_sold_out` pasa a `false`.
  - Se inserta registro inmutable en `shift_menu_quota_adjustments` con el ID del supervisor.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/catalog/internal/QuotaAdjustmentTest.java`

---

### TC-INV-005: Deducción Polimórfica según Estrategia de Inventario (PB-044)
- **Módulo:** Inventario & Kardex (`modules.inventory`)
- **Historia / Tarea:** TASK-407 / TASK-408 / PB-044 / ADR-0015
- **Roles Involucrados:** Sistema / `InventoryEventListener`
- **Precondiciones:** Tres platos configurados con: `RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT` y `COMMERCIAL_NO_INVENTORY`.
- **Pasos:**
  1. Despachar `OrderPaidEvent` que contiene 1 unidad de cada plato.
- **Resultado Esperado:**
  - Para `RECIPE_BOM_STRICT`: Se deducen insumos de receta en `branch_stock` y se inserta `kardex_movements` con `ingredient_id != null`.
  - Para `DISH_AS_INVENTORY_UNIT`: Se descuenta el plato terminado en `branch_stock` (`dish_id != null`) y se asienta en Kardex.
  - Para `COMMERCIAL_NO_INVENTORY`: No se efectúa ningún movimiento ni afectación de saldo.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/inventory/internal/InventoryStrategyFactoryTest.java`

---

### TC-MOD-001: Venta en POS con Modificadores Físicos y Deducción SALE_CONSUMPTION en Kardex (PB-020)
- **Módulo:** Punto de Venta (`modules.pos`) e Inventario (`modules.inventory`)
- **Historia / Tarea:** TASK-501 / PB-020 / ADR-0018
- **Roles Involucrados:** `CASHIER`, Sistema / `KardexService`
- **Precondiciones:** Plato con grupo de modificadores asignado; insumo asociado con stock registrado en `branch_stock`.
- **Pasos:**
  1. Registrar venta en POS (`posPublicApi.placeOrder`) con 2 unidades del plato y modificador extra con ingrediente y cantidad consumida.
  2. Verificar que `order_items` persista `notes` y `order_item_modifiers` almacene los modificadores de forma inmutable.
  3. Comprobar la ejecución asíncrona de `OrderPaidEvent` en `KardexService`.
  4. Realizar venta con modificador comercial (`ingredientId == null`).
- **Resultado Esperado:**
  - El stock físico del insumo extra se reduce en `quantityConsumed * item.quantity`.
  - Se inserta movimiento inmutable en `kardex_movements` con `movement_type = 'SALE_CONSUMPTION'`.
  - Modificadores comerciales no descuentan stock ni generan movimiento en Kardex.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/catalog/ModifierIntegrationTests.java`

---

### TC-MOD-002: Selector Interactivo de Modificadores, Notas de Cocina y Líneas de Carrito Separadas (PB-020)
- **Módulo:** Punto de Venta UI (`features.pos`)
- **Historia / Tarea:** TASK-502 / PB-020 / ADR-0018
- **Roles Involucrados:** `CASHIER`
- **Precondiciones:** Plato con grupos de modificadores configurados (`minSelectable` y `maxSelectable`).
- **Pasos:**
  1. Pulsar sobre el plato en el catálogo; verificar apertura automática del modal `[data-testid="modifier-selection-modal"]`.
  2. Ingresar nota culinaria en `[data-testid="item-kitchen-notes-input"]` y seleccionar modificadores.
  3. Pulsar `[data-testid="confirm-modifiers-btn"]` y verificar llamada a `posService.addToCart`.
  4. Agregar el mismo plato con distintas notas o modificadores.
  5. Ejecutar cobro offline en Dexie.js y verificar persistencia en `PendingOrderEntity` con `notes` y `selectedModifiers`.
- **Resultado Esperado:**
  - El modal valida reglas de selección de grupo en tiempo real.
  - Ítems con distintas notas o extras se mantienen como líneas separadas en el carrito lateral y comanda.
  - El carrito y ticket térmico renderizan modificadores con su importe y la nota en cursiva atenuada.
  - Modo offline preserva fielmente los modificadores y notas en Dexie.js (`local_orders`).
- **Automatizado en:** `frontend/src/app/features/pos/pos.component.spec.ts` y `frontend/src/app/core/services/pos.service.spec.ts`

---

### TC-POS-008: Cancelación de Pedidos, Reintegro de Gaveta y Tratamiento de Mermas (PB-031)
- **Módulo:** POS / Finanzas / Inventario / Seguridad
- **Historia / Tarea:** TASK-504 / PB-031 / ADR-0019
- **Roles Involucrados:** `CASHIER`, `SUPERVISOR`, `ADMIN_TENANT`
- **Precondiciones:** Orden en estado `PAID`, turno de caja `OPEN`, supervisor con PIN numérico configurado.
- **Pasos:**
  1. Invocar endpoint `POST /api/v1/pos/orders/{orderId}/cancel` con motivo tipado, método de reintegro (`CASH`) y `wasPrepared = false`.
  2. Proveer PIN numérico válido de supervisor in-situ.
  3. Verificar anulación atómica de la orden (`CANCELLED`) y publicación de `OrderCancelledEvent`.
  4. Constatar incremento de `total_cash_refunds` en el turno activo y deducción del efectivo esperado en gaveta.
  5. Comprobar reversión de stock en Kardex (`CANCELLATION_REVERSAL`) al no haberse preparado.
  6. Repetir cancelación con `wasPrepared = true` y verificar destino en buffer de vitrina (`prepared_dish_buffer`) o merma (`WASTE_ADJUSTMENT`).
- **Resultado Esperado:**
  - Orden cancelada inmutablemente; PIN erróneo o ausente es rechazado con 400/403.
  - El cuadre de gaveta resta las devoluciones de efectivo exactas (`Expected = Initial + Sales - Expenses - Refunds`).
  - Kardex refleja el destino de insumos según estado de elaboración y política de merma.
- **Automatizado en:** `backend/src/test/java/com/restaurant/app/modules/pos/OrderCancellationIntegrationTests.java` y `KardexServiceTest.java`

---

### TC-POS-009: Componente Presentacional de Cola de Autorizaciones y UI de Cancelación (PB-031)
- **Módulo:** Finanzas / POS UI (`shared.ui.authorization-queue`, `features.cash-control`, `features.finance`, `features.profile`)
- **Historia / Tarea:** TASK-504 / PB-031 / ADR-0019
- **Roles Involucrados:** `CASHIER`, `SUPERVISOR`, `ADMIN_TENANT`
- **Precondiciones:** Sesión iniciada con turno activo.
- **Pasos:**
  1. Configurar PIN numérico de 4-6 dígitos en `/profile`.
  2. En `/cash-control`, abrir detalle de comanda pagada y pulsar `[Cancelar Pedido]`.
  3. Ingresar motivo, reintegro y probar teclado táctil de PIN numérico in-situ.
  4. Probar envío alternativo a cola de autorización remota.
  5. En `/finance`, verificar pestaña de autorizaciones con `<app-authorization-queue [viewMode]="'SUPERVISOR'">`, aprobar y rechazar solicitudes.
  6. En `/cash-control`, verificar visualización de estado en `<app-authorization-queue [viewMode]="'CASHIER'">`.
- **Resultado Esperado:**
  - Componente `<app-authorization-queue>` opera como dumb component puro sin dependencias de red.
  - La anulación in-situ actualiza inmediatamente los KPIs de comanda y gaveta.
  - Las autorizaciones remotas permiten aprobación/rechazo asíncrono con notas de decisión.
- **Automatizado en:**
  - `frontend/src/app/shared/components/ui/authorization-queue/authorization-queue.component.spec.ts`
  - `frontend/src/app/features/cash-control/cash-control.component.spec.ts`
  - `frontend/src/app/features/finance/finance.component.spec.ts`
  - `frontend/src/app/features/profile/profile.component.spec.ts`

---

### TC-POS-012: Modalidades de Servicio, Delivery Fee, Validaciones y Distintivos en Tickets (PB-032)
- **Módulo:** POS / Configuración / Finanzas / UI
- **Historia / Tarea:** TASK-505 / PB-032 / ADR-0020
- **Roles Involucrados:** `CASHIER`, `SUPERVISOR`, `ADMIN_TENANT`
- **Precondiciones:** Turno de caja abierto, catálogo con productos activos.
- **Pasos:**
  1. Seleccionar modalidad en la barra superior del POS (`pos-topbar-modality-selector`) alternando entre `DINE_IN`, `TAKEOUT`, `SCHEDULED_PICKUP` y `DELIVERY`.
  2. Verificar que al elegir `DELIVERY`, el formulario de checkout exija dirección de entrega obligatoria y permita definir costo de envío (`deliveryFee`).
  3. Comprobar que el total a cobrar sume el subtotal de ítems más el `deliveryFee` ($\text{totalAmount} = \sum \text{items} + \text{deliveryFee}$).
  4. Procesar el pago y constatar que el ticket modal renderice el banner prominente correspondiente (`*** PARA LLEVAR ***`, `*** ENCARGO PARA RECOGER ***`, `*** DELIVERY / A DOMICILIO ***` o `CONSUMO EN SALÓN`), junto con los datos de contacto, dirección y costo de envío.
  5. En `/cash-control`, verificar el filtrado por modalidad de servicio en la tabla de comandas y los badges distintivos correspondientes.
  6. En modo offline (Dexie.js), comprobar que `PendingOrderEntity` y el ticket local preserven fielmente la modalidad, datos de entrega y el `deliveryFee`.
- **Resultado Esperado:**
  - Si `orderType == 'DELIVERY'` y la dirección está vacía, se bloquea el cobro con advertencia.
  - La migración Flyway V13 asegura la integridad referencial y las columnas en base de datos.
  - El visor de comandas `/cash-control` y el detalle de ticket reflejan con precisión la modalidad, teléfono, dirección y desglose de envío.
- **Automatizado en:**
  - Backend: `backend/src/test/java/com/restaurant/app/modules/pos/internal/PosServiceTest.java`
  - Frontend: `frontend/src/app/features/pos/pos.component.spec.ts`, `frontend/src/app/core/services/pos.service.spec.ts`, `frontend/src/app/features/cash-control/cash-control.component.spec.ts`

---

### TC-CAT-005: Catálogo Visual con Imágenes, Recorte Nativo WebP, Selector Triple y Ergonomía POS (PB-036 / TASK-506)
- **Módulo:** Catalog / POS
- **Historia / Tarea:** TASK-506 / PB-036 / ADR-0021
- **Roles Involucrados:** ADMIN_TENANT / CASHIER
- **Precondiciones:**
  - Base de datos con migración `V14__dish_image_url.sql` aplicada.
  - Almacenamiento local desacoplado configurado en `application.yml` (`./uploads/dishes/`).
- **Pasos:**
  1. Subir imagen cruda (JPG/PNG/WebP) desde `/catalog` o al crear un nuevo plato.
  2. Ajustar encuadre y zoom en `<app-image-cropper-modal>` (HTML5 Canvas nativo, 4:3 WebP 400x300 al 80% ~30 KB).
  3. Confirmar subida vía `POST /api/v1/catalog/dishes/{id}/image` con validación estricta de tamaño $\le 1\text{ MB}$ (HTTP 413 RFC 7807).
  4. En POS, verificar la cuadrícula en modo Galería (`grid`): fotos cargadas en tarjetas homogéneas `h-28/h-32` y fallback a degradado con emoji temático ante platos sin foto o error `(error)="onImgError()"`.
  5. Conmutar entre modos Galería (`grid`), Compacto (`compact`) y Lista (`list`), verificando persistencia en `localStorage`.
  6. Verificar ergonomía móvil: título oculto en móvil, selector compacto de modalidades con icono de Mesa (`🪑`), y 4 botones de pago en checkout alineados en 1 sola fila.
- **Resultado Esperado:**
  - Imágenes optimizadas WebP servidas estáticamente con `Cache-Control: 365 días`.
  - Integración offline con Dexie.js (`cachedProducts.imageUrl`) sirviendo bytes desde el Disk Cache del navegador sin conexión.
  - 100% tests unitarios e integrados pasando sin errores.
- **Automatizado en:**
  - Backend: `backend/src/test/java/com/restaurant/app/modules/catalog/internal/LocalFileStorageServiceTest.java`, `backend/src/test/java/com/restaurant/app/modules/catalog/internal/CatalogIntegrationTest.java`
  - Frontend: `frontend/src/app/shared/components/ui/image-cropper-modal/image-cropper-modal.component.spec.ts`, `frontend/src/app/features/catalog/catalog.component.spec.ts`, `frontend/src/app/features/pos/pos.component.spec.ts`

### TC-CAT-006: Asistente de SKU Secuencial, Selector de Emojis Gastronómicos y Prefijo de Categoría (PB-048 / TASK-507)
- **Módulo:** Catalog
- **Historia / Tarea:** TASK-507 / PB-048 / ADR-0022
- **Roles Involucrados:** ADMIN_TENANT
- **Precondiciones:**
  - Base de datos con migración `V15__category_code_prefix.sql` aplicada (`code_prefix VARCHAR(10) NOT NULL` con restricción única por tenant).
- **Pasos:**
  1. Al abrir el modal de nueva categoría en `/catalog`, verificar autocalculo de `sortOrder = max(sortOrders) + 1` (default 1).
  2. Escribir nombre de categoría (ej. "Bebidas Calientes") y verificar sugerencia reactiva de `codePrefix` ("BEB") y validación de conflicto en caso de prefijo duplicado en el tenant (HTTP 409 Conflict).
  3. Seleccionar icono con `<app-emoji-picker>` (popover nativo ligero $< 2\text{ KB}$ con catálogo gastronómico curado y buscador) y verificar asignación inmediata del emoji.
  4. Al abrir modal de creación de plato, verificar invocación de `GET /api/v1/catalog/dishes/next-code?categoryId={id}` y precarga de SKU correlativo (`MAX + 1`) en formato simple `${prefix}-${n}` (ej. `BEB-1`).
  5. Verificar que códigos manuales o EAN-13 ingresados por el usuario no se sobreescriben automáticamente al cambiar otros campos, y validar botón `[🪄 Sugerir Código]`.
- **Resultado Esperado:**
  - Unicidad de prefijos garantizada por tenant.
  - Códigos de platos inmutables y correlativos limpios sin ceros a la izquierda.
  - 100% tests unitarios e integrados pasando sin errores en backend y frontend.
- **Automatizado en:**
  - Backend: `backend/src/test/java/com/restaurant/app/modules/catalog/internal/CatalogIntegrationTest.java`
  - Frontend: `frontend/src/app/shared/components/ui/emoji-picker/emoji-picker.component.spec.ts`, `frontend/src/app/features/catalog/catalog.component.spec.ts`

---

### TC-FIN-013: Histórico Gerencial de Turnos, Filtros Multicriterio, Auditoría Drill-Down y Exportación CSV (PB-030 / TASK-508)
- **Módulo:** Finanzas / Auditoría (`modules.finance`, `frontend/src/app/features/finance`)
- **Historia / Tarea:** TASK-508 / PB-030 / ADR-0023
- **Roles Involucrados:** `ADMIN_TENANT`, `BRANCH_MANAGER`
- **Precondiciones:**
  - Turnos cerrados y consolidados en la base de datos con ventas en efectivo, tarjeta y QR, egresos de caja menor y cancelaciones con reintegro.
- **Pasos:**
  1. En `/finance`, seleccionar rango de fechas usando Presets Rápidos (*Hoy*, *Ayer*, *Últimos 7 días*, *Este Mes*) o inputs de calendario.
  2. Filtrar turnos por Cajero (`openedBy`), Estado (`OPEN`, `CLOSED`, `CONSOLIDATED`) y Tipo de Descuadre (`CUADRADO`, `SOBRANTE`, `FALTANTE`).
  3. Comprobar la conversión estricta al huso horario de la sucursal (`branches.timezone`, default `'America/La_Paz'`) según ADR-0006.
  4. Presionar el botón "Exportar Lista (CSV)" y verificar la descarga de `historico_turnos_YYYY-MM-DD.csv` con prefijo BOM UTF-8 (`\uFEFF`) y delimitador `;`.
  5. En una fila de turno, presionar el botón "Ver Detalle" `[👁️]` para abrir el modal drill-down `<app-shift-detail-modal>`.
  6. Navegar entre las pestañas del modal:
     - *Balance y Resumen*: arqueo ciego, desglose de ventas por método de pago, gastos y reintegros de efectivo.
     - *Comandas / Ventas*: tabla de órdenes auditadas con número de ticket, fecha/hora, cliente, medio de pago, monto y estado.
     - *Egresos y Reintegros*: lista de gastos de caja menor y cancelaciones con devolución autorizadas.
  7. En el modal, hacer clic en "Descargar Detalle (CSV)" y verificar la generación y descarga del reporte detallado.
- **Resultado Esperado:**
  - Backend ejecuta consultas JPQL indexadas y `JdbcClient` sin ciclos modulares en Spring Modulith.
  - La utilidad nativa TypeScript `exportToCsv` genera archivos compatibles con Excel y LibreOffice con 0 dependencias npm externas.
  - 100% de tests unitarios y de integración pasando sin errores en Maven y Vitest.
- **Automatizado en:**
  - Backend: `backend/src/test/java/com/restaurant/app/modules/finance/internal/CashShiftServiceTest.java`, `backend/src/test/java/com/restaurant/app/RestaurantModularityTests.java`
  - Frontend: `frontend/src/app/features/finance/finance.component.spec.ts`, `frontend/src/app/features/finance/components/shift-detail-modal/shift-detail-modal.component.spec.ts`, `frontend/src/app/shared/utils/csv-exporter.spec.ts`

---

## 4. Backlog de Pruebas Futuras (Nuevas Funcionalidades)

Conforme se implementen los siguientes bloques y tareas del `product_backlog.md`, se deberán redactar y automatizar los siguientes casos:

| ID Futuro | Iniciativa / Tarea | Alcance a Cubrir | Prioridad |
|---|---|---|---|
| **TC-FUT-001** | Delivery & Llevar (Sprint 3) | Gestión de pedidos para llevar, datos del cliente y despacho | P1 |
| **TC-FUT-002** | Facturación Electrónica (Sprint 3) | Emisión de factura con NIT/CI y código QR fiscal | P1 |
| **TC-FUT-003** | Mermas y Desperdicios (Sprint 3) | Registro de mermas de insumos con deducción directa en Kardex | P2 |
| **TC-FUT-004** | Transferencias entre Sucursales | Envío y recepción de insumos entre almacenes de diferentes sucursales | P2 |
| **TC-FUT-005** | Arqueo Ciego (Blind Count) | Modalidad donde el cajero no ve el saldo teórico antes de contar | P2 |

---

## 5. Plantilla Estándar para Nuevas Pruebas

Al agregar un nuevo caso a este inventario, utilice la siguiente estructura:

```markdown
### [ID-CASO]: [Título Descriptivo del Flujo]
- **Módulo:** (Catalog / POS / Inventory / Finance / Security)
- **Historia / Tarea:** (ej. TASK-211 / PB-021)
- **Roles Involucrados:** (CASHIER / ADMIN_TENANT / etc.)
- **Precondiciones:**
  - Estado inicial requerido de BD, turno o inventario.
- **Pasos:**
  1. Acción 1
  2. Acción 2
- **Resultado Esperado:**
  - Criterio de aceptación verificable con aserción Playwright.
- **Automatizado en:** `frontend/e2e/<nombre>.spec.ts`
```
