---
title: "Backlog Activo de Ejecución — Sprint 6 y Versión Futura (release/sprint-6)"
category: "tasks"
sprint: 6
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-6
  - release
  - testing-local
  - roadmap
last_updated: "2026-11-17"
---

# 🚀 Backlog Activo — Sprint 6 y Siguientes Pasos (`release/sprint-6`)

> **Rama Activa de Pruebas y Estabilización:** `release/sprint-6` (entorno `testing-local`)  
> **Histórico Completo de Sprint 6 (`TASK-601`–`608`, `610`–`618`, `620`–`635`):** [`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md)  
> **Documento Maestro de Planificación:** [`sprint_06_planning.md`](./project/sprints/sprint_06_planning.md)  
> ⬅️ **Navegación:** [Bóveda de Sprints](./project/sprints/README.md) | [Product Backlog](./project/product_backlog/README.md) | [Bóveda de Pruebas](./testing/README.md)

---

## 📋 Reglas de Ejecución (`release/sprint-6`)

1. **Aislamiento de `TASK-609`:** La provisión de Oracle Cloud (`TASK-609`) se ejecuta en paralelo en otro chat/worktree; no modificar infraestructura OCI desde esta rama.
2. **Protocolo Git & Quality Gates:** Validar `./mvnw clean test` (backend), `npm run lint && npm test -- --watch=false` (ERP), `npm run lint && npm test && npm run build` (Backoffice) sin ejecutar `git push` autónomo.
3. **Co-Diseño Obligatorio (`AGENTS.md` Reglas 8 y 9):** Toda tarea nueva de arquitectura, UX o librería externa (`TASK-636`–`TASK-640`) debe recorrer Fases 1 y 2 en co-diseño interactivo con el usuario antes de implementar.

---

## 🎯 Tareas Activas de Cierre de Sprint 6

### 1. Provisión Guiada Oracle Cloud (OCI), IaC y Certificación Staging Nube (En Paralelo)

- [ ] **TASK-609 [CLOUD / IaC] [OCI]:** Provisión Guiada de Oracle Cloud Infrastructure (VCN, IAM, Compute, Object Storage, Presupuestos) y Certificación Smoke Staging Nube (`PB-025`).
  - *Prioridad:* `P1 - Alta`
  - *Nota:* Se ejecuta en chat/worktree independiente. No intervenir desde `release/sprint-6`.
  - *Objetivo:* Aprovisionar VCN, VM 1 (Apps ARM64), VM 2 (PostgreSQL 16), buckets privados de medios/backups, Customer Secret Keys S3, Secrets de GitHub Actions y ejecutar certificación Smoke HTTPS en Staging.

*(La tarea `TASK-613` — Auditoría y Sincronización Total de Documentación, SDD, Gobernanza Agéntica y Manuales Cliente — fue completada y archivada en [`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md).)*

---

## 🧭 Backlog de Versión Futura (Prioridad Alta `P1`)

- [ ] **TASK-639 [DOCS/UX] [ERP/BACKOFFICE] (`PB-062`):** Wiki / Manual Web Integrado en la Aplicación para Usuario Final.
  - *Prioridad:* `P1 - Alta` *(Versión Futura)*
  - *Base Documental:* [`docs/manuals/restaurante/`](./manuals/restaurante/README.md) y [`docs/manuals/plataforma/`](./manuals/plataforma/bootstrap_y_operacion_inicial.md)
  - *Objetivo:* Diseñar e incorporar una sección de ayuda / manual web interactivo dentro de la aplicación orientado a administradores, cajeros y personal de cocina (guías paso a paso por módulo y flujos diarios).
  - *Restricción de Co-Diseño (Reglas 8 y 9):* Antes de implementar, investigar opciones consolidadas de la comunidad compatibles con Markdown (ej. renderizadores ligeros como `marked`, `@ngx-markdown` o compilación estática) frente a una solución propia liviana, presentando pros, contras y peso en bundle al usuario para decidir conjuntamente.

