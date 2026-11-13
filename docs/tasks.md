---
title: "Sprint 6 Backlog — Infraestructura Cloud OCI, Storage S3, DevSecOps y Backoffice SaaS en React (Sprint Activo)"
category: "tasks"
sprint: 6
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-6
  - devops
  - devsecops
  - backoffice
  - react
  - execution-plan
last_updated: "2026-11-12"
---

# ☁️ Sprint 6 Backlog — Infraestructura Cloud OCI, Storage S3, DevSecOps y Backoffice SaaS en React

> **Rama Activa:** `feat/sprint-6` (aislada respecto al núcleo ERP en `develop`)  
> **Documento Maestro de Planificación:** [`sprint_06_planning.md`](./project/sprints/sprint_06_planning.md)  
> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./project/sprints/README.md) | [Product Backlog (Por Prioridades)](./project/product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1](./project/sprints/sprint_01_backlog.md) | [Sprint 2](./project/sprints/sprint_02_backlog.md) | [Sprint 3 (`v0.3.0`)](./project/sprints/sprint_03_backlog.md) | [Sprint 4](./project/sprints/sprint_04_backlog.md) | [Sprint 5 (`v0.5.0`)](./project/sprints/sprint_05_backlog.md)

---

## 📋 Reglas de Ejecución, Alcance y Protocolo de Calidad (DoD)

1. **Aislamiento de Alcance del ERP:** No modificar la lógica operativa del restaurante (`modules.pos`, `modules.catalog` negocio, `modules.finance`, `modules.inventory` ni `frontend/` funcional); los hallazgos pendientes de Sprint 5 ([`observaciones_y_hallazgos.md`](./observaciones_y_hallazgos.md)) se corrigen de forma independiente en `develop`.
2. **Ciclo SDD-SDLC Interactivo (ADR-0007 + Reglas 8 y 9 `AGENTS.md`):** Cada tarea `TASK-60X` ejecutará su propio ciclo de **Fase 1 (Análisis/Investigación + Co-Diseño)** $\rightarrow$ **Fase 2 (Spec-First)** $\rightarrow$ **Fase 3 (Construcción)** $\rightarrow$ **Fase 4 (Sincronización `doc-architect`)** antes de darse por cerrada.
3. **Optimización de Recursos Cloud y Secretos:** Cero sobre-ingeniería (evitar K8s si Docker Compose + Proxy SSL es óptimo en OCI), **Twelve-Factor App** estricto para credenciales y **rotación obligatoria de logs** en todos los componentes.
4. **Quality Gates:**
   - Backend: `./mvnw test` (100% tests + `RestaurantModularityTests` verificando Spring Modulith).
   - Frontend ERP & Backoffice React: `lint` + unit tests al 100% sin warnings.
   - DevSecOps: `scripts/security/run-security-scan.sh` verde (0 secretos expuestos).

---

## 🎯 Tareas del Sprint 6 (Secuencia Ordenada por Dependencias)

### 1. Investigación Cloud / Infraestructura y Configuración Twelve-Factor (PB-025)

- [x] **TASK-601 [INFRA/SPIKE] [ARQUITECTURA]:** Investigación Cloud OCI, Topología de BD Separada, Reverse Proxy, Visor de Logs y Perfiles Twelve-Factor (PB-025).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:*
    1. **Spike Comparativo (Bueno, Bonito, Barato y Seguro):** Investigar y validar conjuntamente las mejores opciones para Oracle Cloud (OCI):
       - **Base de Datos Independiente:** Comparativa VM dedicada PostgreSQL en VCN privada OCI vs DBaaS externo (Neon/Supabase) vs límites reales Always Free + diseño de estrategia de **backups automáticos cifrados hacia Object Storage**.
       - **Reverse Proxy / Ingress & SSL:** Comparativa **Caddy** vs **Traefik** vs **Nginx Proxy Manager** vs **Coolify** (y por qué evitar Kubernetes/Cert-Manager en esta etapa), contemplando subdominios iniciales de proveedor cloud (`api.`, `app.`, `admin.`).
       - **Observabilidad de Logs:** Evaluación de visor ligero de contenedores (**Dozzle** vs **Loki/Promtail**) + política de rotación de logs.
    2. **Perfiles y Variables Twelve-Factor:** Crear perfiles explícitos Spring (`application-dev.yml`, `application-testing-local.yml`, `application-staging.yml`, `application-prod.yml`), externalizar CORS/orígenes/secretos y diseñar matriz de 4 entornos (`dev`, `testing-local`, `staging-nube`, `prod`) + contenedores Nginx con inyección en runtime.
  - *Criterio de Entrega:* [ADR-0026](./adr/0026_cloud_infrastructure_and_deployment_stack.md) aceptado, perfiles Twelve-Factor + HikariCP explícito + CORS externalizado configurados, seeders aislados bajo `@Profile({"dev", "test"})` y validados por tests (`./mvnw test`: 175/175 verdes).

