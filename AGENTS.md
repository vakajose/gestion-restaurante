# AGENTS.md - Protocolo Operativo Maestro para Antigravity

## 1. Alcance y Stack Real (3 Aplicaciones)
- **Host / OS:** WSL2 sobre Linux Fedora (`dnf`); cliente local `psql` para diagnósticos PostgreSQL.
- **Backend (`backend/`):** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith 2.1.x, PostgreSQL 16, Flyway (`V1`–`V24`, 56 tablas), Maven.
- **ERP Restaurante (`frontend/`):** Angular 22 (`provideZonelessChangeDetection()`, `ReactiveFormsModule` / `FormsModule` con Signals, Standalone Components), TailwindAdmin v3.0 (Solar/Tabler Icons), Dexie.js (`PENDING` | `SYNCED` | `FAILED`).
- **Backoffice SaaS (`backoffice-frontend/`):** React 19 LTS, TypeScript, Vite, React Router, Tailwind CSS 3.4, Lucide React, Vitest + React Testing Library.
- **Aislamiento Multi-Tenant y Plataforma:** Toda entidad operativa exige `tenant_id` (UUID) y `branch_id` (UUID) vía `TenantContextHolder` (JWT). El plano plataforma (`modules.backoffice`) opera segregado por audiencia JWT `platform-backoffice`.

## 2. Reglas Arquitectónicas y Gobernanza Inviolable (SSOT)
> Las especificaciones detalladas, ejemplos y topologías residen en [`.agents/rules/`](.agents/rules/) y [`.agents/skills/`](.agents/skills/) sin duplicación.

1. **Límites Modulares (Spring Modulith & DAL):**
   - La suite debe verificar `ApplicationModules.of(RestaurantApplication.class).verify()`.
   - Prohibido inyectar repositorios de otros módulos o de `core`; consumir exclusivamente interfaces `.api` o eventos.
   - En frontend ERP, aislar Dexie.js (`RestaurantPosDb` / `appDb`) tras repositorios DAL.
   - Referencias: [architectural-governance.md](.agents/rules/architectural-governance.md) y [spring-modulith-patterns](.agents/skills/spring-modulith-patterns/SKILL.md).
2. **Eventos Asíncronos Desacoplados:**
   - `modules.pos` publica `OrderPaidEvent` y `OrderCancelledEvent`.
   - `modules.inventory`, `modules.finance` y `modules.backoffice` (`BackofficeTelemetryListener`) los consumen vía `@ApplicationModuleListener` (más `@EventListener` síncrono para tracking ACID de gracia).
3. **Offline-First POS (`ADR-0003`):**
   - Persistir mutaciones localmente en Dexie (`client_transaction_id` idempotente, `syncStatus = 'PENDING'`) antes de invocar la API.
   - Referencia: [offline-dexie-sync](.agents/skills/offline-dexie-sync/SKILL.md).
4. **Fechas y Zonas Horarias (`ADR-0006`):**
   - Fechas calendario puras: `DATE` (PG) / `LocalDate` (Java) / `"YYYY-MM-DD"` (JSON/TS) y `dd/MM/yyyy` (UI).
   - Timestamps transaccionales: `TIMESTAMPTZ` (PG) / `Instant` (Java) / ISO UTC con `Z`; cortes diarios convertidos a `branches.timezone` (default `'America/La_Paz'`).
   - Referencia: [datetime-standard](.agents/skills/datetime-standard/SKILL.md).
5. **SDD-SDLC 4 Fases (`ADR-0007`):**
   - Todo cambio de alcance, modelo de datos o contrato `.api` cumple: Fase 1 (Análisis) $\rightarrow$ Fase 2 (Spec-First) $\rightarrow$ Fase 3 (Construcción) $\rightarrow$ Fase 4 (Sync-Always).
   - Solo *hotfixes* o ajustes triviales usan Fast-Track (Fase 4 obligatoria en el mismo commit).
   - Referencia: [sdd-architecture-diagramming](.agents/skills/sdd-architecture-diagramming/SKILL.md).
6. **Co-Diseño Interactivo Obligatorio en Fases 1 y 2:**
   - Análisis y diseño son sesiones colaborativas (**Proponer $\rightarrow$ Discutir $\rightarrow$ Definir $\rightarrow$ Confirmar**).
   - Prohibido congelar ADRs, diagramas o specs unilateralmente sin validación explícita del usuario (salvo orden directa en el prompt).
