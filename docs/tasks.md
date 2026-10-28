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
- E2E: `./scripts/run-e2e.sh` (100% pruebas de regresión Playwright pasando).
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

## Sprint 2 — Tareas Completadas

- [x] **TASK-201 (Security & Tenancy / Backend & Frontend):** Implementar Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa (PB-019 / UC-201). Soporte de `tenantIdentifier` (`slug` o `nit_or_tax_id`), migración Flyway `V3__tenant_slug_and_rbac.sql` con RBAC granular, token JWT con claims `tenant_slug`, `permissions` y `branch_ids`. Soporte de cabecera `X-Branch-Id` en `JwtAuthenticationFilter` con validación multi-tenant (HTTP 400/403 ProblemDetail), servicio reactivo `BranchContextService` con Angular Signals, interceptor HTTP inyectando `X-Branch-Id` y selector interactivo de sucursal en el Topbar con conmutación dinámica y vista limpia sin selector para usuarios de sucursal única.
- [x] **TASK-202 (Company & Branches / Backend & Frontend):** Implementar módulo `modules.company` en Spring Modulith con `CompanyPublicApi` y `BranchPublicApi`, endpoints REST para Tenants y Branches (respetando ADR-0006 de huso horario `branches.timezone`), `GlobalExceptionHandler` RFC 7807 (`ProblemDetail`) y vistas de administración de Empresa y Red de Sucursales en TailwindAdmin v2.0.
- [x] **TASK-203 (RBAC & Guards / Frontend):** Implementar Route Guards (`authGuard`, `guestGuard`, `roleGuard`, `permissionGuard`) en Angular 22 Zoneless con soporte de permisos granulares por recurso (`pos:access`, `inventory:read`, `finance:shifts`, `catalog:read`, `company:read`) y Sidebar reactivo agrupado por perfil con badges humanizados de rol en TailwindAdmin v2.0.
- [x] **TASK-204 (Users & Profile / Backend & Frontend):** Implementar Gestión de Personal y 'Mi Perfil' (PB-018 / UC-204) con desacoplamiento modular (PB-011). Creación de contrato público `UserPublicApi` en `core.security.api`, endpoints REST `/api/v1/users` y `/api/v1/profile` con hashing Argon2id, validación de unicidad por tenant y protección RBAC (`users:read`, `users:write`). Refactorización de `CashShiftService` y `ExpenseService` para inyectar `UserPublicApi`. Vistas en Angular 22 / TailwindAdmin v2.0 para administración de colaboradores con modal CRUD y panel "Mi Perfil" con actualización de email y cambio seguro de contraseña.
- [x] **TASK-206 (Inventory & Kardex / Backend & Frontend):** Alta de Stock Inicial de Sucursal (`INITIAL_STOCK`) y apertura en Kardex (PB-021 / UC-206). Registro atómico de saldo base y costo unitario en `branch_stock`, inserción de movimiento `INITIAL_STOCK` en `kardex_movements` y validación de conflicto estricta (`HTTP 409 Conflict`) si el insumo ya cuenta con movimientos previos en la sucursal. Vistas en Angular 22 / TailwindAdmin v2.0 con modal de apertura y selección de sucursal contextualizada.
- [x] **TASK-208 (Catalog & POS / Backend & Frontend):** Gestión y venta de Combos gastronómicos flexibles (*Packs*) en catálogo y POS con deducción recursiva en Kardex (PB-020 / UC-208). Definición de entidades `Combo`, `ComboItem`, `ComboItemOption` con modos `FIXED` y `CHOICE`. Integración en POS con modal de personalización interactiva de opciones y selección de platos componentes. Soporte en venta POS y publicación en `OrderPaidEvent` para deducción atómica y recursiva de todas las recetas BOM de los componentes en `KardexService`. Vistas en TailwindAdmin v2.0 y pruebas unitarias/integración al 100%.
- [x] **TASK-209 (POS & Pricing / Backend & Frontend):** Motor de Promociones Temporales Parametrizables (*Happy Hour*, ofertas por horario y día según huso local de sucursal) (PB-020 / UC-209). Entidades `Promotion` y `PromotionBranch`, motor `PromotionEngine` evaluando vigencia temporal contra el huso horario local de la sucursal (`branches.timezone` según ADR-0006), días de semana y ventana horaria. Modos `AUTOMATIC` y `MANUAL`, cálculo y desglose de descuentos porcentuales o de monto fijo, soporte en órdenes y DTOs, banner interactivo en POS y resumen de cobro con desglose reactivo. 100% de pruebas unitarias e integración en backend y frontend.
- [x] **TASK-210 (Catalog & POS / Backend & Frontend):** Grupos de Modificadores y extras de platos con recargo de precio y deducción en Kardex (PB-020 / UC-210). Definición de grupos de modificadores (`modifier_groups`, `modifiers`, `dish_modifier_groups`), personalización opcional u obligatoria con validación de selección mínima y máxima (`minSelectable`, `maxSelectable`). Interfaz POS para selección interactiva de extras al agregar platos al pedido, recargo acumulado en precio unitario/subtotal y persistencia de extras en `order_item_modifiers`. Publicación de extras con consumo de ingredientes en `OrderPaidEvent` y deducción atómica complementaria en `KardexService`. 100% de pruebas unitarias e integración en backend y frontend.
- [x] **TASK-211 (QA & Testing / E2E Automation):** Suite de Pruebas Automatizadas End-to-End (E2E) con Playwright y Chrome DevTools Protocol (CDP). Creación de pruebas para ciclo completo de turno y arqueo (apertura con fondo inicial por cajero, venta multilínea con combos y modificadores, descuentos Happy Hour, emisión de tickets, solicitud de egresos de caja menor con marca de gaveta, autorización de egresos por administrador con RBAC estricto, conciliación física de efectivo con cuadre perfecto y cierre de turno para siguiente jornada). Script maestro `./scripts/run-e2e.sh` para ejecución de regresión a demanda, manual operativo de pruebas y matriz de inventario en `docs/testing/`. 25/25 pruebas E2E pasando al 100%.

