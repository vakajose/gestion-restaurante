---
title: "Sprint 4 Planning: Fundamentos, Blindaje de Caja y Menús Diarios"
category: "sprints"
sprint: 4
status: "planned"
last_updated: "2026-10-29"
---

# 📅 Sprint 4 Planning — Fundamentos, Blindaje de Caja y Menús Diarios (Planificado)

> **Estado del Sprint:** `PLANIFICADO / LISTO PARA INICIO TRAS RETROSPECTIVA SPRINT 3`  
> **Objetivo del Sprint:** Blindar la integridad financiera del Punto de Venta (invariante de venta con turno abierto obligatorio), construir el Design System base con suite de componentes web reutilizables de Angular y notificaciones en portal superior, implementar la gestión de Menús Diarios con plantillas y cupos, habilitar recetas flexibles (con o sin Kardex), y certificar el ciclo de caja continuo con una suite E2E integral.

---

## 🎯 Secuencia Estratégica de Ejecución (SDD-SDLC)

Siguiendo el acuerdo de la retrospectiva, el Sprint 4 se ejecuta en 3 fases secuenciales:
1. **Fase Inmediata (Hotfix & Blindaje Crítico):** Subsanar `PB-041` (Invariante de Turno en POS) en backend y frontend para cerrar la fuga financiera.
2. **Fase Cimiento UI/UX (Design System & Feedback):** Construir `PB-042` (Tokens, `<app-page-wrapper>`, `<app-button>`, `<app-badge>`, z-index) y `PB-043` (Motor de Notificaciones) para que las vistas subsiguientes nazcan estandarizadas.
3. **Fase Negocio & Operación Diaria:** Implementar `PB-034` (Menús Diarios y Cupos), `PB-044` (Flexibilidad de Recetas BOM), `PB-029` (Ventas del Turno para Cajero), `PB-028` (Ergonomía Móvil y Sidebar Colapsable) y certificar con `PB-046` (Batería E2E Crítica).

---

## 📋 Tareas Planificadas del Sprint 4

### 1. Blindaje Financiero del Punto de Venta (PB-041 - Defecto Crítico)

- [x] **TASK-401 [POS] [DISEÑO]:** Especificación Técnica del Invariante de Turno en POS, Políticas de Apertura y Gastos RBAC (PB-041 / ADR-0013).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* Diseñar y formalizar la regla inviolable: "Toda orden o cobro en POS exige un `shift_id` con estado `OPEN`". Contrato de error RFC 7807 (`SHIFT_REQUIRED_ERROR` / HTTP 409), trazabilidad relacional `orders.user_id`, parámetros `finance.shift_opening_policy` y `finance.expense_allow_self_approval`, y ergonomía silenciosa móvil.
  - *Criterio de Entrega:* Documentos certificados en `ADR-0013` y `sequence_pos_order_shift_validation.md`.

- [x] **TASK-402 [POS] [SEGURIDAD]:** Implementación de Invariante de Turno en Backend, Políticas de Apertura y Gastos RBAC (PB-041).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* En `OrderServiceImpl`, inyectar `FinancePublicApi`, validar turno activo y retornar HTTP 409 si la caja está cerrada; asociar `cashShiftId` y `user_id`; proteger `FinanceController` con `@PreAuthorize`.
  - *Criterio de Entrega:* 100% de pruebas en Spring Modulith (`OrderIntegrationTests`, `CashShiftIntegrationTests`).

- [x] **TASK-403 [POS] [UI/UX]:** Bloqueo Reactivo de POS con Caja Cerrada, Integración con FinanceService y Modo Offline Móvil (PB-041).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* En frontend, bloquear comanda y catálogo si el turno no está abierto; mostrar banner explicativo con botón directo a "Abrir Turno con Fondo Inicial"; eliminar tags ruidosos.
  - *Criterio de Entrega:* Es imposible interactuar con el carrito si no hay sesión abierta; al abrir turno, el POS se desbloquea reactivamente.

---

### 2. Design System de Restaurante y Suite de Componentes Standalone (PB-042 / PB-043)

- [x] **TASK-404 [UI/UX] [DISEÑO]:** Especificación de Tokens de Color Semánticos, Motor de Temas en Caliente, Layout Wrapper y Suite UI Kit (PB-042 / ADR-0014).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Definir tokens semánticos en Tailwind con CSS Custom Properties en caliente, contrastes WCAG AA para Dark/Light mode y contratos de los 8 componentes base.
  - *Criterio de Entrega:* Documento SDD en `docs/sdd/05_design_system.md` v3.0 y `ADR-0014`.

- [x] **TASK-405 [UI/UX] [ARQUITECTURA]:** Implementación de la Librería Base de Componentes Standalone (`shared/components/ui/`), Motor de Temas en Caliente, Sidebar Colapsable y Fix de Z-Index (PB-042).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Desarrollar componentes en `shared/components/ui/` con Signal inputs; corregir z-index del selector de sucursales en navbar; estandarizar márgenes y paddings de página y colapso del sidebar (`270px` / `76px` / `0px`).
  - *Criterio de Entrega:* 100% pruebas de componentes en Vitest (`ui-kit.spec.ts`, `theme.service.spec.ts`).

