---
title: "ADR-0023: Histórico Gerencial de Turnos, Auditoría Multicriterio, Drill-Down y Exportación Excel/CSV (PB-030)"
category: "adr"
tags:
  - adr
  - finance
  - audit
  - reporting
  - csv
  - timezone
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0023: Histórico Gerencial de Turnos, Auditoría Multicriterio, Drill-Down y Exportación Excel/CSV (PB-030)

- **Alcance:** Finanzas (Caja y Turnos), Auditoría Gerencial, Exportación CSV, Husos Horarios ([ADR-0006](./0006_datetime_and_timezone_handling.md)) — `PB-030` (`TASK-508`, `TASK-626`)

---

## 1. Contexto y Problemática

El módulo de Finanzas cuenta con la funcionalidad operativa de apertura, arqueo de cierre y consolidación de turnos de caja (`cash_shifts`). Sin embargo, para la supervisión y control de gestión administrativa de restaurantes se identificaron limitaciones críticas:
1. **Falta de Filtros Multicriterio en Base de Datos:** El endpoint anterior `GET /api/v1/finance/shifts` recuperaba listas no paginadas de turnos sin capacidad de filtrado por rango de fechas, cajero ni resultado de arqueo, trasladando una carga excesiva e ineficiente al navegador.
2. **Desajuste de Husos Horarios (ADR-0006):** Si un administrador consulta los turnos de "Hoy" o de una semana específica, el filtrado debe interpretarse estrictamente en la zona horaria oficial de la sucursal (`branches.timezone`, default `'America/La_Paz'`). Truncar por UTC provocaría que turnos nocturnos cerrados a las 01:00 AM aparezcan en el día equivocado.
3. **Ausencia de Inspección en Profundidad (Drill-Down):** El supervisor veía el total vendido en la fila del turno, pero no podía auditar cuáles fueron las ventas individuales, qué pedidos se cobraron con QR/tarjeta, qué gastos de caja menor se efectuaron o si existieron cancelaciones con devolución de dinero.
4. **Exportación de Datos sin Bloatware:** El área contable requiere descargar reportes para conciliar en Microsoft Excel o Google Sheets sin sobrecargar el frontend o backend con librerías pesadas como Apache POI (15 MB en backend) o `xlsx/SheetJS` (250 KB en bundle).

---

## 2. Decisiones Arquitectónicas

### A. Endpoint de Consulta Multicriterio de Turnos en Backend
Se amplía el servicio de finanzas con soporte multicriterio en `FinanceController` y `CashShiftService`:
- **Endpoint:** `GET /api/v1/finance/shifts/history`
- **Parámetros Query:**
  - `branchId` (UUID opcional): Si el usuario es `ADMIN_TENANT` o `SUPERADMIN`, puede filtrar por cualquier sucursal o consultar todas (`null`). Si es cajero o supervisor de sucursal, se fuerza su sucursal activa.
  - `startDate` (String `YYYY-MM-DD` opcional): Límite inferior de apertura.
  - `endDate` (String `YYYY-MM-DD` opcional): Límite superior de apertura.
  - `cashierId` (UUID opcional): Filtro por cajero responsable de la apertura.
  - `status` (String opcional: `'ALL'`, `'OPEN'`, `'CLOSED'`, `'CONSOLIDATED'`).
  - `discrepancyType` (String opcional: `'ALL'`, `'CUADRADO'`, `'SOBRANTE'`, `'FALTANTE'`).
  - `limit` (int, default `50`): Para control de rendimiento.

### B. Conversión Estricta de Zona Horaria por Sucursal (ADR-0006)
Cuando se especifican `startDate` y `endDate`:
1. El backend obtiene el `timezone` de la sucursal consultada desde `BranchRepository` (ej. `'America/La_Paz'`).
2. Transforma las fechas de calendario local en instantes UTC exactos:
   $$\text{startInstant} = \text{LocalDate}(\text{startDate}).\text{atStartOfDay}(\text{ZoneId}).\text{toInstant()}$$
   $$\text{endInstant} = \text{LocalDate}(\text{endDate}).\text{atTime}(23, 59, 59, 999999999).\text{atZone}(\text{ZoneId}).\text{toInstant()}$$
