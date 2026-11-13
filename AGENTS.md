# AGENTS.md - Protocolo Operativo para Antigravity

## 1. Alcance y Límites Técnicos
- **Entorno Host / OS:** WSL2 sobre Linux Fedora (gestor de paquetes `dnf`). Cliente `postgresql` (`psql`) instalado localmente para diagnósticos de base de datos.
- **Backend:** Java 25 LTS (Amazon Corretto), Spring Boot 4.1.x, Spring Modulith, Maven, PostgreSQL, Flyway.
- **Frontend:** Angular 22 (Zoneless, Signal Forms, Standalone), Tailwind CSS (Plantilla TailwindAdmin), Dexie.js (IndexedDB).
- **Modelo de Aislamiento:** Toda entidad transaccional exige `tenant_id` (UUID) y `branch_id` (UUID). El contexto se obtiene de `TenantContextHolder` (propagado por JWT).

## 2. Reglas Arquitectónicas Inviolables
1. **Validación de Límites Modulares:** La compilación debe validar `ApplicationModules.of(RestaurantApplication.class).verify()`. No se permiten dependencias circulares entre paquetes ni inyecciones de repositorios de otros módulos o de `core` (ej. `BranchRepository`, `AppUserRepository`). Todo consumo intermodular o hacia `core` debe realizarse mediante interfaces públicas `.api` (ej. `BranchPublicApi`, `UserPublicApi`). En frontend, la persistencia en Dexie.js debe aislarse mediante una capa de repositorios locales (DAL). Ver [.agents/rules/architectural-governance.md](file:///home/jvaca/proyectos/gestion-restaurante/.agents/rules/architectural-governance.md).
2. **Comunicación Asíncrona / Eventos:**
   - La venta en POS publica `OrderPaidEvent`.
   - El Kardex consume el evento mediante `@ApplicationModuleListener` para aplicar deducciones de recetas atómicamente.
3. **Manejo Offline First:** Las mutaciones en POS deben persistirse en Dexie.js localmente antes de invocar la API mediante `client_transaction_id` idempotente.
4. **Manejo Estricto de Fechas y Zonas Horarias (ADR-0006):**
   - **Fechas de calendario puras (sin hora):** Usar `DATE` en PostgreSQL, `java.time.LocalDate` en Java y string ISO `"YYYY-MM-DD"` en JSON/TypeScript. Prohibido agregar hora o zona a campos `DATE`. Formateo UI: `dd/MM/yyyy`.
   - **Marcas de tiempo transaccionales:** Usar `TIMESTAMPTZ` en PostgreSQL, `java.time.Instant` en Java y string ISO UTC con sufijo `Z` en JSON.
   - **Hora Local de Sucursal:** Los cortes de turno y consultas de ventas del día se calculan convirtiendo el timestamp a la zona horaria de la sucursal (`branches.timezone`, default `'America/La_Paz'`). La UI siempre muestra la hora en el huso local de la sucursal.
5. **Specification-Driven Development (SDD) y Ciclo de Vida SDD-SDLC (ADR-0007):** Toda nueva funcionalidad, modificación de modelos de datos o contratos `.api` debe cumplir obligatoriamente el ciclo de 4 fases (1: Elicitación/Análisis $\rightarrow$ 2: Diseño Arquitectónico/Spec-First $\rightarrow$ 3: Construcción $\rightarrow$ 4: Sincronización/Sync-Always). Los diagramas Mermaid en `docs/project/architecture/` son artefactos vivos de especificación. Ninguna tarea se da por concluida sin sincronizar sus diagramas correspondientes. Solo hotfixes críticos o ajustes triviales pueden usar Fast-Track (iniciando en Construcción con sincronización documental obligatoria posterior). Ver [ADR-0007](docs/adr/0007_sdd_software_development_lifecycle.md) y [.agents/skills/sdd-architecture-diagramming/SKILL.md](.agents/skills/sdd-architecture-diagramming/SKILL.md).
6. **Gobernanza Multi-Agente y Sincronización Autónoma (`doc-architect`):** Toda implementación técnica (backend/frontend) concluye de forma autónoma e inmediata con la fase de sincronización documental a cargo de `doc-architect`. Es una regla obligatoria e inviolable: el agente orquestador DEBE actualizar `docs/tasks.md` (`[x]`), los backlogs en `docs/backlog/`, los diagramas Mermaid afectados, las especificaciones SDD y el inventario de pruebas de manera sistemática, sin que el usuario tenga que solicitarlo o recordarlo en el prompt. Ver [.agents/rules/multi-agent-squad-protocol.md](.agents/rules/multi-agent-squad-protocol.md).
7. **Gobernanza de Testing Automatizado y Calidad Continua (ADR-0017):** Toda nueva funcionalidad o modificación a un módulo existente debe someterse a la Matriz de los 4 Niveles de Verificación (Nivel 1 Dominio, Nivel 2 Componentes, Nivel 3 Integración Modular Spring Modulith, Nivel 4 E2E Playwright con Page Object Model). Prohibido esperas arbitrarias (`waitForTimeout`/`sleep`) en E2E y prohibido cerrar tareas sin catalogar las pruebas en `docs/testing/inventario_de_pruebas.md`. Ver [.agents/rules/testing-governance.md](.agents/rules/testing-governance.md).
8. **Co-Diseño Interactivo Obligatorio en Análisis y Diseño (Fases 1 y 2):** El análisis de requerimientos y el diseño arquitectónico (Fases 1 y 2 de SDD-SDLC) son **sesiones de trabajo colaborativas y en pareja (Pair Programming) entre el usuario y el agente**.
   - **PROHIBIDO:** Que el agente tome decisiones arquitectónicas, redacte especificaciones definitivas o congele ADRs y diagramas de forma unilateral o autónoma sin consultar.
   - **PROTOCOLO OBLIGATORIO:** En Fases 1 y 2, el agente debe seguir estrictamente el ciclo interactivo:
     1. **Proponer:** Presentar el problema, alternativas y propuesta inicial de solución.
     2. **Discutir:** Entrevistar y debatir con el usuario reglas de negocio, casos de borde y trade-offs.
     3. **Definir:** Acordar conjuntamente la solución técnica y de producto.
     4. **Confirmar:** Obtener la validación explícita del usuario antes de formalizar y persistir cualquier ADR, diagrama Mermaid o especificación SDD.
   - **EXCEPCIÓN ÚNICA:** El agente solo puede realizar análisis y diseño autónomo si el usuario lo ordena de manera directa y explícita en su prompt.
9. **Evaluación Consciente de Librerías vs Implementación Propia:** Cuando se aborden funcionalidades estándar o generales (recorte de fotos, galerías, utilidades), el agente tiene prohibido decidir unilateralmente si usar una librería externa o programar una solución propia. Debe investigar opciones consolidadas de la comunidad y diseñar una alternativa propia optimizada, presentando pros, contras y peso en bundle al usuario para decidir conjuntamente. Objetivo: no reinventar la rueda, pero evitar librerías pesadas (*bloatware*) con componentes no utilizados. Ver [.agents/rules/architectural-governance.md](file:///home/jvaca/proyectos/gestion-restaurante/.agents/rules/architectural-governance.md).



## 3. Protocolo de Trabajo en Git, 4 Ambientes y DevOps Git Flow (`TASK-610`)
- **Estrategia de Ramas y Tags por Ambiente (4 Ambientes):**
  1. **Desarrollo (`dev`):**
     - **`feat/<modulo>-<tarea>`:** Ramas efímeras de trabajo atómico nacidas de `develop` (`0` ejecuciones en GitHub Actions).
     - **`develop`:** Rama base de integración continua del Sprint activo. Hacer `push` directo a `develop` **no dispara** builds de Staging ni pipelines pesados (`0` desperdicio de cuota); el CI se ejecuta en Pull Requests hacia `develop` o bajo demanda (`workflow_dispatch`).
  2. **Testing Local (`testing-local`):**
     - **`release/sprint-X`:** Rama de congelamiento y pruebas locales de usuario sobre `docker-compose.testing-local.yml`. Los pushes a `release/*` **no disparan** despliegue a Staging Nube ni a Producción; los fixes encontrados en testing local se suben a esta rama (con CI en PRs si aplica).
  3. **Staging Nube (`staging`):**
     - **Tags `vX.Y.Z-rc.N` o `vX.Y.Z-staging.N`** (ej. `v0.6.0-rc.1`): Únicamente al pushear un tag de pre-release (o vía `workflow_dispatch`) se dispara `cd-staging.yml` (build `linux/arm64` + Trivy + GHCR + deploy a OCI Staging). Si aparecen bugs en nube, se corrigen en `release/sprint-X` y se emite `-rc.2`.
  4. **Producción (`prod`):**
     - **`main` + Tag final `vX.Y.Z` (sin sufijos `-rc`/`-staging`):** Prohibido el *push* directo a `main` (protegido por **GitHub Ruleset** exigiendo Pull Request + status checks de `CI Pipeline` + aprobación). Una vez mergeado `release/sprint-X` $\rightarrow$ `main` vía PR, el tag SemVer final `vX.Y.Z` dispara `cd-production.yml`, el cual verifica que el tag pertenezca a `main` y exige aprobación manual en el *Environment* `production` antes de desplegar.
- **Gobernanza Anti-Colisión Multi-Agente:**
  - **Partición de Migraciones Flyway:** Asignación secuencial preestablecida para evitar colisiones (`V5` Parametría, `V6` Onboarding, `V7` Roles RBAC, `V8` Sesiones).
  - **Límites de Paquetes Modulith:** Cada agente trabaja exclusivamente en su paquete (`modules.configuration`, `modules.company`, `core.security`) consumiendo otros módulos únicamente por `.api`.
  - **Git Worktrees:** Cada subagente de implementación opera en su propio worktree aislado en el sistema de archivos (`../wt_<modulo>_<tarea>`), permitiendo compilación, pruebas y commits independientes sin bloqueos.
- Commits siguiendo Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`).
- Antes de entregar e integrar la rama en `develop`:
  - `./mvnw clean test` (Backend: 100% de tests pasando sin warnings).
  - `npm run lint && npm test` (Frontend: validación estricta de tipos y linting).
- **Prohibición de `git push` autónomo:** Antigravity NUNCA debe ejecutar `git push` hacia ningún repositorio remoto de forma autónoma. Solo se ejecutará si el usuario lo ordena de manera directa y explícita. El usuario siempre revisará localmente antes de subir los cambios.

## 4. Guía de Interacción y Comandos Antigravity
- `/plan`: Utilizar obligatoriamente antes de iniciar la implementación de cualquier tarea compleja para diseñar y acordar el plan técnico.
- `/grill-me`: Recomendar cuando se requiera entrevistar al usuario para destrabar reglas de negocio específicas (ej. arqueo, recetas, promociones).
- `/boost`: Recomendar cuando se aborden problemas de alta complejidad (concurrencia, optimización de consultas Kardex, sincronización Dexie).
- `/learn`: Usar para registrar y persistir preferencias de diseño o patrones corregidos por el usuario.