- [ ] **TASK-640 [ARQ/FUNC] [ERP - CONFIG/POS/CATÁLOGO/DEXIE] (`PB-063`):** Completar Cableado de Parámetros Declarados y Brechas MVP.
  - *Prioridad:* `P1 - Alta` *(Versión Futura)*
  - *Objetivo:* Conectar en tiempo de ejecución los parámetros ya declarados en `SystemParameter.java` / `parameters-catalog.json` y cerrar las brechas funcionales detectadas en la auditoría MVP:
    1. **Inactividad de Sesión:** Conectar `security.session_inactivity_timeout_minutes` a `IdleTimerService` (`setTimeoutMinutes(...)` tras login y actualización de parámetros).
    2. **Modalidades y Delivery en POS:** Cablear `pos.allow_scheduled_pickup`, `pos.delivery_fee_enabled` y `pos.delivery_fee_default` en POS UI/backend (habilitación dinámica de `SCHEDULED_PICKUP` y aplicación automática de tarifa por defecto en `DELIVERY`).
    3. **Selector de Estrategia de Inventario por Plato (`ADR-0015`):** Habilitar endpoint `PATCH /api/v1/catalog/dishes/{id}/inventory-strategy` en `CatalogController` + selector UI en `/catalog` para `RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT` y `COMMERCIAL_NO_INVENTORY`.
    4. **Soporte Caché Offline Dexie para Combos y Menú del Día:** Extender `LocalCatalogRepository` / `appDb` para cachear y operar offline con `Combo`, `DailyMenu` y `ShiftMenuItem` de forma consistente con platos simples.

---

## 🛠️ Backlog de Versión Futura (Prioridad Media `P2`)

- [ ] **TASK-637 [DISEÑO/REFACTOR] [ERP - CATÁLOGO/POS] (`PB-061`):** Unificar Clasificadores de Combos Dinámicos y Filtros de Menú hacia Categorías del Catálogo (`0..*` por Categoría) en Lugar de `CourseType`.
  - *Prioridad:* `P2 - Media`
  - *Origen:* Co-Diseño Post-E2E `TASK-634` (Feedback sobre `CourseType` vs `Category`).
  - *Objetivo:* Evaluar y diseñar (Fases 1 y 2 SDD) el reemplazo de `CourseType` (`STARTER`/`MAIN`/`DESSERT`/`BEVERAGE`/`GENERAL`) y `selectionSource = MENU_COURSE` para que los renglones dinámicos de combos referencien directamente categorías reales (`category_id`) con cardinalidad libre `0..*` (`min`/`max`), definiendo además la resolución unificada cuando la sucursal opera en modo `FULL_CATALOG` vs `DAILY_MENU`.

- [ ] **TASK-638 [ARQ/DISEÑO] [ERP - CATÁLOGO/POS] (`PB-061`):** Unificación Conceptual de Elementos de Carta (`Dish` / `Combo` / `Complemento`), Complementos con Costo Extra en Combos y Materialización de `FULL_CATALOG`.
  - *Prioridad:* `P2 - Media`
  - *Origen:* Co-Diseño Post-E2E (`TASK-634` / `TASK-637`).
  - *Objetivo:* Analizar y diseñar: (1) asignación de grupos de complementos/extras (`ModifierGroup`) también a `Combo` (además de `Dish`) y recargos por selecciones `0..*` adicionales en combos (aprovechando que `OrderServiceImpl` ya soporta sumar `selectedModifiers + selections`), (2) unificación conceptual de elementos vendibles/agrupables en el catálogo, y (3) alinear si `FULL_CATALOG` debe materializarse como un menú completo de la carta para que combos/filtros operen siempre sobre una estructura uniforme.

---

## 📦 Post-MVP / Baja Prioridad (`P3`)

- [ ] **TASK-636 [ESTUDIO/FUNC] [ERP - INVENTARIO/KARDEX] (`OBS-023` / `PB-021`):** Estudio Estándar de Inventario Gastronómico (Insumos Pre-elaborados, Ajustes por Costo Total de Lote y Kardex Completo).
  - *Prioridad:* `P3 - Baja` *(fuera de MVP v1)*
  - *Origen:* [`OBS-023`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diseñar e implementar un modelo estándar y retrocompatible de inventario para pequeños productores/restaurantes (separando insumos crudos vs pre-elaborados, trazabilidad de compras y flujo formal completo de Kardex).
