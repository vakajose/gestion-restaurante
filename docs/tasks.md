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

- [x] **TASK-625 [CRÍTICA] [BACKOFFICE - INQUILINOS/USUARIOS]:** Listado, Vista de Estado y Gestión de Usuarios del Tenant (incluyendo Cambio/Restablecimiento de Contraseña) en `/tenants/:id`.
  - *Prioridad:* `P0 - Crítica`
  - *Objetivo:* Añadir una pestaña/vista de usuarios dentro del detalle del inquilino (`/tenants/:id`) en Backoffice para listar los usuarios del tenant, ver su estado (activo/bloqueado) y permitir restablecer/cambiar su contraseña ante bloqueos o errores de acceso.

- [x] **TASK-626 [BUG/CRÍTICA] [ERP - FINANZAS] (`OBS-011`):** Corregir `500 Internal Server Error` (`could not determine data type of parameter $4`) en Finanzas y Arqueo (`/finance`).
  - *Prioridad:* `P0 - Crítica`
  - *Origen:* [`OBS-011`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Corregir la consulta de `cash_shifts` cuando los filtros opcionales (`branchId`, `openedAt` desde/hasta, `openedBy`, `status`, `difference`) llegan en `null`, evitando el error de inferencia de tipo en PostgreSQL.

### Prioridad Alta (`P1`)

- [x] **TASK-614 [BUG/CAMBIO] [ERP - SUCURSALES] (`OBS-003`):** Corregir Flujo de Creación de Nueva Sucursal, Inicialización y Actualización del Selector de Sucursal.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-003`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Revisar y corregir la creación de sucursales para que inicialice correctamente sus parámetros/contexto y actualice reactivamente el selector de sucursales en la UI.

### Prioridad Media (`P2`)

- [x] **TASK-615 [BUG] [ERP - CONFIGURACIÓN] (`OBS-001`):** Corregir Alerta `Unknown Error` en Parámetros del Sistema al Navegar entre Empresa y Sucursales.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-001`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diagnosticar y eliminar la petición fallida / cancelada que dispara el toast `Unknown Error` al alternar entre alcance Empresa y Sucursales en `/settings/parameters`.

- [x] **TASK-616 [MEJORA/UI-UX] [BACKOFFICE - OBSERVABILIDAD] (`OBS-007`):** Buscador en Loggers en Caliente + Filtro por Detalle, Visor JSON Formateado y Paginación en Auditoría Inmutable.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-007`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Añadir buscador reactivo en la pestaña *Loggers en Caliente*; en *Auditoría Inmutable* (`platform_audit_logs`), incorporar búsqueda sobre detalle, visor/modal con JSON formateado e interactivo, y paginación real en backend y frontend.

- [x] **TASK-617 [CAMBIO/UX] [BACKOFFICE - INQUILINOS] (`OBS-009`):** Hacer Opcionales Dirección de Sucursal y Email del Admin al Aprovisionar Nuevo Inquilino.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-009`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Alinear el modal *Aprovisionar Nuevo Inquilino* (`/tenants`) y su DTO/servicio backend con el self-onboarding del ERP permitiendo omitir dirección y correo del administrador inicial.

- [x] **TASK-618 [MEJORA/CAMBIO] [BACKOFFICE - INQUILINOS] (`OBS-010`):** Estado Inicial Diferenciador para Inquilinos Recién Aprovisionados Sin Primer Ingreso.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-010`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diferenciar visualmente y en modelo de estado cuando un tenant fue aprovisionado desde Backoffice pero aún no ha iniciado sesión ni operado por primera vez (ej. *Pendiente de activación* / *Sin primer ingreso*).

- [x] **TASK-620 [ARQ/REFACTOR] [FRONTENDS] (`OBS-005`):** Auditoría de Reutilización de Componentes Visuales (React Backoffice + Angular ERP) y Regla de Diseño en Proceso SDLC.
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-005`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Escanear componentes repetidos (tarjetas, tablas, modales) en `backoffice-frontend/` y `frontend/`, factorizar los casos justificados sin crear componentes monolíticos y formalizar el criterio como regla del proceso de desarrollo frontend.

### Prioridad Baja (`P3`)

- [x] **TASK-621 [MEJORA/UX] [ERP - ONBOARDING] (`OBS-002`):** Confirmación de Contraseña en Self-Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-002`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Añadir campo de confirmación de contraseña con validación de coincidencia al registrar el administrador principal en `/onboarding`.

- [x] **TASK-622 [UI/UX] [BACKOFFICE] (`OBS-004`):** Soporte de Modo Oscuro en `backoffice-frontend/`.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-004`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Implementar selector y persistencia de tema Claro / Oscuro en la SPA React del Backoffice.

- [x] **TASK-623 [MEJORA/UX] [BACKOFFICE - PLANES] (`OBS-006`):** UX de Límites Ilimitados Gratis y Categorización/Visibilidad de Planes para Onboarding.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-006`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Clarificar/facilitar la creación de planes gratuitos ilimitados en `/plans` y añadir categorización/visibilidad para separar planes públicos de planes especiales/internos en el futuro flujo de Onboarding.

- [x] **TASK-624 [CAMBIO/MEJORA] [BACKOFFICE - ADMINS] (`OBS-008`):** Email Opcional y Política de Contraseñas Configurable para Administradores de Plataforma.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-008`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Permitir crear `platform_admins` con `email` opcional y exponer configuración en Backoffice para flexibilizar la política de complejidad de contraseñas de plataforma.

- [x] **TASK-627 [MEJORA/UX] [ERP - POS] (`OBS-012`):** Advertencia de Confirmación al Abrir Turno sin Platos en el Catálogo.
  - *Prioridad:* `P3 - Baja`
  - *Origen:* [`OBS-012`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Mostrar un aviso/confirmación en POS al intentar abrir turno cuando el restaurante no tiene platos registrados (*"Vas a abrir tu turno sin ningún plato registrado. ¿Estás seguro?"*), permitiendo continuar si el usuario confirma.

---

## 🚀 Tareas MVP v1 (Estabilización POS, Imágenes y Catálogo Unificado)

### Prioridad Crítica (`P0`)

- [x] **TASK-628 [BUG/CRÍTICA] [AUTH - USUARIOS] (`OBS-013`):** Exigir Cambio Obligatorio de Contraseña (`mustChangePassword`) en Primer Login / Post-Reset desde Backoffice.
  - *Prioridad:* `P0 - Crítica`
  - *Origen:* [`OBS-013`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diagnosticar por qué al resetear la contraseña de un usuario del tenant desde Backoffice (`mustChangePassword = true`) el login ERP lo deja navegar directo a `/company` sin forzar el cambio de contraseña, y garantizar el bloqueo de navegación hasta actualizarla.

### Prioridad Alta (`P1` — Fixes Directos MVP)

- [x] **TASK-629 [BUG/INFRA] [STORAGE - CATÁLOGO/POS] (`OBS-015`):** Corregir Visualización de Imágenes (`/uploads/dishes/...`) en Todos los Ambientes y Documentar Requisitos de Storage.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-015`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Resolver la carga de imágenes en `/catalog` y `/pos` para orígenes separados (`http://app.localhost` vs `http://api.localhost`, `staging`, `prod` y S3/LOCAL) y documentar cualquier configuración manual requerida por ambiente para los superadmins.

- [x] **TASK-630 [UI/UX] [ERP - POS & CATÁLOGO] (`OBS-018`):** Pulido Final de POS (Comprobante) y Depuración de Jerga Técnica Orientada a Código.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-018`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Eliminar textos de implementación expuestos al usuario final (ej. *"Registrado en servidor"* en el comprobante POS, *"Receta BOM"* y equivalentes en Catálogo/ERP) sustituyéndolos por lenguaje claro de negocio.

- [x] **TASK-631 [MEJORA/FUNC] [ERP - CATÁLOGO] (`OBS-014`, `OBS-016`):** Edición/Eliminación de Categorías y Edición Completa de Platos en `/catalog`.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-014`](./observaciones_y_hallazgos.md), [`OBS-016`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Habilitar edición/eliminación segura de categorías y edición completa de platos existentes (nombre, descripción, categoría, precio, disponibilidad, etc.) en la vista de Catálogo.

### Prioridad Alta (`P1` — Co-Diseño Arquitectónico y de Producto MVP — Regla 8)

- [x] **TASK-632 [BUG/DISEÑO] [ERP - CATÁLOGO] (`OBS-017`):** Corrección Visual de *"Precios de mi Sucursal"* y Estudio/Simplificación del Flujo Multi-Sucursal (*"Ajustar Sucursal"* / *"Clonar"*).
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-017`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Corregir la ruptura visual de la tabla al desmarcar *"Precios de mi Sucursal"* y realizar sesión de co-diseño sobre parámetros/reglas actuales para simplificar *"Ajustar Sucursal"* y *"Clonar"* con un flujo por defecto claro para el usuario final.

- [x] **TASK-633 [ARQ/ESTUDIO] [BACKEND/BD] (`OBS-022`):** Estudio Arquitectónico de Modelo Flexible para Catálogo Dinámico (Relacional vs Híbrido `JSONB` en PostgreSQL).
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-022`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Evaluar pros, contras, rendimiento, indexación y retrocompatibilidad entre esquema relacional estricto vs híbrido `JSONB` en PostgreSQL (sin NoSQL externo) para soportar platos, complementos, combos y menús dinámicos.

- [x] **TASK-634 [FUNC/DISEÑO] [ERP - CATÁLOGO] (`OBS-019`, `OBS-020`, `OBS-021`):** Diseño e Implementación de Extras/Complementos por Plato, Gestor de Combos y Generalización de Menú del Día.
  - *Prioridad:* `P1 - Alta`
  - *Origen:* [`OBS-019`](./observaciones_y_hallazgos.md), [`OBS-020`](./observaciones_y_hallazgos.md), [`OBS-021`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Definir e implementar sobre la arquitectura acordada en `TASK-633`: (1) categorización/gestión de extras y complementos (vendibles o exclusivos de acompañamiento) asignables a platos, (2) creador/gestor de combos, y (3) generalización del Menú del Día según tipo de negocio.

### Prioridad Media (`P2` — Mínimo Coherente para MVP v1)

- [ ] **TASK-635 [UX/SANEAMIENTO] [ERP - INSUMOS/INVENTARIO] (`OBS-023`):** Saneamiento Mínimo de Alta y Edición de Insumos para MVP v1 (Sin Duplicidades ni Saltos Incoherentes).
  - *Prioridad:* `P2 - Media`
  - *Origen:* [`OBS-023`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Dejar funcional y coherente el registro mínimo de insumos (alta, modificación, eliminación de saltos confusos entre Catálogo e Inventario y clarificación de *"Stock inicial"*), sin abordar todavía Kardex ni producción completa.

---

## 📦 Post-MVP / Baja Prioridad (`P3`)

- [ ] **TASK-636 [ESTUDIO/FUNC] [ERP - INVENTARIO/KARDEX] (`OBS-023`):** Estudio Estándar de Inventario Gastronómico (Insumos Pre-elaborados, Ajustes por Costo Total de Lote y Kardex Completo).
  - *Prioridad:* `P3 - Baja` (fuera de MVP v1)
  - *Origen:* [`OBS-023`](./observaciones_y_hallazgos.md)
  - *Objetivo:* Diseñar e implementar un modelo estándar y retrocompatible de inventario para pequeños productores/restaurantes (separando insumos crudos vs pre-elaborados, cálculo de costo unitario desde valor total del lote y flujo formal de Kardex).