7. **Dependencias Externas y Anti-Duplicación Visual:**
   - Investigar librerías maduras vs alternativa propia (impacto en bundle) antes de añadir dependencias.
   - Reutilizar primitivos compartidos en `frontend/src/app/shared/components/ui/` y `backoffice-frontend/src/components/`.
   - Referencia: [architectural-governance.md](.agents/rules/architectural-governance.md).
8. **Sincronización Autónoma (`doc-architect`):**
   - Al concluir cualquier implementación, actualizar sistemáticamente `docs/tasks.md` (`[x]`), `docs/project/product_backlog/`, `docs/project/sprints/`, diagramas en `docs/project/architecture/`, `docs/sdd/`, `docs/backoffice/`, manuales y `docs/testing/inventario_de_pruebas.md`.
   - Referencia: [multi-agent-squad-protocol.md](.agents/rules/multi-agent-squad-protocol.md).
9. **Gobernanza de Testing (`ADR-0017`):**
   - Cumplir la matriz de 4 niveles (Dominio, Componentes, Integración Modulith, E2E Playwright POM en `frontend/e2e/`).
   - Prohibido `waitForTimeout()`/`sleep()` en E2E y cerrar tareas sin catalogar pruebas en [inventario_de_pruebas.md](docs/testing/inventario_de_pruebas.md).
   - Referencia: [testing-governance.md](.agents/rules/testing-governance.md).

## 3. Git Flow, 4 Ambientes, Worktrees y Quality Gates
- **Ramas y Tags (`ci.yml`, `cd-staging.yml`, `cd-production.yml`):**
  1. **Desarrollo (`dev`):** `feat/<modulo>-<tarea>` $\rightarrow$ PR hacia `develop`. El CI corre en PRs hacia `develop`, `release/**`, `main` y `workflow_dispatch` (0 ejecuciones automáticas en push directo a `feat/*` o `develop`).
  2. **Testing Local (`testing-local`):** `release/sprint-X` sobre `docker/docker-compose.testing-local.yml`.
  3. **Staging Nube (`staging`):** Tags `vX.Y.Z-rc.N` o `vX.Y.Z-staging.N` disparan `cd-staging.yml` (`linux/arm64` + Trivy + GHCR + OCI).
  4. **Producción (`prod`):** PR `release/sprint-X` $\rightarrow$ `main` + tag final `vX.Y.Z` (dispara `cd-production.yml` con aprobación manual). Prohibido *push* directo a `main`.
- **Anti-Colisión Multi-Agente:**
  - **Flyway dinámico:** Preasignar `V(N+1)` secuencial (migración actual: `V24`); nunca modificar migraciones ya aplicadas.
  - **Worktrees:** Los subagentes de construcción operan en worktrees aislados de Antigravity (`Workspace: "branch"` o `"share"`) acotados a su módulo/app; orquestación y `doc-architect` operan en el workspace principal.
- **Commits y `git push`:**
  - Usar Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`).
  - **PROHIBIDO ejecutar `git push` autónomo** sin orden directa y explícita del usuario.
- **Comandos de Calidad Obligatorios (`.github/workflows/ci.yml`):**
  - **Backend:** `cd backend && ./mvnw clean test` (y `./mvnw -B clean test spotbugs:check -Psecurity`)
  - **ERP Frontend:** `cd frontend && npm run lint && npm test -- --watch=false` (y `npm run build`)
  - **Backoffice React:** `cd backoffice-frontend && npm run lint && npm test -- --run && npm run build`
  - **DevSecOps:** `./scripts/security/run-security-scan.sh --quick`
  - **E2E Playwright POM:** `./scripts/run-e2e.sh`

## 4. Comandos Antigravity
- `/plan`: Diseñar y acordar el plan técnico antes de implementar tareas complejas.
- `/grill-me`: Entrevistar al usuario para destrabar reglas de negocio en co-diseño.
- `/boost`: Para concurrencia avanzada, Kardex o sincronización offline Dexie.
- `/learn`: Para persistir preferencias o patrones corregidos por el usuario.
