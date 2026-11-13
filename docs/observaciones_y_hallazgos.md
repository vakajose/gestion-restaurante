---
title: "Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Inbox de Triage)"
category: "feedback"
status: "living-document"
last_updated: "2026-11-13"
tags:
  - observations
  - bugs
  - feedback
  - triage
  - backlog-inbox
---

# 📥 Bitácora Dinámica de Observaciones, Hallazgos y Mejoras

> **Propósito:** Inbox temporal de hallazgos durante pruebas manuales. Los ítems `OBS-001` a `OBS-010` ya fueron convertidos y depurados hacia [`docs/tasks.md`](./tasks.md) (`TASK-614`–`TASK-624`). Aquí permanecen únicamente las observaciones **pendientes de triage**.

---

## 📊 Resumen Rápido (Pendientes)

| ID | Fecha | Módulo / Pantalla | Tipo | Severidad / Prioridad | Título Breve | Estado |
|:---|:---:|:---|:---:|:---:|:---|:---:|
| *OBS-011* | 2026-11-13 | ERP Finanzas y Arqueo (`/finance`) | Bug | Crítica | *Internal Server Error (`could not determine data type of parameter 4`) al ingresar a Finanzas y Arqueo en restaurante vacío* | `PENDIENTE` |
| *OBS-012* | 2026-11-13 | ERP POS (`/pos`) | Mejora | Baja | *Mostrar advertencia/confirmación al abrir turno sin ningún plato registrado en el catálogo* | `PENDIENTE` |

---

## 📋 Plantilla para Nuevas Observaciones

```markdown
### [OBS-XXX] Título conciso del hallazgo o cambio solicitado
- **Fecha:** YYYY-MM-DD
- **Módulo / Ruta:** (ej. POS `/pos`, Finanzas `/finance`, Catálogo `/catalog/menus`)
- **Tipo:** `BUG` | `UI/UX` | `MEJORA` | `CAMBIO`
- **Severidad:** `CRÍTICA` | `ALTA` | `MEDIA` | `BAJA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual:**
  - ...
- **Comportamiento Esperado:**
  - ...
```

---

## 🗂️ Registro Detallado de Observaciones Pendientes

### [OBS-011] Internal Server Error en Finanzas y Arqueo (`could not determine data type of parameter 4`)
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** ERP Finanzas y Arqueo (`/finance`)
- **Tipo:** `BUG`
- **Severidad:** `CRÍTICA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual:**
  - Al ingresar a Finanzas y Arqueo en un restaurante recién creado (sin platos ni datos previos), el backend responde `500 Internal Server Error` con el error PostgreSQL/Hibernate `could not determine data type of parameter 4`.
- **Comportamiento Esperado:**
  - La consulta debe manejar parámetros opcionales/nulos con tipado explícito en PostgreSQL y devolver la lista vacía o los turnos correspondientes sin error 500.

---

### [OBS-012] Advertencia de confirmación al abrir turno sin platos en catálogo
- **Fecha:** 2026-11-13
- **Módulo / Ruta:** ERP POS (`/pos`)
- **Tipo:** `MEJORA`
- **Severidad:** `BAJA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual:**
  - Permite abrir turno en modo carta abierta sin platos registrados sin previo aviso.
- **Comportamiento Esperado:**
  - Mostrar una notificación/confirmación: *"Vas a abrir tu turno sin ningún plato registrado. ¿Estás seguro?"* y permitir continuar si el usuario confirma.
