---
title: "Bóveda de Sprints: Histórico, Planificación y Seguimiento"
category: "sprints"
tags:
  - sprints
  - scrum
  - roadmap
  - agile
  - execution-plan
last_updated: "2026-11-17"
status: "active"
---

# 🏃 Bóveda de Sprints: Histórico, Planificación y Seguimiento

Esta carpeta contiene el registro desacoplado de todos los ciclos de desarrollo (Sprints) del proyecto, permitiendo a los desarrolladores y agentes de IA acceder de forma rápida y sin consumo excesivo de tokens al sprint de su interés.

---

## 🗺️ Mapa General de Sprints y Roadmap

| Sprint | Estado | Enfoque Operativo | Entregable / Hito | Archivo de Seguimiento |
| :---: | :---: | :--- | :--- | :--- |
| **Sprint 1** | `COMPLETADO` | Arquitectura Base, Infraestructura, Persistencia y SDD Fundacional | Monolito modular, POS offline Dexie y Kardex | [**`sprint_01_backlog.md`**](./sprint_01_backlog.md) |
| **Sprint 2** | `COMPLETADO` | Aislamiento Multi-Tenant, Promociones, Combos, Desacoplamiento y Suite E2E | Multi-tenant estricto y turnos de caja | [**`sprint_02_backlog.md`**](./sprint_02_backlog.md) |
| **Sprint 3** | `COMPLETADO` | Parametría Multinivel, Onboarding, RBAC Dinámico, Sesiones y Móvil | Release `v0.3.0` certificado | [**`sprint_03_backlog.md`**](./sprint_03_backlog.md) |
| **Sprint 4** | `COMPLETADO` | Fundamentos, Blindaje de Caja, Design System y Menús Diarios | Caja blindada, UI estandarizada y menús ([Planning archivado](./sprint_04_planning.md)) | [**`sprint_04_backlog.md`**](./sprint_04_backlog.md) |
| **Sprint 5** | `COMPLETADO` | Servicio Avanzado: Extras, Modificadores, Cancelaciones, Delivery, Catálogo Visual, Auditoría y Humanización | **Release `v0.5.0`** ([Planning archivado](./sprint_05_planning.md)) | [**`sprint_05_backlog.md`**](./sprint_05_backlog.md) |
| **Sprint 6** | `EN CURSO (release/sprint-6)` | Cloud OCI, Storage S3/R2, DevSecOps, Backoffice SaaS en React, GitFlow 4 Ambientes y Estabilización MVP v1 (`TASK-601`–`TASK-635`) | **Backoffice React LTS + MVP v1 Estabilizado** ([Planning](./sprint_06_planning.md)) | [**`sprint_06_backlog.md`**](./sprint_06_backlog.md) / [**`docs/tasks.md`**](../../tasks.md) |

---

## 📌 Relación con el Product Backlog y Notas Históricas

- Cada tarea de los sprints se deriva directamente de un requerimiento priorizado en el [**Product Backlog**](../product_backlog/README.md).
- El archivo raíz [**`docs/tasks.md`**](../../tasks.md) concentra **exclusivamente las tareas activas y abiertas** (`TASK-609` OCI en paralelo y backlog futuro `TASK-636`–`TASK-640`), mientras las tareas ya completadas del Sprint 6 (incluida `TASK-613`) se consolidan en [**`sprint_06_backlog.md`**](./sprint_06_backlog.md).
- El antiguo backlog transversal de infraestructura DevOps (`TASK-301`–`TASK-307`) fue absorbido y consolidado íntegramente dentro del Sprint 6 ([`sprint_06_backlog.md`](./sprint_06_backlog.md) y `TASK-609` en [`docs/tasks.md`](../../tasks.md)).
