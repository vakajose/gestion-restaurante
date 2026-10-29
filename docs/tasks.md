---
title: "Sprint 3 Backlog — Parametrización, Experiencia y Flujo de Negocio (Sprint Activo)"
category: "tasks"
sprint: 3
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-3
  - execution-plan
  - agents
last_updated: "2026-10-28"
---

# 🚀 Sprint 3 Backlog — Parametrización, Experiencia y Flujo de Negocio (Sprint Activo)

> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./project/sprints/README.md) | [Product Backlog (Por Prioridades)](./project/product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1 (Cerrado)](./project/sprints/sprint_01_backlog.md) | [Sprint 2 (Cerrado)](./project/sprints/sprint_02_backlog.md)  
> **Sprints Futuros:** [Sprint 4 (Planificado)](./project/sprints/sprint_04_planning.md) | [Sprint 5 (Planificado)](./project/sprints/sprint_05_planning.md) | [DevOps Cloud](./project/sprints/devops_infra_backlog.md)

---

## 📋 Reglas de Ejecución y Protocolo de Calidad (DoD)

El desarrollo se ejecuta de manera incremental y atómica. Cada tarea se implementa en su propia rama Git con la convención:
`git checkout -b feat/<modulo>-<tarea>` o `refactor/<modulo>-<tarea>`, y commits bajo el estándar Conventional Commits.
Antes de dar por concluida cualquier tarea:
- Backend: `./mvnw clean test` (100% pruebas unitarias e integración pasando sin fallos).
- Frontend: `npm run lint && npm test` (100% pruebas unitarias pasando en Vitest/JSDOM).
- E2E: `./scripts/run-e2e.sh` (100% pruebas de regresión Playwright pasando).
- SDD: Sincronización obligatoria de diagramas Mermaid si se alteran APIs, base de datos o flujos.

> [!IMPORTANT]
> **Convención de Títulos:** Cada tarea debe contener en su título dos etiquetas canónicas:
> 1. `[MODULO_CONCEPTUAL]`: Módulo o capa (`[GLOBAL]`, `[CORE]`, `[CONFIG]`, `[AUTH]`, `[COMPANY]`, `[POS]`, `[INVENTORY]`, `[FINANCE]`, `[CATALOG]`, `[STORAGE]`, `[SALON]`, `[UI/UX]`, `[DEVOPS]`, `[DOCS]`, `[QA]`).
> 2. `[TIPO_CAMBIO]`: Tipo de trabajo (`[NEGOCIO]`, `[ARQUITECTURA]`, `[DISEÑO]`, `[SEGURIDAD]`, `[UI/UX]`, `[BD]`, `[INFRA]`, `[CALIDAD]`, `[BI]`).

---

## 🎯 Tareas del Sprint 3 (En Curso)

### 1. Sistema Unificado de Parametrización Multinivel (PB-026 / UC-311)

- [x] **TASK-311 [CONFIG] [DISEÑO]:** Especificación Técnica y Arquitectura del Sistema de Parametrización Multinivel (PB-026 / UC-311).
  - *Objetivo:* Diseñar el modelo de datos (híbrido relacional/JSONB vs KV), catálogo tipado de metadatos de configuración (claves, tipos, defaults, scope `GLOBAL | TENANT | BRANCH`, categorías `POS`, `INVENTORY`, `FINANCE`, `SECURITY`), protocolo de hot-reload/invalidación de caché en Spring/Redis y wireframes de la consola estilo VS Code con buscador jerárquico.
  - *Criterio de Entrega:* Documento SDD en `docs/project/architecture/` con diagramas de clase y secuencia de resolución de parámetros. Formalizado en ADR-0008, ERD unificado, secuencia de fallback y manual de proceso operativo.

- [x] **TASK-312 [CONFIG] [ARQUITECTURA]:** Motor de Parametrización Dinámica en Backend con Hot-Reload y Metadatos de Validación (PB-026).
  - *Objetivo:* Implementar módulo `modules.configuration` con contrato `ConfigurationPublicApi`, endpoints REST para consulta/mutación tipada y caché concurrente invalidable en caliente sin reiniciar la aplicación.
  - *Criterio de Entrega:* 100% pruebas unitarias e integración en Spring Boot validando herencia jerárquica y hot-reload.