---

### 2. Inicialización Limpia Go-Live y Contexto Supra-Tenant (PB-057 / PB-002)

- [x] **TASK-602 [STARTUP/SEGURIDAD] [ARQUITECTURA]:** Arranque Limpio Go-Live sin Tenancies Demo y Desacoplamiento de Identidad `SUPERADMIN` (PB-057).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. Seeders demo aislados bajo `@Profile({"dev", "test"})` (`TASK-601`).
    2. Desacoplamiento físico de identidad `ROLE_SUPERADMIN` en tabla dedicada `platform_admins` y `platform_refresh_tokens` (`V17__platform_admins_and_sessions.sql`), dejando `app_users` y `system_parameter_values` puras para tenants (`ADR-0027`).
    3. Factorización criptográfica (`JwtCodec` + `JwtSigningSpec` + `TokenHashSupport`) con cadenas de seguridad independientes (`@Order(1)` `modules.backoffice` `aud=platform-backoffice` vs `@Order(2)` ERP `aud=erp-tenant`), bootstrap *fail-fast* (`PLATFORM_ADMIN_*`, mín. 12 caracteres, `must_change_password = true`) y recuperación CLI *Break-Glass* (`scripts/ops/reset-superadmin.sh`).
  - *Criterio de Entrega:* [ADR-0027](./adr/0027_platform_identity_and_clean_golive_bootstrap.md) aceptado, migración `V17` validada, `RestaurantModularityTests` y suite completa pasando (`./mvnw test`: 180/180 verdes).

---

### 3. Almacenamiento Desacoplado Agnóstico S3 / OCI (PB-035)

- [x] **TASK-603 [STORAGE] [ARQUITECTURA]:** Motor Agnóstico de Almacenamiento `core.storage` (Local + S3/R2/OCI Object Storage) y Parámetros Dinámicos (PB-035).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. Extraer `LocalFileStorageService` de `modules.catalog.internal` hacia el módulo transversal `core.storage`, exponiendo contrato público `MediaStorageService`.
    2. Implementar **Patrón Strategy / Adapter** intercambiable: `LocalStorageStrategy` + `S3CompatibleStorageStrategy` (`software.amazon.awssdk:s3` + `url-connection-client` compatible con **Oracle Cloud Object Storage S3-API**, Cloudflare R2, MinIO y AWS S3).
    3. Mantener la compresión WebP exclusivamente en frontend (`TASK-506`, evitando bloatware Java); aplicar validación estricta en servidor (MIME + *magic bytes* reales: WebP RIFF, PNG, JPEG).
    4. Parametrización dinámica en tabla `platform_settings` (`V18`) editable desde Backoffice: `storage.active_provider`, **`storage.max_file_size_bytes` (default `1 MB` configurable entre 100 KB y 10 MB)** y `storage.public_url_prefix`. Las credenciales (`AccessKey`/`SecretKey`) permanecen en variables de entorno Twelve-Factor (`S3_*`).
  - *Criterio de Entrega:* `ADR-0028` aprobado; `modules.catalog` consume únicamente `MediaStorageService` vía `.api`; suite backend completa verde (**182 tests passing** en `./mvnw test`, incluyendo `MediaStorageServiceTest` [4], `PlatformSettingsServiceImplTest` [2], `CatalogIntegrationTest` [10] y `RestaurantModularityTests` [2]).

---

### 4. Módulo Backend Supra-Tenant & Observabilidad Actuator (PB-002)