---

## Tareas de Infraestructura, Contenerización y Despliegue (DevOps / Preproducción & Producción)

Estas tareas están desacopladas del avance funcional de los sprints de negocio y definen el proceso de certificación, empaquetado y puesta en marcha del software en la nube (VPS / Oracle Cloud):

- [ ] **TASK-301 (DevOps / Docker Packaging):** Multi-Stage Dockerfiles para Backend (Java 25) y Frontend (Angular 22) + Nginx SPA Gateway.
  - *Categoría:* DevOps & Contenerización | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear `backend/Dockerfile` multi-stage optimizado (compilación Maven + runtime JRE Eclipse Temurin 25 Alpine recortado con flags de contención `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`) y `frontend/Dockerfile` multi-stage (Node 22 build + Nginx Alpine) configurando proxy pass de `/api/*` hacia el backend y resolución de rutas SPA (`try_files $uri $uri/ /index.html;`).
  - *Criterio de Entrega:* Imágenes Docker generadas localmente, con peso inferior a 200MB en backend y menos de 30MB en frontend.

- [ ] **TASK-302 (Config & Security / Backend):** Externalización de Configuración y Seguridad (12-Factor App & Actuator Health).
  - *Categoría:* Seguridad & Configuración Cloud | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Parametrizar orígenes CORS en `SecurityConfig` desde variable de entorno (`CORS_ALLOWED_ORIGINS`), obligar inyección de clave JWT segura (`JWT_SECRET`) evitando el fallback por defecto en perfiles de producción, e incorporar endpoints de salud `spring-boot-starter-actuator` (`/actuator/health/liveness`, `/actuator/health/readiness`) para monitoreo y reinicio automático de contenedores.
  - *Criterio de Entrega:* Perfil Spring `prod` activable con validación estricta de variables obligatorias y probes de salud respondiendo `UP`.

- [ ] **TASK-303 (DevOps / Local Simulation):** Orquestación y Ambiente de Certificación Local (`docker-compose.staging.yml`).
  - *Categoría:* QA Infraestructura & Certificación | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear un stack completo reproducible en local mediante `docker/docker-compose.staging.yml` que levante: 1) Base de datos PostgreSQL con migraciones Flyway automáticas, 2) Backend Spring Boot, 3) Frontend con Nginx sirviendo estáticos y ruteando la API en el puerto 80/443. Ejecutar las 25 pruebas E2E de Playwright apuntando contra este entorno simulado en contenedor.
  - *Criterio de Entrega:* El sistema completo levanta con un solo comando (`docker compose -f docker/docker-compose.staging.yml up -d`) y supera el 100% de las pruebas E2E contra contenedores.

- [ ] **TASK-304 (Cloud & Storage / Evidence & Media):** Abstracción de Almacenamiento de Evidencias Digitales (S3 / Object Storage).
  - *Categoría:* Cloud Storage & Stateless Backend | *Prioridad:* `P2 - Media`
  - *Objetivo:* Implementar una abstracción de almacenamiento (`StorageService`) para comprobantes de gastos (`evidence_url`), fotos y logos, desacoplando el sistema de archivos del contenedor mediante un cliente compatible con S3 (ej. Cloudflare R2 u OCI Object Storage) para garantizar persistencia y portabilidad sin estado.
  - *Criterio de Entrega:* Subida de comprobante de gasto en `/api/v1/expenses` guarda la URL del bucket y no escribe en el disco local de la aplicación.

