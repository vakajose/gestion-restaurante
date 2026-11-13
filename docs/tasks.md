---
title: "Sprint 6 Backlog — Tareas Pendientes, Estabilización y Release (release/sprint-6)"
category: "tasks"
sprint: 6
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-6
  - release
  - testing-local
  - documentation
last_updated: "2026-11-13"
---

# 🚀 Sprint 6 — Tareas Pendientes y Depuración (`release/sprint-6`)

> **Rama Activa de Pruebas y Estabilización:** `release/sprint-6` (entorno `testing-local`)  
> **Histórico de Tareas Completadas del Sprint 6:** [`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md)  
> **Documento Maestro de Planificación:** [`sprint_06_planning.md`](./project/sprints/sprint_06_planning.md)  
> ⬅️ **Navegación:** [Bóveda de Sprints](./project/sprints/README.md) | [Product Backlog](./project/product_backlog/README.md)

---

## 📋 Reglas de Ejecución (`release/sprint-6`)

1. **Aislamiento de `TASK-609`:** La provisión de Oracle Cloud (`TASK-609`) se trabaja en paralelo en otro chat/worktree; no modificar infraestructura OCI en esta rama.
2. **Protocolo Git & Quality Gates:** Corregir hallazgos de forma ordenada sobre `release/sprint-6`, validando `./mvnw clean test` (backend), `npm run lint && npm test` (ERP y Backoffice), sin ejecutar `git push` autónomo.
3. **Objetivo de Cierre:** Dejar el producto y toda la documentación del proyecto completamente alineados y listos para promover a `staging` en nube.

---

## 🎯 Tareas Pendientes

### 1. Documentación de Arranque, Manual Operativo Inicial y README Principal

- [x] **TASK-611 [DOCS/MANUAL] [ONBOARDING-OPS]:** Manual de Bootstrap desde Cero (`testing-local` / Go-Live), Primeras Configuraciones en Backoffice y Actualización del `README.md` Principal.
  - *Prioridad:* `P0 - Crítica` (prerequisito antes de iniciar pruebas manuales).
  - *Objetivo:*
    1. Actualizar el [`README.md`](../README.md) principal del repositorio para reflejar la arquitectura actual de 3 aplicaciones (`backend/`, `frontend/` ERP Angular y `backoffice-frontend/` React), los 4 ambientes (`dev`, `testing-local`, `staging`, `prod`) y los accesos de cada entorno.
    2. Redactar un manual claro de **arranque desde cero y configuración inicial** que cubra: variables `.env` por ambiente/contenedor, secuencia de bootstrap automático (PostgreSQL + Flyway + Superadmin), y checklist de primeras configuraciones obligatorias en Backoffice (`/platform/settings`, prueba de Storage, revisión de `/plans`, gestión de `/platform/admins` y verificación post-onboarding en `/tenants`).

---

### 2. Depuración Pre-Bootstrap Detectada en ERP Frontend

- [x] **TASK-612 [BUG/ERP FE] [SEGURIDAD/UX]:** Ocultar Accesos Rápidos Demo y Prellenado `demo-restaurant` en Login ERP fuera de `dev`.
  - *Prioridad:* `P1 - Alta`
  - *Hallazgo:* En [`login.component.html`](../frontend/src/app/features/auth/login/login.component.html#L185-L278) y [`login.component.ts`](../frontend/src/app/features/auth/login/login.component.ts#L29), el bloque *"Acceso Rápido a Empresas Demo"* (`fillDemoAdmin()`, `fillDemoCashier()`, etc.) y el valor inicial `demo-restaurant` se renderizan incondicionalmente en todos los entornos, cuando en `testing-local`, `staging` y `prod` los seeders demo están deshabilitados (`@Profile({"dev", "test"})`).
  - *Objetivo:* Condicionar la visibilidad de los botones demo y el prellenado al entorno `dev` (ej. `isDevMode()` de Angular) y actualizar sus tests unitarios correspondientes.

---

### 3. Provisión Guiada Oracle Cloud (OCI), IaC y Certificación Staging Nube (En Paralelo)

- [ ] **TASK-609 [CLOUD / IaC] [OCI]:** Provisión Guiada de Oracle Cloud Infrastructure (VCN, IAM, Compute, Object Storage, Presupuestos) y Certificación Smoke Staging Nube (PB-025).
  - *Prioridad:* `P1 - Alta`
  - *Nota:* Se ejecuta en chat/worktree independiente. No intervenir desde `release/sprint-6`.
  - *Objetivo:* Aprovisionar VCN, VM 1 (Apps ARM64), VM 2 (PostgreSQL 16), buckets privados de medios/backups, Customer Secret Keys S3, Secrets de GitHub Actions y ejecutar certificación Smoke HTTPS en Staging.

---

### 4. Depuración Conjunta Sprint 5 & Sprint 6 + Alineación Documental Completa

- [ ] **TASK-613 [QA/DOCS] [CIERRE-SPRINT]:** Auditoría, Depuración y Sincronización Total de Documentación (Proyecto, SDD, Proceso SDLC y Definiciones Agénticas).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Al finalizar las pruebas conjuntas de Sprint 5 y Sprint 6, depurar información incoherente u obsoleta, sintetizar documentos redundantes y alinear desde el `README.md` principal hasta las especificaciones SDD, `AGENTS.md`, skills y reglas agénticas con el estado real del producto.

---

## 🔍 Tareas Generadas por Triage de `observaciones_y_hallazgos.md` (Ordenadas por Prioridad)

### Prioridad Crítica (`P0`)

- [ ] **TASK-625 [CRÍTICA] [BACKOFFICE - INQUILINOS/USUARIOS]:** Listado, Vista de Estado y Gestión de Usuarios del Tenant (incluyendo Cambio/Restablecimiento de Contraseña) en `/tenants/:id`.
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* Añadir una pestaña/vista de usuarios dentro del detalle del inquilino (`/tenants/:id`) en Backoffice para listar los usuarios del tenant, ver su estado (activo/bloqueado) y permitir restablecer/cambiar su contraseña ante bloqueos o errores de acceso.

- [x] **TASK-626 [BUG/CRÍTICA] [ERP - FINANZAS] (`OBS-011`):** Corregir `500 Internal Server Error` (`could not determine data type of parameter $4`) en Finanzas y Arqueo (`/finance`).
  - *Prioridad:* `P0 - Crítica`
  - *Origen:* [`OBS-011`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Corregir la consulta de `cash_shifts` cuando los filtros opcionales (`branchId`, `openedAt` desde/hasta, `openedBy`, `status`, `difference`) llegan en `null`, evitando el error de inferencia de tipo en PostgreSQL.

### Prioridad Alta (`P1`)

- [ ] **TASK-614 [BUG/CAMBIO] [ERP - SUCURSALES] (`OBS-003`):** Corregir Flujo de Creación de Nueva Sucursal, Inicialización y Actualización del Selector de Sucursal.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-003`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Revisar y corregir la creación de sucursales para que inicialice correctamente sus parámetros/contexto y actualice reactivamente el selector de sucursales en la UI.

### Prioridad Media (`P2`)

- [x] **TASK-615 [BUG] [ERP - CONFIGURACIÓN] (`OBS-001`):** Corregir Alerta `Unknown Error` en Parámetros del Sistema al Navegar entre Empresa y Sucursales.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-001`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diagnosticar y eliminar la petición fallida / cancelada que dispara el toast `Unknown Error` al alternar entre alcance Empresa y Sucursales en `/settings/parameters`.

- [ ] **TASK-616 [MEJORA/UI-UX] [BACKOFFICE - OBSERVABILIDAD] (`OBS-007`):** Buscador en Loggers en Caliente + Filtro por Detalle, Visor JSON Formateado y Paginación en Auditoría Inmutable.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-007`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Añadir buscador reactivo en la pestaña *Loggers en Caliente*; en *Auditoría Inmutable* (`platform_audit_logs`), incorporar búsqueda sobre detalle, visor/modal con JSON formateado e interactivo, y paginación real en backend y frontend.

- [ ] **TASK-617 [CAMBIO/UX] [BACKOFFICE - INQUILINOS] (`OBS-009`):** Hacer Opcionales Dirección de Sucursal y Email del Admin al Aprovisionar Nuevo Inquilino.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-009`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Alinear el modal *Aprovisionar Nuevo Inquilino* (`/tenants`) y su DTO/servicio backend con el self-onboarding del ERP permitiendo omitir dirección y correo del administrador inicial.

- [ ] **TASK-618 [MEJORA/CAMBIO] [BACKOFFICE - INQUILINOS] (`OBS-010`):** Estado Inicial Diferenciador para Inquilinos Recién Aprovisionados Sin Primer Ingreso.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-010`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diferenciar visualmente y en modelo de estado cuando un tenant fue aprovisionado desde Backoffice pero aún no ha iniciado sesión ni operado por primera vez (ej. *Pendiente de activación* / *Sin primer ingreso*).

- [ ] **TASK-620 [ARQ/REFACTOR] [FRONTENDS] (`OBS-005`):** Auditoría de Reutilización de Componentes Visuales (React Backoffice + Angular ERP) y Regla de Diseño en Proceso SDLC.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-005`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Escanear componentes repetidos (tarjetas, tablas, modales) en `backoffice-frontend/` y `frontend/`, factorizar los casos justificados sin crear componentes monolíticos y formalizar el criterio como regla del proceso de desarrollo frontend.

### Prioridad Baja (`P3`)

- [ ] **TASK-621 [MEJORA/UX] [ERP - ONBOARDING] (`OBS-002`):** Confirmación de Contraseña en Self-Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-002`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Añadir campo de confirmación de contraseña con validación de coincidencia al registrar el administrador principal en `/onboarding`.

- [ ] **TASK-622 [UI/UX] [BACKOFFICE] (`OBS-004`):** Soporte de Modo Oscuro en `backoffice-frontend/`.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-004`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Implementar selector y persistencia de tema Claro / Oscuro en la SPA React del Backoffice.

- [ ] **TASK-623 [MEJORA/UX] [BACKOFFICE - PLANES] (`OBS-006`):** UX de Límites Ilimitados Gratis y Categorización/Visibilidad de Planes para Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-006`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Clarificar/facilitar la creación de planes gratuitos ilimitados en `/plans` y añadir categorización/visibilidad para separar planes públicos de planes especiales/internos en el futuro flujo de Onboarding.

- [ ] **TASK-624 [CAMBIO/MEJORA] [BACKOFFICE - ADMINS] (`OBS-008`):** Email Opcional y Política de Contraseñas Configurable para Administradores de Plataforma.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-008`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Permitir crear `platform_admins` con `email` opcional y exponer configuración en Backoffice para flexibilizar la política de complejidad de contraseñas de plataforma.

- [ ] **TASK-627 [MEJORA/UX] [ERP - POS] (`OBS-012`):** Advertencia de Confirmación al Abrir Turno sin Platos en el Catálogo.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-012`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Mostrar un aviso/confirmación en POS al intentar abrir turno cuando el restaurante no tiene platos registrados (*"Vas a abrir tu turno sin ningún plato registrado. ¿Estás seguro?"*), permitiendo continuar si el usuario confirma.
