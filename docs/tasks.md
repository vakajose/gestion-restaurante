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

- [x] **TASK-201 (Security & Tenancy / Backend & Frontend):** Implementar Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa (PB-019 / UC-201). Soporte de `tenantIdentifier` (`slug` o `nit_or_tax_id`), migración Flyway `V3__tenant_slug_and_rbac.sql` con RBAC granular, token JWT con claims `tenant_slug`, `permissions` y `branch_ids`. Soporte de cabecera `X-Branch-Id` en `JwtAuthenticationFilter` con validación multi-tenant (HTTP 400/403 ProblemDetail), servicio reactivo `BranchContextService` con Angular Signals, interceptor HTTP inyectando `X-Branch-Id` y selector interactivo de sucursal en el Topbar con conmutación dinámica y vista limpia sin selector para usuarios de sucursal única.
- [x] **TASK-202 (Company & Branches / Backend & Frontend):** Implementar módulo `modules.company` en Spring Modulith con `CompanyPublicApi` y `BranchPublicApi`, endpoints REST para Tenants y Branches (respetando ADR-0006 de huso horario `branches.timezone`), `GlobalExceptionHandler` RFC 7807 (`ProblemDetail`) y vistas de administración de Empresa y Red de Sucursales en TailwindAdmin v2.0.
- [x] **TASK-203 (RBAC & Guards / Frontend):** Implementar Route Guards (`authGuard`, `guestGuard`, `roleGuard`, `permissionGuard`) en Angular 22 Zoneless con soporte de permisos granulares por recurso (`pos:access`, `inventory:read`, `finance:shifts`, `catalog:read`, `company:read`) y Sidebar reactivo agrupado por perfil con badges humanizados de rol en TailwindAdmin v2.0.
- [x] **TASK-204 (Users & Profile / Backend & Frontend):** Implementar Gestión de Personal y 'Mi Perfil' (PB-018 / UC-204) con desacoplamiento modular (PB-011). Creación de contrato público `UserPublicApi` en `core.security.api`, endpoints REST `/api/v1/users` y `/api/v1/profile` con hashing Argon2id, validación de unicidad por tenant y protección RBAC (`users:read`, `users:write`). Refactorización de `CashShiftService` y `ExpenseService` para inyectar `UserPublicApi`. Vistas en Angular 22 / TailwindAdmin v2.0 para administración de colaboradores con modal CRUD y panel "Mi Perfil" con actualización de email y cambio seguro de contraseña.
- [x] **TASK-206 (Inventory & Kardex / Backend & Frontend):** Alta de Stock Inicial de Sucursal (`INITIAL_STOCK`) y apertura en Kardex (PB-021 / UC-206). Registro atómico de saldo base y costo unitario en `branch_stock`, inserción de movimiento `INITIAL_STOCK` en `kardex_movements` y validación de conflicto estricta (`HTTP 409 Conflict`) si el insumo ya cuenta con movimientos previos en la sucursal. Vistas en Angular 22 / TailwindAdmin v2.0 con modal de apertura y selección de sucursal contextualizada.
- [x] **TASK-208 (Catalog & POS / Backend & Frontend):** Gestión y venta de Combos gastronómicos flexibles (*Packs*) en catálogo y POS con deducción recursiva en Kardex (PB-020 / UC-208). Definición de entidades `Combo`, `ComboItem`, `ComboItemOption` con modos `FIXED` y `CHOICE`. Integración en POS con modal de personalización interactiva de opciones y selección de platos componentes. Soporte en venta POS y publicación en `OrderPaidEvent` para deducción atómica y recursiva de todas las recetas BOM de los componentes en `KardexService`. Vistas en TailwindAdmin v2.0 y pruebas unitarias/integración al 100%.
- [x] **TASK-209 (POS & Pricing / Backend & Frontend):** Motor de Promociones Temporales Parametrizables (*Happy Hour*, ofertas por horario y día según huso local de sucursal) (PB-020 / UC-209). Entidades `Promotion` y `PromotionBranch`, motor `PromotionEngine` evaluando vigencia temporal contra el huso horario local de la sucursal (`branches.timezone` según ADR-0006), días de semana y ventana horaria. Modos `AUTOMATIC` y `MANUAL`, cálculo y desglose de descuentos porcentuales o de monto fijo, soporte en órdenes y DTOs, banner interactivo en POS y resumen de cobro con desglose reactivo. 100% de pruebas unitarias e integración en backend y frontend.
- [x] **TASK-210 (Catalog & POS / Backend & Frontend):** Grupos de Modificadores y extras de platos con recargo de precio y deducción en Kardex (PB-020 / UC-210). Definición de grupos de modificadores (`modifier_groups`, `modifiers`, `dish_modifier_groups`), personalización opcional u obligatoria con validación de selección mínima y máxima (`minSelectable`, `maxSelectable`). Interfaz POS para selección interactiva de extras al agregar platos al pedido, recargo acumulado en precio unitario/subtotal y persistencia de extras en `order_item_modifiers`. Publicación de extras con consumo de ingredientes en `OrderPaidEvent` y deducción atómica complementaria en `KardexService`. 100% de pruebas unitarias e integración en backend y frontend.

---

## Tareas en Ejecución (Sprint 2 - Bloque P1)

- [ ] **TASK-207 (POS):** Descuentos directos y cupones en POS parametrizados (sin límites quemados).