- [x] **TASK-313 [CONFIG] [UI/UX]:** Centro de Gestión y Búsqueda de Parámetros Jerárquicos en Frontend TailwindAdmin (PB-026).
  - *Objetivo:* Implementar vista interactiva `/settings/parameters` con buscador en tiempo real, árbol de categorías por módulo, badges de ámbito (Global, Empresa, Sucursal) y guardado reactivo con feedback inmediato.
  - *Criterio de Entrega:* Búsqueda y modificación de parámetros funcional y cubierta por pruebas unitarias de componentes.

---

### 2. Alta Rápida de Empresa y Asistente de Onboarding (PB-001 / PB-040 / UC-333)

- [x] **TASK-333 [COMPANY] [DISEÑO]:** Especificación del Asistente de Onboarding (Alta Rápida de Empresa, Sucursal Principal y Parámetros Semilla) (PB-001 / UC-333).
  - *Objetivo:* Diseñar el flujo de alta guiada de empresa (self-service y backoffice), formulario simplificado de 3 a 5 preguntas clave (identidad fiscal, sucursal principal con timezone según ADR-0006, moneda y selección de preset operativo: Fast Food, A la carta, Bar), contrato de DTOs, diagrama de secuencia de transacción atómica y asignación de parámetros semilla mediante Sparse Storage.
  - *Criterio de Entrega:* Especificación técnica completada y verificada en `docs/project/architecture/` (ADR-0009, `sequence_tenant_onboarding.md`, `state_tenant_lifecycle.md`, `use_cases_onboarding.md`, manual operativo en `docs/business-processes/04-administracion-y-personal/02_flujo_alta_empresa_y_onboarding.md`, actualización de `database_erd.md` y schemas SDD).

- [x] **TASK-334 [COMPANY] [NEGOCIO]:** Implementación de Onboarding Atómico de Empresa, Sucursal Principal y Presets Operativos en Backend (PB-001).
  - *Objetivo:* Implementar servicio `OnboardingService` en `modules.company` ejecutando en una única transacción `@Transactional`: creación de `Tenant`, `Branch` principal, `AppUser` administrador con Argon2id, y aplicación de parámetros semilla en `system_parameter_values` vía `ConfigurationPublicApi`. Endpoint público `POST /api/v1/auth/onboarding` y administrativo `POST /api/v1/backoffice/tenants`.
  - *Criterio de Entrega:* 100% de pruebas unitarias e integración en Spring Boot verificando la transacción atómica, generación de slug y emisión de token JWT.

- [x] **TASK-335 [COMPANY] [UI/UX]:** Asistente Visual de Onboarding (Wizard 3 Pasos) en Frontend TailwindAdmin (PB-001).
  - *Objetivo:* Implementar vista `/onboarding` con asistente visual paso a paso: 1) Datos de restaurante y fiscales, 2) Sucursal principal y moneda, 3) Selección de perfil operativo con tarjetas interactivas (Comida rápida, A la carta, Bar). Validación reactiva, microinteracciones y redirección inmediata al POS o Dashboard tras el alta exitosa.
  - *Criterio de Entrega:* Flujo completo de registro operativo en navegador y cubierto por pruebas unitarias de componentes en Vitest.

- [x] **TASK-339 [UI/UX] [NEGOCIO]:** Tour Guiado Interactivo de Primera Experiencia (Product Tour Onboarding) en Frontend (PB-040).
  - *Prioridad:* `P2 - Media (Sprint 3)`
  - *Objetivo:* Implementar motor de tour guiado interactivo (Driver.js adaptado a Angular Zoneless) que resalta 4 acciones críticas para el nuevo dueño tras completar el onboarding (apertura de turno, cobro de plato demo, verificación en Kardex, subida de catálogo). Persistencia de estado en `user_preferences` (`onboarding_tour_completed`).
  - *Criterio de Entrega:* Tour interactivo responsivo con opción de saltar o reiniciar, probado con unit tests de componentes.

---

### 3. Gestión Dinámica de Roles Personalizados y Matriz RBAC (PB-039 / UC-336)

- [x] **TASK-336 [AUTH] [DISEÑO]:** Especificación de Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y Matriz RBAC (PB-039 / UC-336).
  - *Prioridad:* `P1 - Alta (A diseñar inmediatamente tras TASK-333)`
  - *Objetivo:* Diseñar el modelo de datos relacional para roles personalizados (`roles`, `role_permissions`), asignación multi-sucursal (`user_branches`), expiración temporal (`role_valid_until`), reseteo delegado de contraseñas (`must_change_password`), catálogo de permisos atómicos y DTOs/secuencias de resolución.
  - *Criterio de Entrega:* Especificación técnica formal completada y verificada en `docs/project/architecture/` (ADR-0010, `sequence_rbac_resolution_and_enforcement.md`, `use_cases_rbac.md`, manual operativo en `docs/business-processes/04-administracion-y-personal/03_flujo_gestion_roles_y_privilegios.md`, actualización de `database_erd.md`, `c4_level3_backend_modulith.md` y schemas SDD).

