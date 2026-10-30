---
title: "Sprint 4 Backlog — Fundamentos, Blindaje de Caja y Menús Diarios (Sprint Activo)"
category: "tasks"
sprint: 4
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-4
  - execution-plan
  - agents
last_updated: "2026-10-29"
---

# 🚀 Sprint 4 Backlog — Fundamentos, Blindaje de Caja y Menús Diarios (Sprint Activo)

> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./project/sprints/README.md) | [Product Backlog (Por Prioridades)](./project/product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1 (Cerrado)](./project/sprints/sprint_01_backlog.md) | [Sprint 2 (Cerrado)](./project/sprints/sprint_02_backlog.md) | [Sprint 3 (Cerrado v0.3.0)](./project/sprints/sprint_03_backlog.md)  
> **Sprints Futuros:** [Sprint 5 (Candidato a Producción)](./project/sprints/sprint_05_planning.md) | [Sprint 6 (Infraestructura Cloud y SaaS Backoffice)](./project/sprints/sprint_06_planning.md)

---

## 📋 Reglas de Ejecución y Protocolo de Calidad (DoD)

El desarrollo se ejecuta de manera incremental y atómica siguiendo el ciclo **SDD-SDLC de 4 fases** (ADR-0007 y Protocolo Multi-Agente).
Antes de dar por concluida cualquier tarea:
- Backend: `./mvnw clean test` (100% pruebas unitarias e integración pasando sin fallos).
- Frontend: `npm run lint && npm test` (100% pruebas unitarias pasando en Vitest/JSDOM con 0 advertencias).
- SDD: Sincronización obligatoria de diagramas Mermaid si se alteran APIs, base de datos o flujos.
- Multi-Agente: Sincronización autónoma inmediata a cargo de `doc-architect` sin recordatorios manuales.

> [!IMPORTANT]
> **Secuencia de Arranque del Sprint 4:**
> 1. **Fase Inmediata:** Subsanar `PB-041` (Invariante de Turno en POS) en backend y frontend para cerrar la fuga financiera.
> 2. **Fase Cimiento UI/UX:** Diseñar y construir `PB-042` (Tokens y componentes) y `PB-043` (Motor de notificaciones) para que las vistas subsiguientes nazcan estandarizadas.
> 3. **Fase Negocio & Operación Diaria:** Implementar `PB-034` (Menús Diarios y Cupos), `PB-044` (Flexibilidad de Recetas BOM), `PB-029` (Ventas del Turno para Cajero), `PB-028` (Ergonomía Móvil y Sidebar Colapsable) y certificar con `PB-046` (Batería E2E Crítica).

---

## 🎯 Tareas del Sprint 4 (En Curso)

### 1. Blindaje Financiero del Punto de Venta (PB-041 - Defecto Crítico)

- [x] **TASK-401 [POS] [DISEÑO]:** Especificación Técnica del Invariante de Turno en POS, Políticas de Apertura y Gastos RBAC (PB-041 / ADR-0013).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* Diseñar y formalizar la regla inviolable: "Toda orden o cobro en POS exige un `shift_id` con estado `OPEN`". Contrato de error RFC 7807 (`SHIFT_REQUIRED_ERROR` / HTTP 409), trazabilidad relacional `orders.user_id`, parámetros `finance.shift_opening_policy` y `finance.expense_allow_self_approval`, y ergonomía silenciosa móvil.
  - *Criterio de Entrega:* Documentos certificados: [ADR-0013](./adr/0013_pos_shift_invariant_and_expense_authorizations.md), diagrama de secuencia [sequence_pos_order_shift_validation.md](./project/architecture/04-behavior-and-flows/sequence_pos_order_shift_validation.md), y actualización de manuales SOP y specs SDD backend/frontend.

- [x] **TASK-402 [POS] [SEGURIDAD]:** Implementación de Invariante de Turno en Backend, Políticas de Apertura y Gastos RBAC (PB-041).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* En `OrderServiceImpl`, inyectar `FinancePublicApi`, validar turno activo y retornar HTTP 409 si la caja está cerrada; asociar `cashShiftId` y `user_id`; proteger `FinanceController` con `@PreAuthorize`; aplicar políticas de supervisor y autoaprobación de gastos.
  - *Criterio de Entrega:* 100% de pruebas en Spring Modulith (`OrderIntegrationTests`, `CashShiftIntegrationTests`).

- [x] **TASK-403 [POS] [UI/UX]:** Bloqueo Reactivo de POS con Caja Cerrada, Integración con FinanceService y Modo Offline Móvil (PB-041).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* En `pos.component.ts`, inyectar `FinanceService`, deshabilitar comanda/cobro si `!hasOpenShift()`, mostrar banner de caja cerrada con botón de apertura rápida; transmitir `cashShiftId` en checkout; limpiar header suprimiendo badges ruidosos y activar barra ámbar de modo offline solo al cortar red.
  - *Criterio de Entrega:* POS bloqueado ante caja cerrada en navegador; 100% de tests unitarios en Vitest.

---

### 2. Design System de Restaurante y Suite de Componentes Standalone (PB-042 / PB-043)

