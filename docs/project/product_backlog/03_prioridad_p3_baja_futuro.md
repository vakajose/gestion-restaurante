---
title: "Product Backlog: Prioridad P3 (Baja Prioridad / Futuro)"
category: "product-backlog"
priority: "P3"
tags:
  - backlog
  - p3
  - low-priority
  - future
  - salon
  - kds
last_updated: "2026-10-28"
status: "verified"
---

# 🟢 Product Backlog — Prioridad P3 (Baja Prioridad / Futuro)

> [!NOTE]
> **Definición de Prioridad P3:** Funcionalidades avanzadas, módulos complementarios y extensiones futuras que se abordarán en etapas maduras del producto o ante demanda específica de mercado.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P2](./02_prioridad_p2_media.md)

---

## Índice de Ítems P3

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-009** | Mapa Visual Interactivo de Distribución de Mesas | UX Salón / POS | En Backlog | Por Planificar |
| **PB-010** | Panel KDS Táctil con Métricas de Despacho en Cocina | Operaciones / Cocina | En Backlog | Por Planificar |
| **PB-038** | Módulo de Gestión de Reservas, Capacidad de Salón y Asignación | Salón & Atención | En Backlog | Por Planificar |

---

## Detalle Exhaustivo de Requerimientos P3

### PB-009 [SALON] [UI/UX]: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Editor interactivo drag & drop para diagramar salones y terrazas, con visualización gráfica del estado de mesas en tiempo real.

---

### PB-010 [POS] [UI/UX]: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Pantalla táctil interactiva para cocineros con cronómetros por comanda y métricas de tiempos de despacho.

---

### PB-038 [SALON] [NEGOCIO]: Módulo de Gestión de Reservas, Capacidad de Salón y Asignación de Mesas y Sillas
- **Categoría:** Salón & Atención / Experiencia de Cliente
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Futuras Iteraciones)`
- **Contexto:** Gestión de reservas previas por parte de clientes para asegurar disponibilidad en fechas y horarios pico, con control de capacidad física de comensales.
- **Alcance Funcional y Técnico:**
  - Agenda de reservas por fecha, turno y sucursal.
  - Registro de datos del comensal: nombre, teléfono, cantidad de personas, requerimientos especiales.
  - Asignación de mesas numeradas y sillas específicas para grupos grandes.
  - Estados de reserva: `PENDIENTE`, `CONFIRMADA`, `CLIENTE_SENTADO`, `CANCELADA`, `NO_SHOW` (no asistió).
- **Criterios de Aceptación:**
  - La asignación de reservas no permite sobrepasar la capacidad física máxima declarada para el salón en la franja horaria seleccionada.
