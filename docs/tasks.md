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
last_updated: "2026-11-11"
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

- [ ] **TASK-603 [STORAGE] [ARQUITECTURA]:** Motor Agnóstico de Almacenamiento `core.storage` (Local + S3/R2/OCI Object Storage) y Parámetros Dinámicos (PB-035).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:*
    1. Extraer `LocalFileStorageService` de `modules.catalog.internal` hacia el módulo transversal `core.storage`, exponiendo contrato público `MediaStorageService`.
    2. Implementar **Patrón Strategy / Adapter** intercambiable: `LocalStorageProvider` + `S3CompatibleStorageProvider` (compatible con **Oracle Cloud Object Storage S3-API**, Cloudflare R2, MinIO y AWS S3).
    3. Mantener la compresión WebP exclusivamente en frontend (`TASK-506`, evitando bloatware Java); aplicar validación estricta en servidor (MIME/*magic bytes* reales).
    4. Parametrización dinámica `GLOBAL` editable desde Backoffice: `storage.active_provider`, **`storage.max_file_size_bytes` (default `1 MB` configurable)** y `storage.public_url_prefix`. Las credenciales (`AccessKey`/`SecretKey`) permanecen en variables de entorno Twelve-Factor.
  - *Criterio de Entrega:* `modules.catalog` consume únicamente `MediaStorageService` vía `.api`; pruebas unitarias/integración con MinIO o mock S3 verificando cambio de estrategia y límite dinámico.

---

### 4. Módulo Backend Supra-Tenant & Observabilidad Actuator (PB-002)

- [ ] **TASK-604 [BACKOFFICE BE] [SEGURIDAD]:** Módulo Spring Modulith Supra-Tenant (`modules.backoffice`), Gestión de Tenants y Control de Logs en Caliente (PB-002).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:*
    1. Crear módulo `modules.backoffice` estrictamente desacoplado mediante `.api`, con rutas `/api/v1/backoffice/**` protegidas exclusivamente para `ROLE_SUPERADMIN`.
    2. Definir persistencia en la misma instancia PostgreSQL con aislamiento lógico (decidiendo esquema/tablas propias mínimas de auditoría/plataforma en Fase 1).
    3. **Gestión de Inquilinos:** Listar, crear/aprovisionar, activar/suspender empresas (`tenants`) con invalidación inmediata de sesiones ante suspensión, y métricas agregadas por tenant.
    4. **Configuración Global & Storage:** Consultar/actualizar parámetros `scope = GLOBAL` (incluyendo proveedor storage activo y tamaño máx. en MB) + endpoint **"Probar Conexión Object Storage"**.
    5. **Observabilidad & Logs en Caliente:** Exponer endpoints seguros envolviendo **Spring Boot Actuator** (`/actuator/health` con espacio en disco/Hikari, métricas HTTP/JVM y **`/actuator/loggers` para cambiar niveles `INFO`/`DEBUG`/`TRACE` en caliente sin reiniciar**) + visor de configuración de endpoints efectivos del ambiente.
  - *Criterio de Entrega:* `ApplicationModules.verify()` sin violaciones, pruebas de seguridad estrictas verificando rechazo `403` para usuarios tenant, y tests de Actuator loggers/suspensión al 100%.

---

### 5. SPA Independiente de Backoffice en React LTS (PB-003)

- [ ] **TASK-605 [BACKOFFICE FE] [UI/UX]:** Aplicación Web Independiente de Backoffice en React LTS + Tailwind CSS y Documentación Admin (PB-003).
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:*
    1. Inicializar aplicación independiente en carpeta hermana **`backoffice-frontend/`** usando **React (última versión estable/LTS)** + TypeScript + Vite + **Tailwind CSS** (reutilizando paleta/tokens visuales del ERP con UI administrativa limpia: tablas, formularios, alertas, layout sobrio sin bloatware).
    2. Autenticación dedicada de `SUPERADMIN` e inyección dinámica de `API_BASE_URL` en runtime (`env.js`/`config.json`).
    3. **Módulos UI del Backoffice:**
       - *Dashboard & Salud del Sistema:* Estado de servicios, espacio en disco, pool de BD y endpoints activos del ambiente.
       - *Gestión de Inquilinos (Tenants):* Tabla con métricas, alta, activación y suspensión en 1 clic.
       - *Consola Global & Storage:* Cambio de proveedor (`LOCAL` vs `S3/OCI`), límite máximo de upload configurable (ej. `1 MB`) y botón "Probar Conexión".
       - *Control de Logs en Caliente:* Selector por paquete Java para alternar `INFO`/`DEBUG`/`ERROR` en producción con alerta preventiva al dejar `DEBUG` activo.
    4. Documentación SDD concisa en `docs/sdd/` enfocada en operación para administradores de plataforma y arquitectura React.
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