- [x] **TASK-604 [BACKOFFICE BE] [SEGURIDAD]:** Módulo Spring Modulith Supra-Tenant (`modules.backoffice`), Planes/Suscripciones, Cuotas, Telemetría Event-Driven y Observabilidad Actuator (PB-002 / `ADR-0029`).
  - *Prioridad:* `P0 - Crítica`
  - *Especificación SDD:* [`docs/backoffice/`](./backoffice/README.md)
  - *Objetivo:*
    1. **Desacoplamiento Event-Driven + SPI `core.entitlements`:** Contratos públicos en `core.entitlements.api` con caché O(1) invalidado por `TenantEntitlementChangedEvent`, más read-model `tenant_daily_telemetry` (`V19`) alimentado asíncronamente por `OrderPaidEvent` / `OrderCancelledEvent`.
    2. **ABM `platform_admins` & Perfil:** Gestión de administradores de plataforma, cambio de contraseña propio (`/auth/change-password`) y resguardo del último `SUPERADMIN` activo.
    3. **Planes, Créditos Pendientes, Gracia Condicional y Bloqueos Configurables (`V19`):** CRUD `subscription_plans`, `tenant_subscriptions` (2 modos de bloqueo: `POS_AND_SHIFT_ONLY` y `TOTAL`), asignación de plan en modo pagado o **pendiente de pago / uso por adelantado** (`subscription_charges`), conciliación FIFO en `subscription_payments` y gracia condicional por fecha y/o tope de ventas.
    4. **Cuotas y Downgrade No Destructivo:** Enforcement en `company`, `core.security`, `catalog`, `inventory`, `finance` y `pos` (`BRANCHES`, `USERS`, `DISHES`, `INVENTORY_ITEMS`, `MONTHLY_ORDERS`) + endpoint ERP `/api/v1/company/entitlements` para desactivar excedentes sin borrado.
    5. **Telemetría, Storage Check & Observabilidad:** KPIs globales/por tenant (ventas por día/sucursal, inventario, catálogo, usuarios), prueba de conexión al proveedor activo en `core.storage`, wrapper seguro Actuator (`/observability/**`: salud, métricas, loggers en caliente vía `LoggersEndpoint`/`LoggingSystem`, config enmascarada) y bitácora `platform_audit_logs`.
  - *Criterio de Entrega:* Migración `V19` implementada, `ApplicationModules.verify()` sin violaciones (`RestaurantModularityTests` [2/2]) y suite completa `./mvnw clean test` verde (**191 tests passing, 0 failures, 0 skipped**), incluyendo `SubscriptionPlanAndLedgerServiceTest` [7/7], `BackofficeSaaSIntegrationTests` [2/2], `PlatformAuthAndBootstrapIntegrationTests` [3/3] y `PlatformJwtServiceTest` [2/2].

---

### 5. SPA Independiente de Backoffice en React LTS (PB-003)

- [ ] **TASK-605 [BACKOFFICE FE] [UI/UX]:** Aplicación Web Independiente de Backoffice en React LTS + Tailwind CSS (PB-003 / [`04_backoffice_frontend_react_spec.md`](./backoffice/sdd/04_backoffice_frontend_react_spec.md)).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:*
    1. Inicializar aplicación independiente en carpeta hermana **`backoffice-frontend/`** (al mismo nivel que `backend/` y `frontend/`) usando **React (LTS)** + TypeScript + Vite + **Tailwind CSS** (UI administrativa limpia sin bloatware).
    2. Autenticación dedicada de `SUPERADMIN`/`PLATFORM_OPERATOR`, flujo `mustChangePassword` e inyección dinámica de `API_BASE_URL` en runtime (`env.js`).
    3. **Módulos UI del Backoffice:**
       - *Dashboard & Telemetría Multi-Tenant:* KPIs globales, ingresos vs mora, ventas por día y sucursal, inventario/usuarios/platos por tenant.
       - *Inquilinos, Suscripciones, Gracia & Pagos:* Alta de tenant, asignación de planes (pagado o pendiente/crédito), registro de pagos, gracia condicional, overrides de límites y suspensión (`POS_AND_SHIFT_ONLY` vs `TOTAL`).
       - *Planes & Admins Plataforma:* CRUD de `subscription_plans` y ABM de `platform_admins`.
       - *Consola Storage & Settings:* Edición de `storage.active_provider`, límite máximo en MB y botón "Probar Conexión" (proveedor activo).
       - *Observabilidad & Logs en Caliente:* Salud BD/Hikari/disco, métricas JVM, selector de loggers por paquete (`INFO`/`DEBUG`/`TRACE`) con alerta preventiva y visor `platform_audit_logs`.
  - *Criterio de Entrega:* Build de producción sin errores, linting y suite de tests unitarios (Vitest + React Testing Library) al 100% pasando; `frontend/` Angular intacto sin rastro del Backoffice.

---

### 6. Contenerización, Rotación de Logs, BD Separada, Backups y Rollout OCI (PB-025)

