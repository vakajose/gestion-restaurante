---
title: "Backlog de Implementación y Tareas de Agentes"
category: "tasks"
tags:
  - tasks
  - execution-plan
  - agents
  - kanban
last_updated: "2026-10-27"
status: "verified"
---

# Backlog de Implementación para Agentes


El desarrollo se ejecuta de manera incremental y atómica. Cada tarea se implementa en su propia rama Git con la convención:
`git checkout -b feat/<modulo>-<tarea>` o `refactor/<modulo>-<tarea>`, y commits bajo el estándar Conventional Commits.
Antes de dar por concluida cualquier tarea:
- Backend: `./mvnw clean test` (100% pruebas pasando sin fallos).
- Frontend: `npm run lint && npm test` (100% pruebas unitarias pasando en Vitest/JSDOM).
- SDD: Sincronización obligatoria de diagramas Mermaid si se alteran APIs, base de datos o flujos.

---

## Tareas Completadas (Hito 1 e Infraestructura)

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

## Tareas Pendientes (Sprint 2 - Gobernanza, RBAC, Abastecimiento y Promociones)

Las siguientes tareas se derivan directamente del diagnóstico y acuerdos de la [Retrospectiva Funcional del Sprint 1](file:///home/jvaca/proyectos/gestion-restaurante/docs/project/retrospectiva_negocio_sprint1.md) y de las iniciativas del [Product Backlog](file:///home/jvaca/proyectos/gestion-restaurante/docs/project/product_backlog.md):

### Bloque 1: Gobernanza, Seguridad y Contexto Multi-Tenant (Prioridad P0 - Bloqueantes)

- [ ] **TASK-018 (Security & Tenancy / Backend & Frontend):** Login Calificado por Identificador de Tenant y Prevención de Colisiones de Usuario.
  - *Caso de Uso:* UC-201 | *Backlog:* PB-019 | *Prioridad:* `P0 - Crítica` | *Rama:* `feat/security-qualified-login`
  - *Objetivo:* Resolver el fallo sistémico donde `findByUsernameOrEmail` busca globalmente colisionando ante nombres duplicados entre empresas (`uq_user_tenant_username`).
  - *Alcance:*
    - Backend: Adaptar `AuthService.login()` para requerir `tenantSlug` (o subdominio). Buscar tenant activo y consultar usuario exclusivamente dentro de `(tenant_id, username)`. Token JWT con claims `tenantId`, `userId`, `role`, `username` y `branchIds`.
    - Frontend: Actualizar formulario en TailwindAdmin v2.0 solicitando Empresa/Código, Usuario y Contraseña. Manejo estructurado de error HTTP 401 (*"Empresa no encontrada o inactiva"*).

- [ ] **TASK-019 (RBAC & Security / Frontend):** Navegación Condicional en Sidebar y Route Guards Estrictos (`roleGuard`).
  - *Caso de Uso:* UC-202 | *Backlog:* PB-018 | *Prioridad:* `P0 - Crítica` | *Rama:* `feat/rbac-frontend-guards`
  - *Objetivo:* Eliminar la visibilidad de opciones restringidas para cajeros y personal de cocina en el menú lateral y bloquear accesos directos por barra de navegación.
  - *Alcance:*
    - Interfaz `NavItem` tipada con `allowedRoles: Role[]`.
    - Agrupación por categorías en Sidebar: *Operaciones Diarias* (POS), *Gestión de Sucursal* (Inventario, Finanzas), *Administración Corporativa* (Catálogo, Empresa, Usuarios).
    - Menú reactivo vía `computed()` conectado a `authService.currentUser()`.
    - Implementar guard funcional `roleGuard` evaluando metadata de ruta `data: { roles: [...] }`. Redirección automática a zona permitida con alerta de acceso denegado.

- [ ] **TASK-020 (Company & Branches / Backend & Frontend):** CRUD Administrativo de Empresa (Tenant) y Sucursales con Huso Horario IANA y Bloqueo Operativo.
  - *Caso de Uso:* UC-203 | *Backlog:* PB-017 | *Prioridad:* `P0 - Crítica` | *Rama:* `feat/company-branches-crud`
  - *Objetivo:* Permitir a la empresa autogestionar su perfil comercial y sucursales sin depender de scripts SQL en base de datos.
  - *Alcance:*
    - Backend: Endpoints REST para perfil de empresa (razón social, NIT, logo para comandas, moneda) y CRUD de sucursales con huso horario IANA obligatorio ([ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_and_timezone_handling.md)). Inicialización automática de `branch_settings` por defecto. Switch de estado operativo (inactivo bloquea aperturas de turno y cobros en POS).
    - Frontend: Pantalla "Configuración de Empresa" y "Gestión de Sucursales" en TailwindAdmin v2.0 con tarjetas, badges de estado y modal de sucursal.

---

### Bloque 2: Operatividad, Contexto y Abastecimiento (Prioridad P1 - Alta)

- [ ] **TASK-021 (Users & Profile / Backend & Frontend):** Gestión de Colaboradores por Tenant, Baja Lógica y Pantalla 'Mi Perfil' con Humanización de Roles.
  - *Caso de Uso:* UC-204 | *Backlog:* PB-018 | *Prioridad:* `P1 - Alta` | *Rama:* `feat/users-management-profile`
  - *Objetivo:* Permitir al administrador de empresa dar de alta a sus empleados con roles y sucursales asignadas, y ofrecer a cada colaborador una vista de perfil amigable con cambio de clave.
  - *Alcance:*
    - Backend: Endpoints CRUD de usuarios (`/api/v1/users`) protegidos por `@PreAuthorize` con asignación en `user_branches` y baja lógica sin borrado físico (`is_active = false`). Endpoints `/api/v1/auth/me` con cambio seguro de contraseña (Argon2id).
    - Frontend: Pantalla "Gestión de Usuarios" con tabla y modal; y pantalla "Mi Perfil" con diccionario humanizado de roles (*"Administrador de Empresa"*, *"Gerente de Sucursal"*, *"Cajero / Atención"*, *"Personal de Cocina"*), badges temáticos TailwindAdmin, edición personal y switch de tema claro/oscuro.

- [ ] **TASK-022 (Context & Topbar / Frontend):** Selector Contextual de Sucursal Activa en Topbar con Signals y Propagación `X-Branch-Id`.
  - *Caso de Uso:* UC-205 | *Backlog:* PB-019 | *Prioridad:* `P1 - Alta` | *Rama:* `feat/topbar-branch-selector`
  - *Objetivo:* Conmutar el contexto de trabajo en vivo para usuarios multisucursal o administradores sin forzar re-login, manteniendo insignia fija para cajeros.
  - *Alcance:*
    - Topbar adaptativo: insignia fija para monosucursal; Dropdown interactivo TailwindAdmin v2.0 para multisucursal con avatar, badge verde/rojo y chevron.
    - Servicio `BranchContextService` reactivo con Signals para almacenar `activeBranch`.
    - Interceptor HTTP que adjunta `X-Branch-Id` en peticiones REST.
    - Recarga reactiva de catálogo, comandas, turnos e inventario al cambiar de local.

- [ ] **TASK-023 (Inventory & Stock / Backend & Frontend):** Alta de Stock Inicial de Sucursal (`INITIAL_STOCK`), Kardex `INITIAL_INVENTORY` y Modal TailwindAdmin.
  - *Caso de Uso:* UC-206 | *Backlog:* PB-021 | *Prioridad:* `P1 - Alta` | *Rama:* `feat/inventory-initial-stock`
  - *Objetivo:* Habilitar formalmente existencias físicas de insumos en sucursales resolviendo la confusión entre catálogo abstracto e inventario local.
  - *Alcance:*
    - Backend: Endpoint para registrar saldo base y costo unitario de apertura. Creación en `branch_stock` y movimiento inmutable en Kardex `INITIAL_INVENTORY`. Regla de integridad: prohibir si el insumo ya cuenta con movimientos previos en la sucursal.
    - Frontend: Modal interactivo guiado en TailwindAdmin v2.0 con selector de insumo maestro, alerta informativa en tono `lightprimary` y confirmación de apertura de libro mayor.

- [ ] **TASK-024 (POS & Pricing / Backend & Frontend):** Descuentos Directos, Cupones en POS, Límite Autónomo y PIN Supervisor.
  - *Caso de Uso:* UC-207 | *Backlog:* PB-020 (Fase 1) | *Prioridad:* `P1 - Alta` | *Rama:* `feat/pos-discounts-coupons`
  - *Objetivo:* Otorgar flexibilidad comercial en caja permitiendo rebajas porcentuales y fijas, cupones y gobernanza de autorizaciones.
  - *Alcance:*
    - Backend: Soporte de descuentos porcentuales (%) y monto fijo en comanda, entidad `coupons`, límite autónomo de cajero (hasta 5%) y requerimiento de PIN de `BRANCH_MANAGER` para descuentos mayores o cortesías al 100%. Persistencia de motivo y autorizador.
    - Frontend: Sección colapsable en carrito del POS, desglose transparente (Subtotal, Descuento en `.text-error`, Total en `.text-link`), input de cupones y modal interactivo para ingreso de PIN supervisor.

- [ ] **TASK-027 (Inventory & Purchases / Backend & Frontend):** Módulo de Recepción de Compras a Proveedores (`PURCHASE`) y Ajustes de Merma / Conteo Físico.
  - *Caso de Uso:* Ciclo de Abastecimiento | *Backlog:* PB-021 | *Prioridad:* `P1 - Alta` | *Rama:* `feat/inventory-purchases-waste`
  - *Objetivo:* Completar el ciclo de abastecimiento de almacén con registro de facturas/recibos, recálculo de costo ponderado móvil y control de mermas.
  - *Alcance:*
    - Backend: Endpoints de compras (`purchases`, `purchase_items`) con recálculo automático de costo unitario promedio ponderado y Kardex `PURCHASE_IN`. Endpoints de mermas (`WASTE_ADJUSTMENT` con cuantificación de pérdida monetaria) y recuento físico (`PHYSICAL_COUNT_ADJUSTMENT` con notas).
    - Frontend: Pantalla de Recepción de Compras con proveedor y factura; y pantalla de Registro de Mermas y Ajustes en TailwindAdmin v2.0.

---

### Bloque 3: Resiliencia y Desacoplamiento Arquitectónico (Prioridad P1 - Alta)

- [ ] **TASK-028 (Architecture / Backend):** Desacoplamiento de Repositorios en Módulos de Negocio mediante Interfaces Públicas `.api`.
  - *Backlog:* PB-011 | *Prioridad:* `P1 - Alta` | *Rama:* `refactor/modular-public-api-decoupling`
  - *Objetivo:* Eliminar la violación modular donde `OrderServiceImpl`, `CashShiftService` y `ExpenseService` inyectan directamente repositorios internos (`BranchRepository`, `AppUserRepository`).
  - *Alcance:*
    - Definir `core.common.api.BranchPublicApi` y `core.security.api.UserPublicApi`.
    - Encapsular repositorios en implementaciones internas y refactorizar servicios de negocio.
    - Validar mediante `ApplicationModules.of(RestaurantApplication.class).verify()`.

- [ ] **TASK-029 (Architecture / Backend & Frontend):** Estandarización de Excepciones de Dominio y Formato ProblemDetail RFC 7807.
  - *Backlog:* PB-012 | *Prioridad:* `P1 - Alta` | *Rama:* `feat/error-handling-problem-detail`
  - *Objetivo:* Reemplazar excepciones genéricas de runtime por jerarquía de dominio estructurada y respuestas uniformes `application/problem+json`.
  - *Alcance:*
    - Jerarquía `DomainException`, `EntityNotFoundException`, `BusinessRuleException` y `@RestControllerAdvice` centralizado en Spring Boot 4.1.x.
    - Interceptor HTTP en Angular 22 para presentación consistente de alertas ante errores de API.

- [ ] **TASK-030 (Architecture / Frontend):** Capa de Repositorios Locales (DAL) para Dexie.js aislando `appDb`.
  - *Backlog:* PB-016 | *Prioridad:* `P1 - Alta` | *Rama:* `refactor/frontend-dexie-dal-layer`
  - *Objetivo:* Aislar la persistencia en IndexedDB mediante clases de repositorio tipadas en Angular, desacoplando `PosService` y `FinanceService` de la instancia global `appDb`.
  - *Alcance:*
    - Crear `LocalOrderRepository`, `LocalCatalogRepository`, `LocalShiftRepository` y `LocalExpenseRepository` en `core/repositories/`.
    - Refactorizar servicios para inyectar repositorios locales mediante `inject()`, facilitando pruebas unitarias aisladas.

---

### Bloque 4: Ofertas Avanzadas y Combos (Prioridad P2 - Media / Deseable)

- [ ] **TASK-025 (Catalog & POS / Backend & Frontend):** Gestión y Venta de Combos (Packs Gastronómicos) con Deducción Recursiva en Kardex.
  - *Caso de Uso:* UC-208 | *Backlog:* PB-020 (Fase 2) | *Prioridad:* `P2 - Media` | *Rama:* `feat/catalog-pos-combos`
  - *Objetivo:* Habilitar la comercialización de paquetes de platos y bebidas a precio preferencial con consumo atómico de ingredientes.
  - *Alcance:*
    - Backend: Entidades de combo (`dish_type = 'COMBO'`, `combo_items`). Listener de inventario que ante `OrderPaidEvent` resuelve recursivamente recetas y deduce todos los ingredientes en una única transacción atómica.
    - Frontend: Administración de combos en Catálogo y selector interactivo de armado de packs en POS.

- [ ] **TASK-026 (POS & Pricing / Backend & Frontend):** Motor de Promociones Temporales Automatizadas (Happy Hour) con Cálculo en Huso Horario Local.
  - *Caso de Uso:* UC-209 | *Backlog:* PB-020 (Fase 2) | *Prioridad:* `P2 - Media` | *Rama:* `feat/pos-happy-hour-promotions`
  - *Objetivo:* Automatizar ofertas por días de la semana y ventanas horarias sin intervención manual del cajero.
  - *Alcance:*
    - Backend: Reglas de promoción evaluadas en base a `branches.timezone` ([ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_and_timezone_handling.md)).
    - Frontend: Detección y aplicación automática de precios de oferta en comanda durante el horario de la promoción.