- [x] **TASK-406 [UI/UX] [ARQUITECTURA]:** Motor Centralizado de Notificaciones (`<app-toast-portal>` en `z-[9999]`, `NotificationService`) y Validación Inline (`<app-form-field>`) (PB-043).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* `NotificationService` en Angular con renderizado en portal superior (`z-[9999]`), visibilidad garantizada sobre modales, y `<app-form-field>` con bordes rojos y texto de ayuda.
  - *Criterio de Entrega:* Toasts visibles por encima de cualquier backdrop de modal; pruebas unitarias de servicio.

---

### 3. Gestión de Menús Diarios, Plantillas y Flexibilidad de Recetas (PB-034 / PB-044)

- [x] **TASK-407 [CATALOG] [DISEÑO]:** Especificación del Modelo de Menús Diarios, Plantillas de Turno y Estrategias de Inventario (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar modelo relacional (`daily_menus`, `daily_menu_items`, `shift_menu_assignments`, `shift_menu_items`), plantillas reutilizables por día (ej. Lunes Ejecutivo), y 3 estrategias de insumos: BOM estricto, plato como insumo unitario o venta libre.
  - *Criterio de Entrega:* Diagramas de secuencia, actividad, ERD y modelo de dominio formalizados en `docs/project/architecture/` y ADR-0015 aceptado. Finalizado el 30/10/2026.

- [x] **TASK-408 [CATALOG] [NEGOCIO]:** Módulo Backend de Menús Diarios, Asignación a Turnos de Caja y Recetas Flexibles (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints para CRUD de plantillas de menú, asignación al turno con cupos pactados, alerta predictiva de stock según receta BOM y soporte de venta libre sin insumos.
  - *Criterio de Entrega:* Migración Flyway V9 (`V9__daily_menus_and_flexible_inventory.sql`), entidades JPA (`DailyMenu`, `DailyMenuItem`, `ShiftMenuAssignment`, `ShiftMenuItem`, `ShiftMenuQuotaAdjustment`), `DailyMenuPublicApi`, decremento atómico condicional con `@Version`, `InventoryStrategyFactory` con deducción polimórfica (BOM, unidad física, venta libre), 100% pruebas en Spring Modulith (`RestaurantModularityTests`, TC-MENU-001..004, TC-INV-005). Finalizado el 30/10/2026.

- [x] **TASK-409 [CATALOG/POS] [UI/UX]:** Centro de Menús en Catálogo y Selección de Menú en Apertura de Turno con Cupos Reactivos (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista `/catalog/menus` para armar plantillas; integración en modal de apertura de turno para elegir menú y ajustar cupos del día; decremento reactivo de cupos en POS con etiqueta "Agotado".
  - *Criterio de Entrega:* Vistas standalone `/catalog/menus` con UI Kit TailwindAdmin (`app-page-wrapper`, `app-card`, `app-button`, `app-badge`, `app-modal`), dropdown de selección de plantilla y tabla editable de cupos del día en modal de apertura de turno, modo híbrido en POS con banner de menú activo, filtro por tiempo de servicio (`ENTRADA`, `SEGUNDO`, `POSTRE`, `BEBIDA`), badge reactivo de cupos (`Cupo: X`), bloqueo táctil y etiqueta `AGOTADO` cuando el cupo llega a 0, y manejo de HTTP 409 Conflict; 100% pruebas unitarias en Vitest (`daily-menus.component.spec.ts`, `pos.component.spec.ts`, `catalog.service.spec.ts`). Finalizado el 30/10/2026.

---

### 4. Control de Turno en Finanzas y Ergonomía Móvil (PB-029 / PB-028)

- [ ] **TASK-410 [FINANCE] [NEGOCIO]:** Pestaña de Ventas del Turno Activo para el Cajero en Módulo de Finanzas (PB-029).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista en tiempo real de tickets cobrados en el turno en `/finance` con desglose por medio de pago (`CASH`, `CARD`, `QR`) y modal de detalle de comanda.
  - *Criterio de Entrega:* Totales coinciden 100% con el cálculo teórico del panel de arqueo de caja.

- [ ] **TASK-411 [UI/UX] [ARQUITECTURA]:** Ergonomía Móvil en POS y Sidebar Colapsable en Pantallas Medianas/Grandes (PB-028).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Botón hamburguesa en desktop/tablet para colapsar sidebar; eliminación de subtítulos innecesarios en móviles; menú contextual flotante para abrir turno/gasto; números legibles `text-sm font-mono` en inventario.
  - *Criterio de Entrega:* Interfaz limpia sin textos redundantes en smartphones y sidebar colapsable en escritorio.

---

### 5. Calidad, Testing y Certificación de Ciclo Continuo (PB-046)

- [ ] **TASK-412 [QA] [CALIDAD]:** Suite Automatizada E2E de Jornada Completa (Turno $\rightarrow$ Venta $\rightarrow$ Gasto $\rightarrow$ Arqueo) (PB-046).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Script Playwright/CDP persistido en `scripts/e2e/` validando de punta a punta: bloqueo sin turno $\rightarrow$ apertura con fondo $\rightarrow$ menú del día $\rightarrow$ 3 ventas $\rightarrow$ gasto menor con aprobación $\rightarrow$ arqueo matemático exacto.
  - *Criterio de Entrega:* Ejecución 100% determinista sin capturas de pantalla residuales; reporte en `docs/testing/`.