- [ ] **TASK-305 (Cloud & Ops / Provisioning):** Despliegue en Preproducción / Producción en la Nube (VPS / Oracle Cloud Always Free).
  - *Categoría:* Rollout & Operaciones | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Aprovisionar la instancia de servidor (Oracle Cloud ARM Ampere Free Tier o VPS Linux x86), configurar firewall estricto (UFW / OCI Security Lists abriendo solo puertos 22 con SSH Key, 80 y 443), aprovisionar certificados SSL/TLS automáticos con Let's Encrypt (vía Nginx/Certbot o Caddy) y orquestar el despliegue con Docker Compose.
  - *Criterio de Entrega:* Aplicación accesible públicamente mediante dominio/subdominio HTTPS con certificados válidos y base de datos con aislamiento de red no expuesta a internet.

- [ ] **TASK-306 (Ops / Disaster Recovery):** Automatización de Respaldos (Backups) y Restauración de Base de Datos PostgreSQL.
  - *Categoría:* Continuidad de Negocio | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Configurar tarea programada (cron job) diaria en el servidor host que ejecute `pg_dump` con compresión de la base de datos `restaurant_db`, cifre el volcado y lo transfiera a un almacenamiento externo seguro (Object Storage/bucket S3 independiente) con política de retención de 30 días, documentando el procedimiento de restauración (`pg_restore`).
  - *Criterio de Entrega:* Script de backup automatizado probado y procedimiento de disaster recovery verificado con una restauración exitosa en un entorno limpio.

- [ ] **TASK-307 (DevOps & CI/CD / GitHub Actions):** Pipeline de Integración y Despliegue Continuo (CI/CD) hacia Oracle Cloud.
  - *Categoría:* CI/CD & Automatización DevOps | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Crear workflow `.github/workflows/deploy.yml` que, ante un push o merge a la rama de lanzamiento (`main` o tags de release), compile los artefactos, construya y empaquete las imágenes Docker (publicándolas en GitHub Packages / GHCR o transfiriéndolas directamente vía SSH), conecte de forma segura al host de Oracle Cloud usando GitHub Secrets (`SSH_PRIVATE_KEY`, `HOST`, `USER`) y ejecute la actualización con downtime mínimo (`docker compose pull && docker compose up -d`).
  - *Criterio de Entrega:* Despliegue 100% automatizado y sin intervención manual tras la aprobación del código en GitHub, con tiempo de ejecución dentro de los minutos gratuitos de GitHub Actions.

---

## Tareas en Ejecución y Próximas (Sprint 2 - Bloque P1)
  - *Caso de Uso:* UC-207 | *Backlog:* PB-020 | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Habilitar descuentos manuales por porcentaje o monto fijo en comanda, validando topes máximos por rol y soporte de cupones de descuento.

- [ ] **TASK-227 (Inventory & Purchases / Backend & Frontend):** Módulo de Recepción de Compras a Proveedores (`PURCHASE`) y Ajustes de Merma / Conteo Físico.
  - *Caso de Uso:* Ciclo de Abastecimiento | *Backlog:* PB-021 | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Completar el ciclo de abastecimiento de almacén con registro de facturas/recibos, recálculo de costo ponderado móvil y control de mermas.

- [ ] **TASK-228 (Architecture / Backend):** Desacoplamiento de Repositorios en Módulos de Negocio mediante Interfaces Públicas `.api`.
  - *Backlog:* PB-011 | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Eliminar la inyección directa de repositorios internos (`BranchRepository`, `AppUserRepository`) mediante `BranchPublicApi` y `UserPublicApi`.

- [ ] **TASK-229 (Architecture / Backend & Frontend):** Estandarización de Excepciones de Dominio y Formato ProblemDetail RFC 7807.
  - *Backlog:* PB-012 | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Jerarquía `DomainException` y `@RestControllerAdvice` centralizado con interceptor HTTP en frontend.

- [ ] **TASK-230 (Architecture / Frontend):** Capa de Repositorios Locales (DAL) para Dexie.js aislando `appDb`.
  - *Backlog:* PB-016 | *Prioridad:* `P1 - Alta`
  - *Objetivo:* Aislar la persistencia en IndexedDB mediante clases de repositorio tipadas en Angular.
