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

- [ ] **TASK-611 [DOCS/MANUAL] [ONBOARDING-OPS]:** Manual de Bootstrap desde Cero (`testing-local` / Go-Live), Primeras Configuraciones en Backoffice y Actualización del `README.md` Principal.
  - *Prioridad:* `P0 - Crítica` (prerequisito antes de iniciar pruebas manuales).
  - *Objetivo:*
    1. Actualizar el [`README.md`](../README.md) principal del repositorio para reflejar la arquitectura actual de 3 aplicaciones (`backend/`, `frontend/` ERP Angular y `backoffice-frontend/` React), los 4 ambientes (`dev`, `testing-local`, `staging`, `prod`) y los accesos de cada entorno.
    2. Redactar un manual claro de **arranque desde cero y configuración inicial** que cubra: variables `.env` por ambiente/contenedor, secuencia de bootstrap automático (PostgreSQL + Flyway + Superadmin), y checklist de primeras configuraciones obligatorias en Backoffice (`/platform/settings`, prueba de Storage, revisión de `/plans`, gestión de `/platform/admins` y verificación post-onboarding en `/tenants`).

---

### 2. Depuración Pre-Bootstrap Detectada en ERP Frontend

- [ ] **TASK-612 [BUG/ERP FE] [SEGURIDAD/UX]:** Ocultar Accesos Rápidos Demo y Prellenado `demo-restaurant` en Login ERP fuera de `dev`.
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