- [ ] **TASK-606 [DEVOPS/DEPLOY] [INFRA]:** Dockerfiles Multi-Stage, Rotación de Logs, BD Separada, Backups a Object Storage y Despliegue Cloud OCI (PB-025).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. **Dockerfiles Multi-Stage Ligeros (Non-Root):** Backend Spring Boot Java 25, ERP Frontend Angular (Nginx + runtime env injection) y Backoffice React (Nginx + runtime env injection).
    2. **Rotación Estricta de Logs (Prevención `Disk Full`):** `logback-spring.xml` con topes (`10MB`/`500MB` cap) + driver Docker `json-file` (`max-size: 10m`, `max-file: 5`) en todos los contenedores.
    3. **Topología Cloud & Reverse Proxy SSL:** `docker-compose.staging.yml` y `docker-compose.prod.yml` integrando el Reverse Proxy elegido en `TASK-601` (enrutando `app.`, `admin.`, `api.` y visor Dozzle protegido), comunicándose con la **base de datos PostgreSQL separada** seleccionada en el Spike.
    4. **Respaldos Automatizados de BD:** Script/contenedor cron (`pg_dump` comprimido y cifrado $\rightarrow$ bucket privado Object Storage con retención rotativa) + procedimiento documentado y probado de restauración (*restore drill*).
    5. **Rollout Staging en Oracle Cloud (OCI):** Despliegue vivo funcional accesible por HTTPS.
  - *Criterio de Entrega:* Los 3 servicios levantando vía HTTPS detrás de Proxy SSL contra BD separada; backup automático verificado en Object Storage; rotación de logs probada.

---

### 7. Escaneo Local de Seguridad y Secretos DevSecOps (PB-058)

- [ ] **TASK-607 [DEVSECOPS] [SEGURIDAD]:** Auditoría Local de Secretos, Vulnerabilidades SAST/SCA y Escaneo de Imágenes Docker (PB-058).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. Evaluar e integrar herramientas open-source locales (ej. **`gitleaks`** para detección de secretos en Git, **`trivy`** para escaneo de dependencias Maven/npm + Dockerfiles + imágenes construidas, `npm audit` y SAST **`semgrep` / `SpotBugs`**).
    2. Implementar script unificado `scripts/security/run-security-scan.sh` y plantilla de hook pre-commit para bloquear filtración accidental de credenciales antes del `git push`.
    3. Ejecutar auditoría completa sobre el monorepo (`backend`, `frontend`, `backoffice-frontend`) y las 3 imágenes Docker construidas en `TASK-606`, mitigando cualquier hallazgo `CRITICAL`/`HIGH`.
  - *Criterio de Entrega:* 0 secretos detectados en el historial y árbol de trabajo; reporte de vulnerabilidades limpio antes de certificar Staging/CI.

---

### 8. Pipeline GitHub Actions Optimizado y Certificación Staging Cloud (PB-025 / QA)

- [ ] **TASK-608 [CI/CD & QA] [CALIDAD]:** Workflows GitHub Actions sin Desperdicio de Minutos y Certificación Smoke Cloud Staging (PB-025).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. **Pipeline `.github/workflows/` Optimizado para Cuota Gratuita:**
       - **Ramas `feat/*`:** **0 ejecuciones automáticas** (pruebas locales obligatorias; disparador manual `workflow_dispatch` opcional).
       - **Integración en `develop` (Staging):** *Path filters*, caché Maven/npm, cancelación de concurrencia (`cancel-in-progress`), validación de build/tests en Linux limpio, push de imágenes Docker a **GHCR** y despliegue a Staging.
       - **Ramas `release/*` / `main` (Producción):** Gate final + despliegue controlado con aprobación manual (*Environment Protection*).
    2. **Certificación End-to-End en Cloud Staging (OCI):** Batería Smoke verificando:
       - Acceso HTTPS a ERP Angular y React Backoffice sin errores CORS/mixtos.
       - Subida de imagen de plato en ERP almacenándose en Object Storage con respeto al límite configurado en Backoffice.
       - Cambio dinámico de nivel de log en caliente desde React Backoffice.
       - Suspensión de un tenant desde Backoffice bloqueando sesión en ERP + verificación de backup/restore de PostgreSQL.
    3. Reporte formal de ejecución y certificación en `docs/testing/reporte_ejecucion_sprint6.md`.
  - *Criterio de Entrega:* Pipeline CI/CD verde consumiendo mínimos minutos, certificación Staging 100% superada y documentación SDD/inventario sincronizada.
