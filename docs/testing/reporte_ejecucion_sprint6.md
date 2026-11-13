# 🏁 Reporte de Ejecución, DevSecOps y Preparación Cloud — Sprint 6

- **Fecha:** 13 de Noviembre de 2026
- **Sprint:** Sprint 6 (`feat/sprint-6`)
- **Estándar:** ADR-0017 (Pruebas), ADR-0026 (OCI Stack), ADR-0027 (Bootstrap Limpio), ADR-0028 (Storage Agnóstico), ADR-0029 (Backoffice SaaS)
- **Estado General:** ⚙️ **IMPLEMENTADO Y VERIFICADO EN LOCAL — PENDIENTE PROVISIÓN OCI Y DOCKER DESKTOP WSL2 PARA SMOKE NUBE**

---

## 1. Resumen Ejecutivo por Capa y Tarea

| Tarea | Alcance | Evidencia Verificada Localmente | Estado |
| :--- | :--- | :--- | :---: |
| **`TASK-601`..`TASK-603`** | ADRs Cloud/Auth/Storage, Bootstrap Limpio Go-Live (`@Profile dev/demo`), Motor `core.storage` (Local + S3/OCI) | Compilado y cubierto por suite Backend + Modulith | ✅ **COMPLETADO** |
| **`TASK-604`** | Módulo Spring Modulith `modules.backoffice` (`V17..V19`), `core.entitlements`, Cuotas/Gracia/Ledger FIFO, Observabilidad Actuator | `./mvnw clean test`: **191 tests, 0 failures, 0 errors, 0 skipped** (`RestaurantModularityTests` 2/2) | ✅ **COMPLETADO** |
| **`TASK-605`** | SPA Independiente `backoffice-frontend/` (React 19 + TypeScript + Vite + Tailwind CSS + `env.js` runtime) | `eslint` 0 errores, `vitest run` **4 archivos / 11 tests**, `vite build` limpio (`111.05 kB` JS gzip) | ✅ **COMPLETADO** |
| **`TASK-606`** | Dockerfiles Multi-Stage Non-Root (`backend`, `frontend`, `backoffice-frontend`), `env.js` en ambas SPAs, `logback-spring.xml` (`10MB`/`500MB`), Compose `testing-local`/`staging`/`prod` + Caddy + Dozzle + VM2 PostgreSQL + Scripts Backup/Restore GPG AES-256 | Backend `191/191`, Angular `lint` + `43 archivos / 376 tests` + `ng build`, `restore-postgres-s3.sh --verify-only` GPG/SHA-256 OK | ⏳ **LOCAL LISTO / PENDIENTE ROLLOUT OCI** |
| **`TASK-607`** | `scripts/security/run-security-scan.sh`, `.gitleaks.toml`, pre-commit hook, SpotBugs 4.10.5.0 + FindSecBugs 1.14.0 (`-Psecurity`), mitigación MD5 -> SHA-256 y `npm audit` | SpotBugs + FindSecBugs: **0 bugs, 0 errors**; `npm audit --omit=dev --audit-level=high`: **0 HIGH/CRITICAL** en `frontend` y `backoffice-frontend` | ✅ **LOCAL VERIFICADO (CI TRIVY)** |
| **`TASK-608`** | `.github/workflows/{ci,cd-staging,cd-production}.yml` (0 runs en `feat/*`, path filters, GHCR `linux/arm64`, Trivy, manual prod gate) + Reporte | Workflows y checks locales listos; pendiente corrida real GH Actions / Smoke HTTPS OCI | ⏳ **PIPELINES LISTOS / PENDIENTE SMOKE OCI** |

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

### 2.4 Respaldos Cifrados & Hardening Docker (`scripts/db/`, `docker/`)
- **Prueba local GPG AES-256 + SHA-256:** `restore-postgres-s3.sh --verify-only` verificó checksum SHA-256 y descifrado AES-256 con `GNUPGHOME` efímero sobre SQL gzippeado íntegro.
- **Validación de sintaxis:** `bash -n` y `sh -n` sin errores en `backup-postgres-s3.sh`, `restore-postgres-s3.sh`, `40-generate-env.sh` y `scripts/security/*.sh`.

---

## 3. Brechas Pendientes de Entorno Externo (Para Cierre en Nube)
1. **Docker Desktop WSL2 Integration:** El comando `docker` no estuvo habilitado en la distro Linux WSL2 actual (`The command 'docker' could not be found in this WSL 2 distro`), por lo que la construcción local de imágenes (`docker compose -f docker/docker-compose.testing-local.yml up --build -d`) y el escaneo local de imágenes con `trivy` quedan pendientes hasta activar la integración WSL2 en Docker Desktop o ejecutarse en GitHub Actions.
2. **Provisión OCI (VM 1 + VM 2 + Bucket Object Storage + Subdominios DNS):** Pendiente configurar secretos/variables en GitHub (`OCI_VM1_HOST`, `OCI_VM1_USER`, `OCI_VM1_SSH_KEY`, `OCI_STAGING_DEPLOY_ENABLED`, `APP_DOMAIN`) para ejecutar el Smoke Test HTTPS Staging de `TASK-608`.
