---
title: "Bóveda de Sprints: Histórico, Planificación y Seguimiento"
category: "sprints"
tags:
  - sprints
  - scrum
  - roadmap
  - agile
  - execution-plan
last_updated: "2026-10-28"
status: "verified"
---

# 🏃 Bóveda de Sprints: Histórico, Planificación y Seguimiento

Esta carpeta contiene el registro desacoplado de todos los ciclos de desarrollo (Sprints) del proyecto, permitiendo a los desarrolladores y agentes de IA acceder de forma rápida y sin consumo excesivo de tokens al sprint de su interés.

---

## 🗺️ Mapa General de Sprints

| Sprint | Estado | Enfoque Operativo | Archivo de Seguimiento |
| :---: | :---: | :--- | :--- |
| **Sprint 1** | `CERRADO` | Hito 1: Arquitectura Base, Infraestructura, Persistencia y SDD Fundacional | [**`sprint_01_backlog.md`**](./sprint_01_backlog.md) |
| **Sprint 2** | `CERRADO` | Aislamiento Multi-Tenant, Promociones, Combos, Desacoplamiento y Suite E2E | [**`sprint_02_backlog.md`**](./sprint_02_backlog.md) |
| **Sprint 3** | `ACTIVO` | **Sprint en Ejecución:** Parametría Multinivel, Onboarding, RBAC Dinámico, Sesiones y Móvil | [**`docs/tasks.md`**](../../tasks.md) |
| **Sprint 4** | `PLANIFICADO` | Flujo Operativo: Ventas del Turno, Turnos Históricos, Cancelaciones y Delivery | [**`sprint_04_planning.md`**](./sprint_04_planning.md) |
| **Sprint 5** | `PLANIFICADO` | Medios Desacoplados (S3/WebP), Catálogo Visual, Exclusiones BOM y Almacén Central | [**`sprint_05_planning.md`**](./sprint_05_planning.md) |
| **DevOps** | `BACKLOG` | Infraestructura Cloud, Dockerfiles, Certificación Staging, SSL y CI/CD | [**`devops_infra_backlog.md`**](./devops_infra_backlog.md) |

---

## 📌 Relación con el Product Backlog

- Cada tarea de los sprints se deriva directamente de un requerimiento priorizado en el [**Product Backlog**](../product_backlog/README.md).
- El archivo raíz [**`docs/tasks.md`**](../../tasks.md) representa **exclusivamente el Sprint Backlog del Sprint Activo (Sprint 3)**. Al cerrarse un sprint, sus tareas se congelan en su archivo histórico correspondiente y `docs/tasks.md` se actualiza con el siguiente sprint planificado.
