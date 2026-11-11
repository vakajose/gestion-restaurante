---
title: "Bóveda de Sprints: Histórico, Planificación y Seguimiento"
category: "sprints"
tags:
  - sprints
  - scrum
  - roadmap
  - agile
  - execution-plan
last_updated: "2026-11-11"
status: "verified"
---

# 🏃 Bóveda de Sprints: Histórico, Planificación y Seguimiento

Esta carpeta contiene el registro desacoplado de todos los ciclos de desarrollo (Sprints) del proyecto, permitiendo a los desarrolladores y agentes de IA acceder de forma rápida y sin consumo excesivo de tokens al sprint de su interés.

---

## 🗺️ Mapa General de Sprints y Roadmap

| Sprint | Estado | Enfoque Operativo | Entregable / Hito | Archivo de Seguimiento |
| :---: | :---: | :--- | :--- | :--- |
| **Sprint 1** | `CERRADO` | Arquitectura Base, Infraestructura, Persistencia y SDD Fundacional | Monolito modular, POS offline Dexie y Kardex | [**`sprint_01_backlog.md`**](./sprint_01_backlog.md) |
| **Sprint 2** | `CERRADO` | Aislamiento Multi-Tenant, Promociones, Combos, Desacoplamiento y Suite E2E | Multi-tenant estricto y turnos de caja | [**`sprint_02_backlog.md`**](./sprint_02_backlog.md) |
| **Sprint 3** | `CERRADO` | Parametría Multinivel, Onboarding, RBAC Dinámico, Sesiones y Móvil | Release `v0.3.0` certificado | [**`sprint_03_backlog.md`**](./sprint_03_backlog.md) |
| **Sprint 4** | `CERRADO` | Fundamentos, Blindaje de Caja, Design System y Menús Diarios | Caja blindada, UI estandarizada y menús | [**`sprint_04_backlog.md`**](./sprint_04_backlog.md) |
| **Sprint 5** | `CERRADO` | Servicio Avanzado: Extras, Modificadores, Cancelaciones, Delivery, Catálogo Visual, Auditoría y Humanización | **Release Candidato a Producción (`v0.5.0`)** | [**`sprint_05_backlog.md`**](./sprint_05_backlog.md) |
| **Sprint 6** | `ACTIVO` | **Sprint Activo:** Infraestructura Cloud OCI, Storage S3/R2, DevSecOps, Backoffice SaaS en React y CI/CD | **Staging Cloud OCI + Backoffice React LTS** | [**`docs/tasks.md`**](../../tasks.md) |

---

## 📌 Relación con el Product Backlog

- Cada tarea de los sprints se deriva directamente de un requerimiento priorizado en el [**Product Backlog**](../product_backlog/README.md).
- El archivo raíz [**`docs/tasks.md`**](../../tasks.md) representa **exclusivamente el Sprint Backlog del Sprint Activo**. Al cerrarse un sprint, sus tareas se congelan en su archivo histórico correspondiente y `docs/tasks.md` se actualiza con el siguiente sprint planificado una vez acordado.
