---
title: "Sprint 6 Backlog — Infraestructura Cloud OCI, Storage S3, DevSecOps, Backoffice SaaS y Estabilización MVP v1"
category: "sprints"
sprint: 6
status: "in-progress"
tags:
  - tasks
  - sprint-backlog
  - sprint-6
  - devops
  - devsecops
  - backoffice
  - react
  - mvp-v1
last_updated: "2026-11-17"
---

# ☁️ Sprint 6 Backlog — Histórico de Tareas Completadas

> **Estado del Sprint:** `EN CURSO — ESTABILIZACIÓN COMPLETADA EN release/sprint-6`  
> **Tareas Activas / Siguientes:** [`docs/tasks.md`](../../tasks.md)  
> **Documento Maestro de Planificación:** [`sprint_06_planning.md`](./sprint_06_planning.md)  
> **Reporte de Ejecución y Certificación:** [`reporte_ejecucion_sprint6.md`](../../testing/historico/reporte_ejecucion_sprint6.md)  
> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./README.md) | [Product Backlog (Por Prioridades)](../product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1](./sprint_01_backlog.md) | [Sprint 2](./sprint_02_backlog.md) | [Sprint 3 (`v0.3.0`)](./sprint_03_backlog.md) | [Sprint 4](./sprint_04_backlog.md) | [Sprint 5 (`v0.5.0`)](./sprint_05_backlog.md)

> [!NOTE]
> **Trazabilidad de IDs en Sprint 6:** El identificador **`TASK-619` fue omitido deliberadamente** durante el triage de `observaciones_y_hallazgos.md` (salto intencional entre `TASK-618` y `TASK-620`). Las tareas abiertas restantes (`TASK-609` OCI en paralelo y backlog de versión futura `TASK-636`–`TASK-640`) permanecen en [`docs/tasks.md`](../../tasks.md).

---

## ✅ Tareas Completadas en Sprint 6

### Fase 1 — Fundación Cloud, Backoffice SaaS, Storage Agnóstico, DevSecOps y GitFlow

- [x] **TASK-601 [INFRA/SPIKE] [ARQUITECTURA]:** Investigación Cloud OCI, Topología de BD Separada, Reverse Proxy, Visor de Logs y Perfiles Twelve-Factor (`PB-025`).
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* [ADR-0026](../../adr/0026_cloud_infrastructure_and_deployment_stack.md) aceptado, perfiles Twelve-Factor + HikariCP explícito + CORS externalizado configurados y seeders aislados bajo `@Profile({"dev", "test"})`.