- [x] **TASK-404 [UI/UX] [DISEÑO]:** Especificación de Tokens de Color Semánticos, Motor de Temas en Caliente, Layout Wrapper y Suite UI Kit (PB-042 / ADR-0014).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Definir la arquitectura `TailwindAdmin v3.0` con variables CSS en caliente (`slate-clean`, `warm-tinted`, `dark-nav` y 5 paletas de marca), escala canónica de `z-index`, iconografía vectorial Offline-First (`Solar` / `Tabler`) y contratos de los 8 componentes Standalone del UI Kit.
  - *Criterio de Entrega:* Documentos certificados: [ADR-0014](./adr/0014_tailwindadmin_v3_dynamic_theme_and_uikit.md), [05_design_system.md](./sdd/05_design_system.md) v3.0 y sincronización de [03_frontend_spec.md](./sdd/03_frontend_spec.md) y [c4_level3_frontend_angular.md](./project/architecture/01-c4-system/c4_level3_frontend_angular.md).

- [x] **TASK-405 [UI/UX] [ARQUITECTURA]:** Implementación de la Librería Base de Componentes Standalone (`shared/components/ui/`), Motor de Temas en Caliente, Sidebar Colapsable y Fix de Z-Index (PB-042).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Construir en `frontend/src/app/shared/components/ui/` los componentes `<app-icon>`, `<app-page-wrapper>`, `<app-button>`, `<app-badge>`, `<app-card>`, `<app-modal>` y `<app-theme-customizer>`; actualizar `ThemeService`, `styles.css`, `tailwind.config.js` y `AppComponent` (Topbar `z-30`, Sidebar colapsable `270px`/`76px`/`0px` y márgenes de página garantizados). *(La adopción en cada vista de negocio se realiza gradualmente por módulo).*
  - *Criterio de Entrega:* 100% pruebas de componentes y servicios pasando en Vitest (`ui-kit.spec.ts`, `theme.service.spec.ts`, `app.component.spec.ts`).

- [x] **TASK-406 [UI/UX] [ARQUITECTURA]:** Motor Centralizado de Notificaciones (`<app-toast-portal>` en `z-[9999]`, `NotificationService`) y Validación Inline (`<app-form-field>`) (PB-043).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* `NotificationService` reactivo con Signals conectado automáticamente a `errorInterceptor` para proyectar errores `RFC 7807 ProblemDetail` sobre cualquier modal activo (`z-[9999]`), y componente `<app-form-field>` con tinción de borde rojo (`.has-error`) y auto-scroll en `<app-modal>`.
  - *Criterio de Entrega:* Toasts visibles por encima de cualquier backdrop de modal; pruebas unitarias en `notification.service.spec.ts` y `ui-kit.spec.ts`.

---

### 3. Gestión de Menús Diarios, Plantillas y Flexibilidad de Recetas (PB-034 / PB-044)

- [x] **TASK-407 [CATALOG] [DISEÑO]:** Especificación del Modelo de Menús Diarios, Plantillas de Turno y Estrategias de Inventario (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar modelo relacional (`daily_menus`, `daily_menu_items`, `shift_menu_assignments`, `shift_menu_items`), plantillas reutilizables por día (ej. Lunes Ejecutivo), y 3 estrategias de insumos: BOM estricto, plato como insumo unitario o venta libre.
  - *Criterio de Entrega:* Diagramas de secuencia, actividad, ERD y modelo de dominio formalizados en `docs/project/architecture/` y ADR-0015 aceptado. Finalizado el 30/10/2026.

- [ ] **TASK-408 [CATALOG] [NEGOCIO]:** Módulo Backend de Menús Diarios, Asignación a Turnos de Caja y Recetas Flexibles (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints para CRUD de plantillas de menú, asignación al turno con cupos pactados, alerta predictiva de stock según receta BOM y soporte de venta libre sin insumos.
  - *Criterio de Entrega:* 100% pruebas en Spring Modulith con aislamiento modular y verificación de cupos.

- [ ] **TASK-409 [CATALOG/POS] [UI/UX]:** Centro de Menús en Catálogo y Selección de Menú en Apertura de Turno con Cupos Reactivos (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista `/catalog/menus` para armar plantillas; integración en modal de apertura de turno para elegir menú y ajustar cupos del día; decremento reactivo de cupos en POS con etiqueta "Agotado".
  - *Criterio de Entrega:* Operación fluida en navegador; pruebas unitarias de componentes.

---

### 4. Control de Turno en Finanzas y Ergonomía Móvil (PB-029 / PB-028)

- [x] **TASK-410 [FINANCE] [NEGOCIO]:** Reingeniería Integral de Control de Caja (`/cash-control`), Finanzas y Arqueos (`/finance`), Turnos Multi-Cajero y Vista de Comandas en Vivo (PB-029).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Separación arquitectónica de operativa diaria (`/cash-control`) y auditoría gerencial (`/finance`), parámetro multinivel `finance.shift_concurrency_mode` para turnos concurrentes multi-cajero, barra unificada de KPIs (0 redundancias), pestaña de comandas en vivo cronológica descendente con filtros rápidos (Todas, Mis Ventas, Por Cajero, Método de Pago, Búsqueda de Ticket) y modal de detalle de comanda.
  - *Criterio de Entrega:* 100% de tests unitarios y de arquitectura pasando en backend (133/133) y frontend (268/268, 35 archivos), 0 errores de linting. ADR-0016 documentado.

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
