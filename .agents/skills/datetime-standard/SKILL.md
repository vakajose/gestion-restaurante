---
name: datetime-standard
description: >-
  Estándar y buenas prácticas para el manejo de fechas, horas y zonas horarias en este proyecto.
  Usar siempre que se definan modelos de base de datos, DTOs, entidades JPA, servicios o componentes Angular con fechas.
---

# Estándar de Fechas y Zonas Horarias (ADR-0006)

Este skill define la forma obligatoria de manipular fechas y horas en todas las capas del sistema.

## 1. Fechas de Calendario Puras (Date-only)
Aplica a: `expenses.expense_date`, fechas de facturas de compras, filtros contables (`startDate`, `endDate`).
- **PostgreSQL:** `DATE`
- **Java:** `java.time.LocalDate`
- **JSON Serializado:** `"YYYY-MM-DD"` (ej. `"2026-09-30"`). **NUNCA incluir 'T00:00:00Z'**.
- **Angular/TypeScript:** `string` (`'YYYY-MM-DD'`). Input HTML: `<input type="date">`.
- **Formato Visual UI:** `dd/MM/yyyy` (ej. `30/09/2026`).

## 2. Timestamps Transaccionales (Instantes Precisos)
Aplica a: `created_at`, `updated_at`, `orders.closed_at`, `cash_shifts.opened_at`, `kardex_movements.movement_date`, `audit_logs.created_at`.
- **PostgreSQL:** `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`), almacenado en UTC.
- **Java:** `java.time.Instant`.
- **JSON Serializado:** ISO-8601 con sufijo `Z` (ej. `"2026-09-30T13:28:34.123Z"`).
- **Conversión a Hora Local de Sucursal:**
  - En backend: las consultas de corte de día convierten el instante usando la zona de la sucursal:
    `WHERE (o.created_at AT TIME ZONE b.timezone)::date = :targetDate`
  - En frontend: formatear con `DatePipe` pasando el timezone de la sucursal activa (`branches.timezone`, default `'America/La_Paz'`).
  - **Formato Visual UI:** `dd/MM/yyyy HH:mm:ss`.