- [x] **TASK-337 [AUTH] [SEGURIDAD]:** Implementación de Roles Dinámicos, Privilegios Agrupados y Endpoints RBAC en Backend (PB-039).
  - *Objetivo:* Endpoints REST para CRUD de roles personalizados por tenant, asignación atómica de privilegios, validación de inmutabilidad del rol `ADMIN_TENANT`, integración con `UserPublicApi` y propagación de permisos calculados en los claims del token JWT.
  - *Criterio de Entrega:* 100% de pruebas unitarias e integración en Spring Modulith verificando aislamiento multi-tenant y seguridad de endpoints.

- [x] **TASK-338 [AUTH] [UI/UX]:** Centro de Administración de Roles, Permisos y Asignación a Colaboradores en Frontend (PB-039).
  - *Objetivo:* Implementar vista interactiva `/users/roles` en TailwindAdmin v2.0: creación de roles personalizados con matriz de switches agrupados por módulo funcional, modal de asignación rápida a usuarios y directivas estructurales (`*hasPrivilege`) para ocultar/habilitar acciones en tiempo real.
  - *Criterio de Entrega:* Gestión completa de roles operativa en navegador y cubierta por pruebas unitarias de componentes en Vitest.

---

### 4. Ciclo de Vida de Sesiones y Silent Refresh (PB-027 / UC-314)

- [x] **TASK-314 [AUTH] [DISEÑO]:** Especificación del Ciclo de Vida de Sesiones, Expiración JWT y Renovación Silenciosa (PB-027 / UC-314).
  - *Objetivo:* Diseñar la arquitectura de tokens duales (Access Token corto 15m + Refresh Token rotativo en BD), expiración por inactividad vinculada al parámetro `SESSION_INACTIVITY_TIMEOUT_MINUTES`, detección de reúso de sesión y Silent Refresh reactivo con cola RxJS en frontend.
  - *Criterio de Entrega:* Especificación técnica completada y verificada en `docs/project/architecture/` (ADR-0011, `use_cases_sessions.md`, `sequence_session_lifecycle_and_refresh.md`, manual operativo en `docs/business-processes/04-administracion-y-personal/04_flujo_ciclo_vida_sesiones_y_seguridad.md`, actualización de `database_erd.md`, `c4_level3_backend_modulith.md`, `c4_level3_frontend_angular.md` y schemas SDD).

- [x] **TASK-315 [AUTH] [SEGURIDAD]:** Implementación de Silent Refresh, Timeout Configurable y Revocación de Sesiones (PB-027).
  - *Objetivo:* Endpoints de refresco y revocación en backend, interceptor HTTP en Angular para renovación transparente en segundo plano, modal de aviso por inactividad y panel de sesiones en "Mi Perfil".
  - *Criterio de Entrega:* Sesión expira de forma determinista ante inactividad y se renueva transparentemente durante el uso activo.

---

### 5. Adaptabilidad Móvil y Diseño Responsivo Integral (PB-028)

- [ ] **TASK-316 [UI/UX] [DISEÑO]:** Especificación de Adaptabilidad Móvil y Breakpoints para Vistas Críticas de Restaurante (PB-028).
  - *Objetivo:* Diseñar la experiencia móvil para Punto de Venta (POS táctil, bottom-sheet para comanda) e Inventario (tarjetas apilables para Kardex y conteo físico).
  - *Criterio de Entrega:* Guía de diseño responsivo y especificación de componentes adaptativos en TailwindAdmin.

- [ ] **TASK-317 [UI/UX] [ARQUITECTURA]:** Implementación de Vistas Responsivas Adaptativas para Punto de Venta (POS) e Inventario (PB-028).
  - *Objetivo:* Aplicar diseño responsivo en POS (cuadrícula flexible con carrito deslizable inferior en móviles) y vistas de stock/Kardex utilizables desde smartphones.
  - *Criterio de Entrega:* Flujo completo de venta POS y ajuste de stock operable en viewport de 375px (iPhone SE).
