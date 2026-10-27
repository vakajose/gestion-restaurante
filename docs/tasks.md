---
title: "Backlog de Implementación y Tareas de Agentes"
category: "tasks"
tags:
  - tasks
  - execution-plan
  - agents
  - kanban
last_updated: "2026-10-25"
status: "verified"
---

# Backlog de Implementación para Agentes


El desarrollo se ejecuta de manera incremental y atómica. Cada tarea se implementa en su propia rama Git con la convención:
`git checkout -b feat/<modulo>-<tarea>` y commits bajo el estándar Conventional Commits.

---

## Tareas Completadas y en Ejecución

- [x] **TASK-001 (Init / Backend):** Inicializar estructura Spring Boot con Java 25 LTS (Amazon Corretto) en `backend/` usando Maven Wrapper, Spring Modulith, Spring Security, Spring Data JPA y Flyway. Configurar soporte de Virtual Threads (`spring.threads.virtual.enabled=true`).
- [x] **TASK-002 (DB / Migration):** Crear script Flyway `V1__init_schema.sql` con el DDL PostgreSQL completo y normalizado/denormalizado para multi-tenancy (`tenants`, `tenant_settings`, `branches`, `branch_settings`, `app_users`, `categories`, `ingredients`, `dishes`, `dish_recipes`, `branch_dishes`, `branch_stock`, `purchases`, `purchase_items`, `kardex_movements`, `orders`, `order_items`, `cash_shifts`, `expenses`, `audit_logs`).
- [x] **TASK-003 (Auth & Config / Backend):** Implementar en `core.common` las entidades abstractas `BaseTenantEntity` y `BaseBranchEntity`. Configurar `TenantContextHolder`, filtro JWT, autenticación adaptativa (login por `username` obligatorio o `email` opcional con Argon2id) y el servicio de configuración dinámica `TenantConfigService`.
- [x] **TASK-004 (Init / Frontend):** Crear proyecto Angular 22 en `frontend/` con componentes Standalone puros, Zoneless Change Detection (`provideExperimentalZonelessChangeDetection`), Tailwind CSS con tokens Datta Able y servicio `ThemeService` reactivo con persistencia local y sincronización.
- [x] **TASK-005 (UI / Auth):** Maquetar vistas de Login adaptativo (ingreso por usuario o correo) y Registro según la estética Datta Able (círculos decorativos turquesa/púrpura y formulario centrado).
- [x] **TASK-010 (UI / Refactor Design System):** Migración y rediseño completo del sistema visual del Frontend de Datta Able a TailwindAdmin v2.0 (Refactor de TASK-004 y TASK-005). Actualización de tokens Tailwind (`primary: #5d87ff`, `lightprimary: #ecf2ff`, DM Sans, radios 7px, sombras multicapa), clases utilitarias (`.card`, `.btn`, `.form-control`, `.sidebar-link`, `.activemenu`), layout principal con Sidebar (270px) / Topbar y vista de login adaptativo sin orbes fluorescentes.
- [x] **TASK-009 (Catalog & Overrides / Backend & Frontend):** Implementar catálogo maestro de platos, activación/sobreescritura de precios por sucursal (`branch_dishes`) y clonación de platos con receta diferenciada (`cloned_from_id`).
- [x] **TASK-007 (POS / Offline):** Configurar Dexie.js en Angular 22 para persistencia local de catálogo (`products`, `categories`), turno activo (`activeShift`) y cola de pedidos offline (`pendingOrders`) con reconciliación en segundo plano usando cabecera de idempotencia `X-Client-Transaction-Id`.
- [x] **TASK-006 (Inventory & Deductions / Backend & Frontend):** Implementar la deducción de recetas en `modules.inventory` mediante `@ApplicationModuleListener` ante el evento `OrderPaidEvent`, consumiendo `CatalogPublicApi` y aplicando el patrón Strategy según `INVENTORY_DEDUCTION_MODE` (`MANUAL_ISOLATED`, `AUTOMATIC_PERMISSIVE`, `AUTOMATIC_STRICT`), con libro mayor de Kardex y vistas en TailwindAdmin.

