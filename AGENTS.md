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


## 3. Protocolo de Trabajo en Git y DevOps Git Flow
- **Estrategia de Ramas:**
  - **`main`:** Reservada estrictamente para releases finales certificados y producción con tags SemVer (`v0.3.0`). Prohibido commits directos.
  - **`develop`:** Rama base de integración continua del Sprint activo. Toda rama de funcionalidad nace de `develop` y se reintegra a `develop`.
  - **`feat/<modulo>-<tarea>`:** Ramas efímeras de trabajo atómico para cada tarea de `docs/tasks.md` (ej. `feat/config-task-312`, `feat/company-task-334`).
  - **`release/sprint-X`:** Rama de congelamiento y estabilización pre-producción para certificación E2E Playwright previa a mergear a `main`.
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