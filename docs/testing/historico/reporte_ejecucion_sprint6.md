---
title: "Reporte de Ejecución, DevSecOps, Backoffice y Estabilización MVP v1 — Sprint 6"
category: "testing"
tags:
  - testing
  - qa
  - devsecops
  - sprint-6
  - report
  - mvp-v1
  - historical
last_updated: "2026-11-17"
status: "historical"
---

# 🏁 Reporte de Ejecución, DevSecOps, Backoffice SaaS y Estabilización MVP v1 — Sprint 6

- **Fecha:** 17 de Noviembre de 2026 *(Ronda inicial Docker/DevSecOps: 13 de Noviembre de 2026)*
- **Rama:** `release/sprint-6`
- **Estándar:** ADR-0017 (Pruebas), ADR-0026 (OCI Stack), ADR-0027 (Bootstrap Limpio), ADR-0028 (Storage Agnóstico), ADR-0029 (Backoffice SaaS), ADR-0030 (Aislamiento Catálogo), ADR-0031 (Sesiones de Servicio y Combos), ADR-0033 (Runtime Env y Media Cross-Origin)
- **Migraciones Flyway Cubiertas:** `V1`–`V24` (`24` migraciones)
- **Estado General:** ✅ **SUITES UNITARIAS, INTEGRACIÓN Y DEVSECOPS CERTIFICADAS HASTA `TASK-635` + FIXES POST-E2E (`82ecd5e`–`3cc923d`) — PENDIENTE PROVISIÓN OCI (`TASK-609` EN PARALELO) Y ADAPTACIÓN DE SELECTORES E2E PARA `testing-local`**

---

## 1. Resumen Ejecutivo de Cobertura Verificada (`release/sprint-6`)

| Capa / Aplicación | Comando Ejecutado | Métricas Verificadas | Resultado |
| :--- | :--- | :--- | :---: |
| **Backend Spring Boot 4.1.1 / Java 25** | `cd backend && ./mvnw test -o` | **49 clases Surefire / 213 `@Test`** (`RestaurantModularityTests` 2/2 + Flyway `V1`–`V24` + H2) | ✅ **213/213 PASSED** (`0` fail, `0` skip) |
| **ERP Frontend Angular 22** | `cd frontend && npm run lint && npm test -- --watch=false` | `ng lint` limpio (`0` errores) + **43 archivos `.spec.ts` / 390 tests** | ✅ **390/390 PASSED** |
| **Backoffice Frontend React 19** | `cd backoffice-frontend && npm run lint && npm test` | `eslint .` limpio + **11 archivos `.test.*` / 22 tests** | ✅ **22/22 PASSED** |
| **Playwright E2E (`frontend/e2e/`)** | `./scripts/run-e2e.sh` *(entorno `dev`)* | **7 archivos `.spec.ts` / 34 tests + 4 Page Objects** | ⚠️ Ver nota `testing-local` |
| **DevSecOps Local & Docker** | `./scripts/security/run-security-scan.sh --images` | Gitleaks `0`, SpotBugs `0`, `npm audit --omit=dev` `0`, Trivy `0` `HIGH`/`CRITICAL` con fix | ✅ **PASSED (`failures=0`)** |

*(Nota: En `backend/src/test/` existen 50 archivos `.java` en total; 49 corresponden a clases de prueba ejecutadas por Surefire y 1 es la clase auxiliar `@TestConfiguration` `TestH2DbConfig.java`).*

---

## 2. Resumen por Fases y Bloques de Sprint 6

### 2.1 Bloque 1 — Cloud Prep, Backoffice SaaS, Storage, Docker y DevSecOps (`TASK-601`–`TASK-608`, `TASK-610`)
- **`TASK-601`–`TASK-603`:** Perfiles Twelve-Factor, HikariCP explícito, CORS externalizado, `@Profile({"dev", "test"})` en seeders demo, `platform_admins` (`V17`), `platform_settings` (`V18`) y motor agnóstico `core.storage` (`LOCAL` + `S3` con *magic bytes*).
- **`TASK-604`–`TASK-605`:** Módulo Spring Modulith `modules.backoffice` (`V19`), `core.entitlements`, ledger FIFO, loggers en caliente Actuator y SPA independiente `backoffice-frontend/` en React 19 + Tailwind + runtime `env.js`.
- **`TASK-606`–`TASK-608`, `TASK-610`:** Stack `docker-compose.testing-local.yml` (6 contenedores saludables detrás de Caddy + Dozzle), backup/restore drill GPG AES-256 + SHA-256 validado, escaneo integral DevSecOps (`failures=0`) y workflows GitHub Actions 4 ambientes (`ci.yml`, `cd-staging.yml` por tags `-rc`/`-staging`, `cd-production.yml` desde `main` con aprobación manual).