3. La consulta SQL compara: `s.opened_at >= :startInstant AND s.opened_at <= :endInstant`.

### C. Inspección en Profundidad (Modal Drill-Down)
- **Endpoint:** `GET /api/v1/finance/shifts/{id}/audit-detail`
- **Contrato DTO `ShiftAuditDetailDto`:**
  - Información de cabecera: `id`, `branchName`, `openedByName`, `closedByName`, `openedAt`, `closedAt`, `status`.
  - Resumen de Totales: `initialCash`, `totalCashSales`, `totalCardSales`, `totalQrSales`, `totalTransferSales`, `totalExpenses`, `totalCashRefunds`, `expectedCash`, `actualCash`, `difference`.
  - Desglose de Ventas: Lista de `OrderSummaryDto` (ticket, fecha local, cliente, medio de pago, total, estado `PAID` / `CANCELLED`).
  - Desglose de Gastos de Gaveta: Lista de `ExpenseDto` (concepto, monto, categoría, aprobado por).
  - Desglose de Cancelaciones y Devoluciones: Detalle de pedidos cancelados con motivo y vía de reintegro (`CASH` / `DIGITAL`).
- **Frontend Modal `<app-shift-detail-modal>`:** Componente presentacional puro con pestañas para alternar entre el resumen contable, la lista de órdenes y los gastos de caja chica.

### D. Exportador Nativo CSV / Excel-Ready (Regla 9/10 de Dependencias)
En estricto cumplimiento de la regla de gobernanza, se descarta la inclusión de dependencias externas pesadas. Se implementa una utilidad nativa en TypeScript (`exportToCsv`):
- **Peso:** **< 3 KB** (0 dependencias npm).
- **Compatibilidad con Microsoft Excel y Hojas de Cálculo:**
  - Prefijo Byte Order Mark UTF-8 (`\uFEFF`) obligatorio para que Excel interprete correctamente tildes, eñes y caracteres especiales sin requerir importación manual.
  - Delimitador estándar punto y coma (`;`) o coma (`,`) con escape seguro de cadenas que contienen saltos de línea o comillas.
- **Alcance de Exportación:**
  1. *Reporte Consolidado:* Exporta la tabla de turnos filtrada actualmente en pantalla (`reporte_turnos_YYYYMMDD.csv`).
  2. *Auditoría Individual:* Exporta todas las ventas y egresos del turno inspeccionado (`detalle_turno_XXXX.csv`).

---

## 3. Consecuencias y Beneficios

- **Auditoría Financiera Confiable:** Detección inmediata de faltantes o sobrantes de caja por cajero, fecha y sucursal.
- **Rendimiento Óptimo:** Las consultas filtradas se resuelven en la base de datos PostgreSQL en milisegundos sin sobrecargar el navegador.
- **Cero Bloatware:** Exportación completa a hojas de cálculo con menos de 3 KB de código nativo.
- **Rigor Horario:** Cumplimiento total del estándar [ADR-0006](./0006_datetime_and_timezone_handling.md) evitando desfasajes entre turnos nocturnos y fechas de calendario.

## Documentos Relacionados
- [ADR-0005: Workflow de Aprobación de Gastos de Caja Chica](./0005_cash_shift_expense_workflow.md)
- [ADR-0006: Manejo Estricto de Fechas y Zonas Horarias](./0006_datetime_and_timezone_handling.md)
- [ADR-0016: Separación de Control de Caja y Turnos Multi-Cajero](./0016_cash_control_separation_and_multi_cashier_shifts.md)
- [ADR-0019: Cancelaciones de Pedidos, Autorizaciones y Devoluciones](./0019_order_cancellation_workflow_authorizations_and_waste_treatment.md)
- [Casos de Uso de Finanzas](../project/architecture/03-use-cases/use_cases_finance.md)
- [Máquina de Estados de Turnos y Gastos](../project/architecture/04-behavior-and-flows/state_shift_and_expense_workflow.md)
