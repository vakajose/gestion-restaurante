# 🏁 Reporte de Ejecución, DevSecOps y Preparación Cloud — Sprint 6

- **Fecha:** 13 de Noviembre de 2026
- **Sprint:** Sprint 6 (`feat/sprint-6`)
- **Estándar:** ADR-0017 (Pruebas), ADR-0026 (OCI Stack), ADR-0027 (Bootstrap Limpio), ADR-0028 (Storage Agnóstico), ADR-0029 (Backoffice SaaS)
- **Estado General:** ✅ **CERTIFICADO EN STACK DOCKERIZADO LOCAL & DEVSECOPS (`TASK-601`..`TASK-608`) — PENDIENTE PROVISIÓN OCI Y SMOKE HTTPS NUBE (`TASK-609`)**

---

## 1. Resumen Ejecutivo por Capa y Tarea

| Tarea | Alcance | Evidencia Verificada Localmente | Estado |
| :--- | :--- | :--- | :---: |
| **`TASK-601`..`TASK-603`** | ADRs Cloud/Auth/Storage, Bootstrap Limpio Go-Live (`@Profile dev/demo`), Motor `core.storage` (Local + S3/OCI) | Compilado y cubierto por suite Backend + Modulith | ✅ **COMPLETADO** |
| **`TASK-604`** | Módulo Spring Modulith `modules.backoffice` (`V17..V19`), `core.entitlements`, Cuotas/Gracia/Ledger FIFO, Observabilidad Actuator | `./mvnw -B clean test`: **191 tests, 0 failures, 0 errors, 0 skipped** (`RestaurantModularityTests` 2/2) | ✅ **COMPLETADO** |
| **`TASK-605`** | SPA Independiente `backoffice-frontend/` (React 19 + TypeScript + Vite + Tailwind CSS + `env.js` runtime) | `eslint` 0 errores, `vitest run` **4 archivos / 11 tests**, `vite build` limpio (`111.05 kB` JS gzip) | ✅ **COMPLETADO** |
| **`TASK-606`** | Dockerfiles Multi-Stage Non-Root (`backend` `729MB`, `erp-frontend` `103MB`, `backoffice-frontend` `101MB`), `env.js`, `logback-spring.xml` (`10MB`/`500MB`), Compose `testing-local`/`staging`/`prod` + Caddy + Dozzle + VM2 PostgreSQL + Backup/Restore GPG AES-256 | Stack `restaurant-testing-local` levantado saludable (6/6 contenedores), `uid=1001`/`101` non-root, log rotation `json-file` (`10m`/`5`), restore drill real en BD (`19` migraciones Flyway, `1` `platform_admins`) | ✅ **COMPLETADO** |
| **`TASK-607`** | `scripts/security/run-security-scan.sh --images`, `.gitleaks.toml`, pre-commit hook, SpotBugs `4.10.5.0` + FindSecBugs `1.14.0` (`-Psecurity`), `npm audit` y Trivy FS + Imágenes Docker | **RESUMEN DEVSECOPS (`failures=0`)**: Gitleaks `0` leaks, SpotBugs `0` bugs/errors, `npm audit --omit=dev` `0` HIGH/CRITICAL, Trivy FS y 3 imágenes (`backend`, `erp-frontend`, `backoffice-frontend`) con **`0` vulnerabilidades HIGH/CRITICAL con fix** (parcheados Tomcat `11.0.25`, Jackson BOM `3.1.7`, BouncyCastle `1.85` y Nginx `1.31-alpine`) | ✅ **COMPLETADO** |
| **`TASK-608`** | `.github/workflows/{ci,cd-staging,cd-production}.yml` (0 runs en `feat/*`, path filters, GHCR `linux/arm64`, Trivy, manual prod gate) + Certificación Smoke Docker `testing-local` | Validados `api.localhost/actuator/health` (`UP`), `app.localhost` (`200` + `env.js`), `admin.localhost` (`200` + `env.js`), `logs.localhost` (`200`), CORS `http://admin.localhost`, login/change-password Backoffice, `/storage/test-connection` (`LOCAL`), `PUT /observability/loggers/com.restaurant.app` (`configuredLevel: DEBUG -> INFO`) | ✅ **COMPLETADO** |
| **`TASK-609`** | Provisión guiada OCI (evaluación IaC OpenTofu/Terraform vs OCI CLI, Compartments/IAM/Budgets, VCN Zero-Trust, VM1 + VM2, Buckets + S3 keys) y Smoke HTTPS Staging Nube | Pendiente inicio de sesión interactiva Fase 1–2 (`AGENTS.md` Regla 8) | ⏳ **PENDIENTE (NUEVA TAREA)** |

---

## 2. Detalle de Verificaciones Ejecutadas

### 2.1 Backend Spring Boot 4.1.1 / Java 25 (`backend/`)
- **Comando:** `./mvnw -B clean test spotbugs:check -Psecurity`
- **Pruebas:** **191 tests run, 0 failures, 0 errors, 0 skipped** (incluye `logback-spring.xml` activo y reemplazo de `MD5` por `SHA-256` en `ConfigurationController.generateETag`).
- **SAST (`SpotBugs 4.10.5.0 + FindSecBugs 1.14.0`):** Se detectó y corrigió `WEAK_MESSAGE_DIGEST_MD5` en `ConfigurationController.java:125`; segunda corrida finalizó con **0 bugs / 0 errors**.