### 2.2 Bloque 2 — Manual Operativo, Demo Guard y Remediación UAT Ronda 1 (`TASK-611`, `TASK-612`, `TASK-614`–`TASK-618`, `TASK-620`–`TASK-627`)
- **ERP (`TASK-611`, `612`, `614`, `615`, `621`, `626`, `627`):** Manual [`bootstrap_y_operacion_inicial.md`](../../manuals/plataforma/bootstrap_y_operacion_inicial.md), ocultamiento de accesos rápidos demo fuera de `dev` (`isDevMode()`), refresco reactivo en `BranchContextService`, manejo de HTTP 304 en `errorInterceptor`, confirmación de contraseña en `/onboarding`, consulta de arqueos en `/finance` con `JpaSpecificationExecutor` (resolviendo error de parámetros nulos en PostgreSQL) y doble confirmación al abrir turno sin platos.
- **Backoffice (`TASK-616`–`618`, `620`, `622`–`625`):** Buscador de loggers y auditoría paginada con visor JSON (`ObservabilityPage`), aprovisionamiento opcional sin dirección/email (`V20` + tracking `Sin primer ingreso`), primitivas UI compartidas (`Modal`, `Card`, `EmptyState`, `Badge`), Dark Mode (`ThemeProvider`), planes categorizados e ilimitados gratis (`V21`), email opcional/política de contraseñas en `platform_admins` (`V22`) y gestión/reseteo de usuarios del tenant en `/tenants/:id` (`TenantUsersTab`).

### 2.3 Bloque 3 — Estabilización MVP v1 y Fixes Post-E2E (`TASK-628`–`TASK-635` + `82ecd5e`–`3cc923d`)
- **`TASK-628` (`OBS-013`):** Redirección forzada a `/profile` cuando `mustChangePassword = true`, gobernada por `security.enforce_password_change_on_reset`, más validación condicional `security.require_user_email` en `/users` (`82ecd5e`).
- **`TASK-629` (`OBS-015`):** Proxy `handle /uploads/*` en `Caddyfile.testing-local` / `Caddyfile.cloud` y resolución de URLs relativas contra `API_BASE_URL` en `runtime-env.ts`.
- **`TASK-630` (`OBS-018`):** Eliminación de jerga técnica (*"Registrado en servidor"*, *"Receta BOM"*) en POS, Catálogo y Control de Caja.
- **`TASK-631`–`TASK-632` (`OBS-014`, `OBS-016`, `OBS-017`):** Edición completa de platos y edición/eliminación segura de categorías; reparación visual de tabla *"Precios de mi Sucursal"*, aislamiento por sucursal por defecto (`catalog.scope_mode = BRANCH_ISOLATED`) y clonado a sucursal destino.
- **`TASK-633`–`TASK-634` (`OBS-019`–`OBS-022`):** Migraciones `V23` y `V24` (`day_of_week VARCHAR(120)`), jornadas compartidas (`branch_service_sessions`, `finance.auto_open_full_catalog_session`), CRUD de combos `FIXED`/`MENU_COURSE` y complementos, cupos ilimitados (`is_unlimited`), restricción `allow_individual_sale`, selecciones en comanda (`order_item_selections`) y deducción en Kardex (`DailyMenuAndServiceSessionIntegrationTests`).
- **`TASK-635` (`OBS-023`):** Edición y borrado seguro de insumos, distinción visual Stock Inicial vs Ajuste de Conteo y cálculo automático de costo unitario desde Valor Total del Lote.
- **Pulido Post-E2E (`82ecd5e`, `b6cc737`, `3cc923d`):** Cambio recibido (`Order.changeAmount`) y fecha/hora local en comprobante POS, reglas `anyPermissions` en guards y sidebar RBAC, ancho de tabla catálogo y apilamiento limpio de badges de días en `/catalog/menus`.
- **`TASK-613` (Auditoría y Sincronización Documental Total):** Depuración de obsoletos, normalización Obsidian sin `[[wikilinks]]`, creación de [`ADR-0030`](../../adr/0030_catalog_branch_isolation_by_default.md)–[`ADR-0033`](../../adr/0033_spa_runtime_env_and_cross_origin_media.md), guías cliente/plataforma y sincronización exacta de métricas QA y backlogs.

---

## 3. Brechas Conocidas y Pendientes de Cierre

1. **`TASK-609` (OCI Rollout en Paralelo):** Aprovisionamiento de VCN, VM1/VM2, Object Storage y certificación HTTPS Staging en Oracle Cloud ejecutándose en chat/worktree independiente.
2. **Playwright contra `testing-local`:** Las 7 suites (`34` tests) de `frontend/e2e/` pasan sobre entorno `dev` (`:4200`), pero `playwright.config.ts`, `auth.page.ts`, `auth.spec.ts`, `catalog.spec.ts` e `inventory.spec.ts` aún dependen de `:4200` y de `[data-testid="demo-admin-btn"]` (oculto fuera de `dev` tras `TASK-612`). Su migración a login estándar y `PLAYWRIGHT_BASE_URL` permitirá ejecutar la batería automática directamente sobre `http://app.localhost`.