- [x] **TASK-008 (Finance & Shift / Backend & Frontend):** Implementar ciclo de turnos de caja (`cash_shifts`), arqueo de efectivo con conciliación en tiempo real y workflow de solicitud y aprobación de gastos menores de caja chica (`expenses`) con soporte de evidencia digital adjunta y panel TailwindAdmin v2.0.
- [x] **TASK-011 (CI / Fix Frontend Workflow):** Corregir la ejecución del pipeline CI en GitHub Actions para el Frontend Angular 22 (`.github/workflows/ci.yml`). Eliminar el flag legado `--browsers=ChromeHeadless` que causaba error de runner Vitest no encontrado y permitir que `@angular/build:unit-test` ejecute la suite completa con JSDOM headless (`npm test -- --watch=false`).
- [x] **TASK-012 (Docs / Governance & Skill):** Establecer estándar de Specification-Driven Development (SDD), crear Skill `.agents/skills/sdd-architecture-diagramming/SKILL.md`, actualizar `.agents/rules/architectural-governance.md` y `AGENTS.md`, y estructurar el directorio `docs/project/architecture/` con índice Obsidian (`README.md`).
- [x] **TASK-013 (Docs / C4 Architecture & Component Diagrams):** Diseñar y documentar la arquitectura C4 (Contenedores Nivel 2, Componentes Backend Spring Modulith Nivel 3 y Componentes Frontend Angular Zoneless Nivel 3) con sus interfaces `.api`, visibilidad `.internal` y eventos.
- [x] **TASK-014 (Docs / Domain Model & Database ERD):** Diseñar y documentar el Diagrama de Modelo de Dominio Conceptual, el ERD Físico PostgreSQL completo (18 tablas, claves, índices y denormalización multi-tenant) y el esquema IndexedDB Dexie.js.
- [x] **TASK-015 (Docs / Use Cases Catalog):** Diseñar y documentar el Mapa General de Actores y Casos de Uso del Sistema, y los diagramas de casos de uso focalizados por módulo (`catalog`, `pos`, `inventory`, `finance`).
- [x] **TASK-016 (Docs / Behavioral & Sequence Flows):** Diseñar y documentar los diagramas de comportamiento: Secuencia de Venta POS + Deducción Asíncrona Kardex/Finanzas, Secuencia Offline-First e Idempotencia Dexie, Máquina de Estados de Orden, Workflow de Turnos y Gastos, y Flujo de Actividad de Estrategias de Inventario.
- [x] **TASK-017 (Docs / Cross-Linking & Readme Sync):** Integrar y actualizar las referencias cruzadas en toda la documentación existente (`README.md`, `docs/sdd/`, `docs/adr/`, `docs/project/product_backlog.md`, `docs/project/retrospectiva_negocio_sprint1.md`).

---

## Sprint 2 — Tareas en Ejecución y Completadas

- [x] **TASK-201 (Security & Tenancy / Backend & Frontend):** Implementar Login Calificado por Tenant (`tenantIdentifier` con soporte indistinto para `slug` o `nit_or_tax_id`), migración Flyway `V3__tenant_slug_and_rbac.sql` agregando `slug` a `tenants` y soporte de Control de Acceso Basado en Recursos (RBAC granular multi-tenant) con tablas `roles` y `role_permissions`. Claims de permisos y `tenantSlug` en token JWT y formulario de login adaptativo en TailwindAdmin v2.0.
- [ ] **TASK-202 (Company & Branches / Backend & Frontend):** Implementar módulo `modules.company` en Spring Modulith con `CompanyPublicApi` y `BranchPublicApi`, endpoints REST para Tenants y Branches (respetando ADR-0006 de huso horario `branches.timezone`) y vistas de administración en TailwindAdmin v2.0.
- [ ] **TASK-203 (RBAC & Guards / Frontend):** Implementar Route Guards (`authGuard`, `roleGuard`, `permissionGuard`) en Angular 22 Zoneless y Sidebar dinámico por rol y permisos del usuario.

---

## Tareas Pendientes (Sprint 2 - Bloques Posteriores)

- [ ] **TASK-204 (Users & Profile):** Gestión de Personal y 'Mi Perfil' con badges humanizados.
- [ ] **TASK-205 (Context):** Selector contextual de sucursal en Topbar.
- [ ] **TASK-206 (Inventory):** Alta de stock inicial de sucursal (`INITIAL_STOCK`) y apertura en Kardex.
- [ ] **TASK-207 (POS):** Descuentos directos y cupones en POS con PIN de supervisor.



