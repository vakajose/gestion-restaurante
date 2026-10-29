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

- [ ] **TASK-401 [POS] [SEGURIDAD]:** Especificación e Invariante de Venta Exclusiva con Turno de Caja Abierto en Backend (PB-041 / UC-401).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* Diseñar y hacer cumplir la regla inviolable: "Toda orden o cobro en POS exige un `shift_id` con estado `OPEN`". Contrato de error RFC 7807 (`SHIFT_REQUIRED_ERROR` / HTTP 409).
  - *Criterio de Entrega:* El backend rechaza peticiones de venta sin turno activo; pruebas de integración en Spring Boot verificando HTTP 409.

- [ ] **TASK-402 [POS] [UI/UX]:** Bloqueo Reactivo de POS con Caja Cerrada y CTA Rápido de Apertura de Turno (PB-041).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* En frontend, bloquear comanda y catálogo si el turno no está abierto; mostrar banner explicativo con botón directo a "Abrir Turno con Fondo Inicial"; eliminar tag "Pendiente" y sanear indicador "En línea".
  - *Criterio de Entrega:* Es imposible interactuar con el carrito si no hay sesión abierta; al abrir turno, el POS se desbloquea reactivamente.

---

### 2. Design System de Restaurante y Suite de Componentes Standalone (PB-042 / PB-043)

- [ ] **TASK-403 [UI/UX] [DISEÑO]:** Especificación de Tokens de Color Semánticos, Layout Wrapper y Componentes Web Base (PB-042).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Definir tokens semánticos en Tailwind (`primary`, `secondary`, `destructive/danger` diferenciado de `success`), contrastes WCAG AA para Dark/Light mode y wireframes de componentes base.
  - *Criterio de Entrega:* Documento SDD en `docs/sdd/05_design_system.md` actualizado con guía de uso de componentes.

- [ ] **TASK-404 [UI/UX] [ARQUITECTURA]:** Implementación de Componentes Reutilizables Standalone (`<app-button>`, `<app-badge>`, `<app-page-wrapper>`) y Fix de Z-Index (PB-042).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Desarrollar componentes en `shared/components/` con Signal inputs; corregir z-index del selector de sucursales en navbar; estandarizar márgenes y paddings de página con `<app-page-wrapper>`.
  - *Criterio de Entrega:* 100% pruebas de componentes en Vitest; eliminación de botones de cancelar verdes.

- [ ] **TASK-405 [UI/UX] [ARQUITECTURA]:** Motor Centralizado de Notificaciones (Toasts en Portal Z-Index Seguro y Alertas en Línea) (PB-043).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* `NotificationService` en Angular con renderizado en portal superior (`z-[9999]`), visibilidad garantizada sobre modales, y directiva de validación en línea de formularios con bordes rojos y texto de ayuda.
  - *Criterio de Entrega:* Toasts visibles por encima de cualquier backdrop de modal; pruebas unitarias de servicio.

---

### 3. Gestión de Menús Diarios, Plantillas y Flexibilidad de Recetas (PB-034 / PB-044)

- [ ] **TASK-406 [CATALOG] [DISEÑO]:** Especificación del Modelo de Menús Diarios, Plantillas de Turno y Estrategias de Inventario (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar modelo relacional (`menus`, `menu_items`, `shift_menu_selection`), plantillas reutilizables por día (ej. Lunes Ejecutivo), y 3 estrategias de insumos: BOM estricto, plato como insumo unitario o venta libre.
  - *Criterio de Entrega:* Diagramas de clase, ERD y secuencias SDD formalizados en `docs/project/architecture/`.

- [ ] **TASK-407 [CATALOG] [NEGOCIO]:** Módulo Backend de Menús Diarios, Asignación a Turnos de Caja y Recetas Flexibles (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints para CRUD de plantillas de menú, asignación al turno con cupos pactados, alerta predictiva de stock según receta BOM y soporte de venta libre sin insumos.
  - *Criterio de Entrega:* 100% pruebas en Spring Modulith con aislamiento modular y verificación de cupos.

- [ ] **TASK-408 [CATALOG/POS] [UI/UX]:** Centro de Menús en Catálogo y Selección de Menú en Apertura de Turno con Cupos Reactivos (PB-034 / PB-044).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista `/catalog/menus` para armar plantillas; integración en modal de apertura de turno para elegir menú y ajustar cupos del día; decremento reactivo de cupos en POS con etiqueta "Agotado".
  - *Criterio de Entrega:* Operación fluida en navegador; pruebas unitarias de componentes.

---

### 4. Control de Turno en Finanzas y Ergonomía Móvil (PB-029 / PB-028)

- [ ] **TASK-409 [FINANCE] [NEGOCIO]:** Pestaña de Ventas del Turno Activo para el Cajero en Módulo de Finanzas (PB-029).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista en tiempo real de tickets cobrados en el turno en `/finance` con desglose por medio de pago (`CASH`, `CARD`, `QR`) y modal de detalle de comanda.
  - *Criterio de Entrega:* Totales coinciden 100% con el cálculo teórico del panel de arqueo de caja.

- [ ] **TASK-410 [UI/UX] [ARQUITECTURA]:** Ergonomía Móvil en POS y Sidebar Colapsable en Pantallas Medianas/Grandes (PB-028).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Botón hamburguesa en desktop/tablet para colapsar sidebar; eliminación de subtítulos innecesarios en móviles; menú contextual flotante para abrir turno/gasto; números legibles `text-sm font-mono` en inventario.
  - *Criterio de Entrega:* Interfaz limpia sin textos redundantes en smartphones y sidebar colapsable en escritorio.

---

### 5. Calidad, Testing y Certificación de Ciclo Continuo (PB-046)

- [ ] **TASK-411 [QA] [CALIDAD]:** Suite Automatizada E2E de Jornada Completa (Turno $\rightarrow$ Venta $\rightarrow$ Gasto $\rightarrow$ Arqueo) (PB-046).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Script Playwright/CDP persistido en `scripts/e2e/` validando de punta a punta: bloqueo sin turno $\rightarrow$ apertura con fondo $\rightarrow$ menú del día $\rightarrow$ 3 ventas $\rightarrow$ gasto menor con aprobación $\rightarrow$ arqueo matemático exacto.
  - *Criterio de Entrega:* Ejecución 100% determinista sin capturas de pantalla residuales; reporte en `docs/testing/`.