### 2.2 ERP Frontend Angular 22 (`frontend/`)
- **Inyección Runtime Twelve-Factor (`env.js`):** Incorporado `public/env.js`, `runtime-env.ts` y `apiBaseUrlInterceptor` (3 pruebas nuevas en `api-base-url.interceptor.spec.ts`).
- **Comandos:** `npm run lint && npm test -- --watch=false && npm run build`
- **Resultado:** `ng lint` sin errores, Vitest **43 test files / 376 tests passed**, `ng build` generó `frontend/dist/frontend/browser` sin errores.
- **SCA (`npm audit --omit=dev --audit-level=high`):** Se actualizó `frontend/package-lock.json` (`zod` mitigado) logrando **0 vulnerabilidades HIGH/CRITICAL en dependencias de producción**. *(Nota: en `devDependencies`, `@angular/build` arrastra 1 vulnerabilidad `high` en `vite` dev-server únicamente).*

### 2.3 Backoffice Frontend React 19 (`backoffice-frontend/`)
- **Comandos:** `npm run lint && npm test && npm run build`
- **Resultado:** `eslint .` limpio, Vitest v5.1.0 **4 test files / 11 tests passed**, `tsc -b && vite build` limpio.
- **SCA (`npm audit --audit-level=high` y `--omit=dev`):** Tras actualizar `vitest` a `^5.1.0` (eliminando `CRITICAL` en `tinypool`), reporta **0 vulnerabilidades HIGH/CRITICAL totales y en producción**.

### 2.4 Respaldos Cifrados, Stack Docker & Hardening Contenerizado (`scripts/db/`, `docker/`)
- **Validación `docker compose config --quiet`:** Aprobada sin errores/warnings para `docker-compose.testing-local.yml`, `docker-compose.prod.yml`, `docker-compose.staging.yml` (`--profile staging`) y `postgres-vm2/docker-compose.db.yml` (hashes bcrypt en `.env.cloud.example` escapados con `$$`).
- **Imágenes Multi-Stage Non-Root Construidas:**
  - `restaurant-testing-local-backend:latest` (`729MB`, `uid=1001(appuser)`), Logback `/app/logs/restaurant-backend.log` activo (`11.2K`) y driver `json-file` (`max-size=10m`, `max-file=5`).
  - `restaurant-testing-local-erp-frontend:latest` (`103MB`, `nginxinc/nginx-unprivileged:1.31-alpine` `3.24.2`, `uid=101(nginx)`).
  - `restaurant-testing-local-backoffice-frontend:latest` (`101MB`, `nginxinc/nginx-unprivileged:1.31-alpine` `3.24.2`, `uid=101(nginx)`).
- **Prueba End-to-End Caddy + API + Loggers en Caliente (`testing-local`):**
  - `api.localhost/actuator/health` -> `200 UP`; `app.localhost` -> `200` + `env.js`; `admin.localhost` -> `200` + `env.js`; `logs.localhost` (Dozzle) -> `200`.
  - Login Backoffice (`superadmin`) -> `200` (`mustChangePassword=true`, header CORS `Access-Control-Allow-Origin: http://admin.localhost`), cambio obligatorio de contraseña -> `200` (`mustChangePassword=false`), `GET /dashboard/overview` -> `200`, `POST /storage/test-connection` (`LOCAL`) -> `200`, `PUT /observability/loggers/com.restaurant.app` con `{"configuredLevel":"DEBUG"}` -> `200` (`effectiveLevel="DEBUG"`) y restauración a `"INFO"`.
- **Restore Drill PostgreSQL Encriptado (GPG AES-256 + SHA-256):**
  - `backup-postgres-s3.sh --local-only --docker-container restaurant-testing-db` generó artefacto cifrado (`20K`) + checksum SHA-256.
  - `restore-postgres-s3.sh --docker-container restaurant-testing-db --target-db restaurant_restore_drill` validó SHA-256, descifró con `GNUPGHOME` efímero y restauró integralmente en PostgreSQL (`19` migraciones Flyway exitosas, `1` fila `platform_admins`).
- **Auditoría DevSecOps Integral (`./scripts/security/run-security-scan.sh --images`):**
  - **Exit code `0` (`failures=0`):** Gitleaks (`0` secretos en `2.52 MB`), 3/3 Dockerfiles non-root, SpotBugs/FindSecBugs (`0` bugs/errors), `npm audit --omit=dev --audit-level=high` (`0` en ambas SPAs), Trivy FS (`0` HIGH/CRITICAL) y Trivy Imágenes (`0` HIGH/CRITICAL con fix tras actualizar `tomcat.version` `11.0.25`, `jackson-bom.version` `3.1.7`, `bcprov-jdk18on` `1.85` y base `1.31-alpine` + `apk upgrade`).

---

## 3. Alcance Cloud Reorganizado (`TASK-609`)
1. **Provisión Guiada Oracle Cloud Infrastructure (OCI) + IaC:** Evaluación de IaC (**OpenTofu / Terraform OCI** vs **OCI CLI**), configuración de Compartment, políticas IAM, Presupuesto Always Free PAYG, red privada VCN Zero-Trust, Compute `VM 1` (Apps ARM64) + `VM 2` (PostgreSQL 16), Object Storage privado y Secretos GitHub Actions (`OCI_VM1_HOST`, `OCI_VM1_USER`, `OCI_VM1_SSH_KEY`, `OCI_STAGING_DEPLOY_ENABLED`, `APP_DOMAIN`).
2. **Rollout Staging & Certificación Smoke HTTPS Real:** Primer despliegue vía `cd-staging.yml` y verificación end-to-end contra URLs HTTPS públicas en OCI.