- [x] **TASK-602 [STARTUP/SEGURIDAD] [ARQUITECTURA]:** Arranque Limpio Go-Live sin Tenancies Demo y Desacoplamiento de Identidad `SUPERADMIN` (`PB-057` / `PB-002`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* [ADR-0027](../../adr/0027_platform_identity_and_clean_golive_bootstrap.md) aceptado, migración `V17` validada, aislamiento JWT por audiencia (`platform-backoffice` vs `erp-tenant`) y `RestaurantModularityTests` pasando.

- [x] **TASK-603 [STORAGE] [ARQUITECTURA]:** Motor Agnóstico de Almacenamiento `core.storage` (Local + S3/R2/OCI Object Storage) y Parámetros Dinámicos (`PB-035`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* [ADR-0028](../../adr/0028_agnostic_media_storage_and_platform_settings.md) aprobado, migración `V18` (`platform_settings`), validación *magic bytes* y consumo exclusivo de `MediaStorageService` vía `.api`.

- [x] **TASK-604 [BACKOFFICE BE] [SEGURIDAD]:** Módulo Spring Modulith Supra-Tenant (`modules.backoffice`), Planes/Suscripciones, Cuotas, Telemetría Event-Driven y Observabilidad Actuator (`PB-002` / `ADR-0029`).
  - *Prioridad:* `P0 - Crítica`
  - *Especificación SDD:* [`docs/backoffice/`](../../backoffice/README.md)
  - *Criterio de Entrega:* Migración `V19` implementada, `core.entitlements`, ledger FIFO de pagos, loggers en caliente vía Actuator y `ApplicationModules.verify()` sin violaciones.

- [x] **TASK-605 [BACKOFFICE FE] [UI/UX]:** Aplicación Web Independiente de Backoffice en React 19 LTS + Tailwind CSS (`PB-003` / [`04_backoffice_frontend_react_spec.md`](../../backoffice/sdd/04_backoffice_frontend_react_spec.md)).
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* Carpeta independiente `backoffice-frontend/` completada con rutas de plataforma, `env.js` en runtime y `frontend/` Angular libre de código Backoffice.

- [x] **TASK-606 [DEVOPS/DEPLOY] [INFRA]:** Dockerfiles Multi-Stage Non-Root, Rotación de Logs, BD Separada y Backups Cifrados a Object Storage (`PB-025`).
  - *Prioridad:* `P1 - Alta`
  - *Nota de Alcance:* El aprovisionamiento y rollout vivo en Oracle Cloud (OCI) se gestiona en paralelo en **`TASK-609`**.
  - *Criterio de Entrega:* Stack `docker-compose.testing-local.yml` levantando servicios saludables detrás de Caddy + Dozzle, rotación `json-file` + Logback y backup/restore drill GPG AES-256 verificado.

- [x] **TASK-607 [DEVSECOPS] [SEGURIDAD]:** Auditoría Local de Secretos, Vulnerabilidades SAST/SCA y Escaneo de Imágenes Docker (`PB-058`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* `scripts/security/run-security-scan.sh`, `.gitleaks.toml`, SpotBugs/FindSecBugs limpio, `npm audit --omit=dev` limpio y Trivy sobre FS/imágenes Docker con 0 vulnerabilidades `HIGH`/`CRITICAL` con fix.

- [x] **TASK-608 [CI/CD & QA] [CALIDAD]:** Workflows GitHub Actions sin Desperdicio de Minutos y Certificación Local Dockerizada (`PB-025`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Workflows CI/CD implementados, stack Docker completo validado en local y certificado en [`reporte_ejecucion_sprint6.md`](../../testing/historico/reporte_ejecucion_sprint6.md).

- [x] **TASK-609 [CLOUD / IaC] [OCI]:** Provisión Guiada de Oracle Cloud Infrastructure (VCN, IAM, Compute, Object Storage, Presupuestos) y Certificación Smoke Staging Nube (`PB-025`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Compartment `cmp-erp`, gobernanza CIS (`ops-erp` + `svc-erp-s3`), Budget `$1.00 USD`, VCN Zero-Trust (`VM 1` `129.80.29.124` + `VM 2` `10.0.1.20` privada), PostgreSQL 16 (`erp_prod` + `erp_staging`) con backup GPG hacia `erp-db-backups`, buckets `erp-media-*`, IaC OpenTofu + `cloud-init` en [`infra/oci/`](../../../infra/oci/README.md) y primer despliegue HTTPS Staging (`v0.6.0-rc.1`) certificado en `tecton.duckdns.org`.

- [x] **TASK-610 [DEVOPS/CI-CD] [GITFLOW]:** Redefinición de Pipelines GitHub Actions por Ambiente (`dev`, `testing-local`, `staging`, `prod`) y Reglas de Protección (`PB-025`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Workflows `.github/workflows/{ci,cd-staging,cd-production}.yml` alineados (disparo Staging por tags `vX.Y.Z-rc.N`/`-staging.N`, Producción por tag `vX.Y.Z` en `main` con aprobación manual, cero ejecuciones por push directo a `develop`/`release/*`).

---

### Fase 2 — Documentación Operativa y Pre-Bootstrap

- [x] **TASK-611 [DOCS/MANUAL] [ONBOARDING-OPS]:** Manual de Bootstrap desde Cero (`testing-local` / Go-Live), Primeras Configuraciones en Backoffice y Actualización del `README.md` Principal.
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* Publicación de [`bootstrap_y_operacion_inicial.md`](../../manuals/plataforma/bootstrap_y_operacion_inicial.md) y actualización del [`README.md`](../../../README.md) raíz para las 3 aplicaciones y 4 ambientes.

- [x] **TASK-612 [BUG/ERP FE] [SEGURIDAD/UX]:** Ocultar Accesos Rápidos Demo y Prellenado `demo-restaurant` en Login ERP fuera de `dev`.
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Condicionamiento mediante `isDevMode()` en `LoginComponent` y cobertura unitaria en `login.component.spec.ts`.

---

### Fase 3 — Remediación UAT Backoffice y ERP (`OBS-001`–`OBS-012`)

- [x] **TASK-614 [BUG/CAMBIO] [ERP - SUCURSALES] (`OBS-003`):** Corregir Flujo de Creación de Nueva Sucursal, Inicialización y Actualización Reactiva del Selector de Sucursal.
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Refresco automático en `BranchContextService` al crear, editar o alternar sucursales sin recargar página.

- [x] **TASK-615 [BUG] [ERP - CONFIGURACIÓN] (`OBS-001`):** Corregir Alerta `Unknown Error` en Parámetros del Sistema al Navegar entre Empresa y Sucursales.
  - *Prioridad:* `P2 - Media`
  - *Criterio de Entrega:* Manejo de `HTTP 304 Not Modified` en `errorInterceptor` sin emitir falsa alerta de error en `/settings/parameters`.

- [x] **TASK-616 [MEJORA/UI-UX] [BACKOFFICE - OBSERVABILIDAD] (`OBS-007`):** Buscador en Loggers en Caliente + Filtro por Detalle, Visor JSON Formateado y Paginación Real en Auditoría Inmutable.
  - *Prioridad:* `P2 - Media`
  - *Criterio de Entrega:* Búsqueda multicriterio en `PlatformAuditSearchTest.java` y visor paginado con detalle JSON en `ObservabilityPage.tsx`.

- [x] **TASK-617 [CAMBIO/UX] [BACKOFFICE - INQUILINOS] (`OBS-009`):** Hacer Opcionales Dirección de Sucursal y Email del Admin al Aprovisionar Nuevo Inquilino.
  - *Prioridad:* `P2 - Media`
  - *Criterio de Entrega:* Aprovisionamiento consistente con el self-onboarding del ERP en backend y `TenantsPage.tsx`.

- [x] **TASK-618 [MEJORA/CAMBIO] [BACKOFFICE - INQUILINOS] (`OBS-010`):** Estado Inicial Diferenciador para Inquilinos Recién Aprovisionados Sin Primer Ingreso.
  - *Prioridad:* `P2 - Media`
  - *Criterio de Entrega:* Migración `V20__tenant_first_login_tracking.sql`, tracking de primer login y badge *Pendiente de activación / Sin primer ingreso* verificado en `BackofficeTenantFirstLoginTest.java`.

- [x] **TASK-620 [ARQ/REFACTOR] [FRONTENDS] (`OBS-005`):** Extracción de Primitivas UI Reutilizables en Backoffice (`Modal`, `Card`, `EmptyState`, `Badge`) y Regla de Reutilización Frontend.
  - *Prioridad:* `P2 - Media`
  - *Nota de Numeración:* Salto intencional previo en `TASK-619` (ID no asignado).
  - *Criterio de Entrega:* Primitivas compartidas en `backoffice-frontend/src/components/` con pruebas en `UiPrimitives.test.tsx`.

- [x] **TASK-621 [MEJORA/UX] [ERP - ONBOARDING] (`OBS-002`):** Confirmación de Contraseña en Self-Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Criterio de Entrega:* Validación de coincidencia en `/onboarding` verificada en `onboarding.component.spec.ts`.

- [x] **TASK-622 [UI/UX] [BACKOFFICE] (`OBS-004`):** Soporte de Modo Oscuro Persistente en `backoffice-frontend/`.
  - *Prioridad:* `P3 - Baja`
  - *Criterio de Entrega:* `ThemeProvider` con variables CSS y pruebas en `ThemeProvider.test.tsx`.

- [x] **TASK-623 [MEJORA/UX] [BACKOFFICE - PLANES] (`OBS-006`):** UX de Límites Ilimitados Gratis y Categorización/Visibilidad de Planes para Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Criterio de Entrega:* Migración `V21__subscription_plans_category_and_unlimited.sql` (`plan_category`, `duration_days = 0`), cobertura en `SubscriptionPlanCategoryAndUnlimitedTest.java` y `PlansPage.test.tsx`.

- [x] **TASK-624 [CAMBIO/MEJORA] [BACKOFFICE - ADMINS] (`OBS-008`):** Email Opcional y Política de Contraseñas Configurable para Administradores de Plataforma.
  - *Prioridad:* `P3 - Baja`
  - *Criterio de Entrega:* Migración `V22__platform_admin_optional_email_and_password_policy.sql`, `PlatformAdminOptionalEmailAndPasswordPolicyTest.java` y `AdminsPage.test.tsx`.

- [x] **TASK-625 [CRÍTICA] [BACKOFFICE - INQUILINOS/USUARIOS]:** Listado, Vista de Estado y Gestión de Usuarios del Tenant (incluyendo Reseteo de Contraseña) en `/tenants/:id`.
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* Pestaña `TenantUsersTab` en `/tenants/:id`, reseteo temporal con `mustChangePassword` y pruebas en `BackofficeTenantUsersTest.java` y `TenantUsersTab.test.tsx`.

- [x] **TASK-626 [BUG/CRÍTICA] [ERP - FINANZAS] (`OBS-011`):** Corregir `500 Internal Server Error` (`could not determine data type of parameter $4`) en Finanzas y Arqueo (`/finance`).
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* Sustitución de filtros JPQL con parámetros nulos por `JpaSpecificationExecutor` en `CashShiftService` y pruebas en `CashShiftServiceTest.java`.

- [x] **TASK-627 [MEJORA/UX] [ERP - POS] (`OBS-012`):** Advertencia de Confirmación al Abrir Turno sin Platos en el Catálogo.
  - *Prioridad:* `P3 - Baja`
  - *Criterio de Entrega:* Doble confirmación explícita en POS verificada en `pos.component.spec.ts`.

---

### Fase 4 — Estabilización MVP v1 (`TASK-628`–`TASK-635` + Pulido E2E) (`PB-060`)

- [x] **TASK-628 [BUG/CRÍTICA] [AUTH - USUARIOS] (`OBS-013`):** Exigir Cambio Obligatorio de Contraseña (`mustChangePassword`) en Primer Login / Post-Reset desde Backoffice.
  - *Prioridad:* `P0 - Crítica`
  - *Criterio de Entrega:* Port `PasswordChangePolicyPort` conectado al parámetro `security.enforce_password_change_on_reset`, bloqueo de navegación hacia `/profile` en guardias ERP y pruebas en `UserIntegrationTests.java` y `auth.guard.spec.ts`.

- [x] **TASK-629 [BUG/INFRA] [STORAGE - CATÁLOGO/POS] (`OBS-015`):** Corregir Visualización de Imágenes (`/uploads/dishes/...`) en Todos los Ambientes y Documentar Requisitos de Storage.
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* [ADR-0033](../../adr/0033_spa_runtime_env_and_cross_origin_media.md), resolución de URLs relativas contra `API_BASE_URL` en `runtime-env.ts`, proxy `/uploads/*` en `Caddyfile.testing-local` y `Caddyfile.cloud`, y actualización de [`bootstrap_y_operacion_inicial.md`](../../manuals/plataforma/bootstrap_y_operacion_inicial.md).

- [x] **TASK-630 [UI/UX] [ERP - POS & CATÁLOGO] (`OBS-018`):** Pulido Final de POS (Comprobante) y Depuración de Jerga Técnica Orientada a Código.
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Eliminación de términos técnicos (*"Registrado en servidor"*, *"Receta BOM"*) en Catálogo, POS y Control de Caja por lenguaje claro de negocio.

- [x] **TASK-631 [MEJORA/FUNC] [ERP - CATÁLOGO] (`OBS-014`, `OBS-016`):** Edición/Eliminación Segura de Categorías y Edición Completa de Platos en `/catalog`.
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Endpoints `PUT/DELETE` en `CatalogController`, formularios de edición en `CatalogComponent` y pruebas en `CatalogIntegrationTest.java` y `catalog.component.spec.ts`.

- [x] **TASK-632 [BUG/DISEÑO] [ERP - CATÁLOGO] (`OBS-017`):** Corrección Visual de *"Precios de mi Sucursal"* y Simplificación del Flujo Multi-Sucursal (`catalog.scope_mode`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* [ADR-0030](../../adr/0030_catalog_branch_isolation_by_default.md), aislamiento por sucursal por defecto (`BRANCH_ISOLATED`), reparación de tabla al alternar precios locales y clonado explícito hacia sucursal destino.

- [x] **TASK-633 [ARQ/ESTUDIO] [BACKEND/BD] (`OBS-022`):** Estudio Arquitectónico de Modelo Flexible para Catálogo Dinámico (Relacional vs Híbrido `JSONB` en PostgreSQL).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Decisión consensuada de arquitectura relacional con extensiones estructuradas para jornadas compartidas, combos y selecciones sin motor NoSQL adicional.

- [x] **TASK-634 [FUNC/DISEÑO] [ERP - CATÁLOGO/POS] (`OBS-019`, `OBS-020`, `OBS-021`):** Jornada Compartida de Sucursal, Gestor de Combos Dinámicos (`MENU_COURSE`), Complementos y Menú del Día Generalizado (`PB-060`).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* [ADR-0031](../../adr/0031_branch_service_sessions_and_dynamic_combos.md), migraciones `V23` (`branch_service_sessions`, `daily_menu_combos`, `order_item_selections`) y `V24` (`day_of_week` `VARCHAR(120)`), parámetro `finance.auto_open_full_catalog_session`, cupos ilimitados (`is_unlimited`), restricción `allow_individual_sale`, CRUD completo en Catálogo/Menús, selector POS de combos por tiempos y deducción atómica en Kardex/cupos (`DailyMenuAndServiceSessionIntegrationTests.java`).

- [x] **TASK-635 [UX/SANEAMIENTO] [ERP - INSUMOS/INVENTARIO] (`OBS-023`):** Saneamiento Mínimo de Alta y Edición de Insumos para MVP v1.
  - *Prioridad:* `P2 - Media`
  - *Criterio de Entrega:* Edición/eliminación segura de insumos (`PUT/DELETE /ingredients/{id}`), distinción explícita Stock Inicial vs Ajuste de Conteo y cálculo de costo unitario desde Valor Total del Lote en `/inventory`.

- [x] **Pulido Post-E2E & Regresión Final MVP v1 (`82ecd5e` / `b6cc737` / `3cc923d`):**
  - *Alcance:* Resolución de bloqueantes detectados en verificación E2E: cambio recibido (`Order.changeAmount`) y timestamp en comprobante POS, reglas de visibilidad RBAC en sidebar/guards (`anyPermissions`), validación condicional de email en gestión de usuarios (`security.require_user_email`), ajuste de `daily_menus.day_of_week` (`V24`) y apilamiento responsive de tarjetas/tablas en `/catalog`, `/catalog/menus`, `/pos` y `/users`.

- [x] **TASK-613 [QA/DOCS] [CIERRE-SPRINT]:** Auditoría, Depuración y Sincronización Total de Documentación (SDD, Bóveda Obsidian sin `[[wikilinks]]`, Manuales Cliente/Plataforma, Procesos de Negocio y Definiciones Agénticas).
  - *Prioridad:* `P1 - Alta`
  - *Criterio de Entrega:* Retiro de `docs/temp/`, `docs/project/product_backlog.md`, `devops_infra_backlog.md` y 10 diagramas redundantes de `04-behavior-and-flows/`; normalización YAML Obsidian (sin `[[wikilinks]]`, 0 huérfanos y 0 enlaces rotos sobre 130 documentos en `docs/`); creación de [`ADR-0030`](../../adr/0030_catalog_branch_isolation_by_default.md)–[`ADR-0033`](../../adr/0033_spa_runtime_env_and_cross_origin_media.md), 7 guías cliente en [`docs/manuals/restaurante/`](../../manuals/restaurante/README.md) + manuales plataforma en [`docs/manuals/plataforma/`](../../manuals/plataforma/bootstrap_y_operacion_inicial.md), incorporación de `PB-060`–`PB-063` / `TASK-639`–`TASK-640` e inventario QA exacto (`213` BE / `390` ERP / `22` Backoffice / `34` E2E).
