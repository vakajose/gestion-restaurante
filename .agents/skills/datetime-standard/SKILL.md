---
name: datetime-standard
description: >-
  Estándar y buenas prácticas para el manejo de fechas, horas y zonas horarias en este proyecto.
  Usar siempre que se definan modelos de base de datos, DTOs, entidades JPA, servicios o componentes Angular con fechas.
---

# Estándar de Fechas y Zonas Horarias (ADR-0006)

Este skill define la forma obligatoria de manipular fechas y horas en todas las capas del sistema (`backend/`, `frontend/` y `backoffice-frontend/`).

## 1. Fechas de Calendario Puras (Date-only)
Aplica a: `expenses.expense_date`, `branch_service_sessions.service_date`, `tenant_daily_telemetry.stat_date`, `subscription_charges.due_date` y filtros contables/telemetría (`startDate`, `endDate`, `from`, `to`).
- **PostgreSQL:** `DATE`
- **Java:** `java.time.LocalDate`
- **JSON Serializado:** `"YYYY-MM-DD"` (ej. `"2026-11-17"`). **NUNCA incluir `'T00:00:00Z'`**.
- **Angular / React / TypeScript:** `string` (`'YYYY-MM-DD'`). Input HTML: `<input type="date">`.
- **Formato Visual UI:** `dd/MM/yyyy` (ej. `17/11/2026`).

## 2. Timestamps Transaccionales (Instantes Precisos)
Aplica a: `created_at`, `updated_at`, `orders.closed_at`, `orders.cancelled_at`, `cash_shifts.opened_at`, `cash_shifts.closed_at`, `kardex_movements.movement_date`, `platform_audit_logs.created_at`.
- **PostgreSQL:** `TIMESTAMP WITH TIME ZONE` (`TIMESTAMPTZ`), almacenado en UTC.
- **Java:** `java.time.Instant` (con `spring.jackson.time-zone: UTC` y `write-dates-as-timestamps: false`).
- **JSON Serializado:** ISO-8601 con sufijo `Z` (ej. `"2026-11-17T13:28:34.123Z"`).
- **Conversión a Hora Local de Sucursal:**
  - En backend: convertir explícitamente el instante usando `branches.timezone` (default `'America/La_Paz'`), tanto en consultas JPA/SQL como en `LocalDate.atStartOfDay(ZoneId)` / `ZonedDateTime`.
  - En frontend: formatear usando la zona horaria de la sucursal activa (`branches.timezone`).
  - **Formato Visual UI:** `dd/MM/yyyy HH:mm:ss` (o `dd/MM/yyyy HH:mm` según vista).
