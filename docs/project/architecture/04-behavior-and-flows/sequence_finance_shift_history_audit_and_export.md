---
title: "Diagrama de Secuencia: Auditoría Multicriterio de Turnos, Drill-Down y Exportación CSV"
category: "architecture"
level: "Sequence"
module: "finance"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - finance
  - audit
  - csv
  - sprint-5
last_updated: "2026-11-08"
status: "verified"
---

# 📊 Diagrama de Secuencia: Auditoría Multicriterio de Turnos, Drill-Down y Exportación CSV (PB-030 / TASK-508)

Este documento modela las interacciones entre la interfaz de gestión gerencial de Finanzas, el servicio de conversión de zonas horarias de sucursal (ADR-0006), la consulta multicriterio en base de datos y la exportación de reportes compatibles con hojas de cálculo (Excel/CSV).

---

## 1. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Gerente / Auditor
    participant UI as FinanceComponent (/finance)
    participant Modal as app-shift-detail-modal
    participant ExportUtil as exportToCsv Utility (Frontend)
    participant Api as FinanceController (Backend)
    participant Svc as CashShiftService
    participant BranchRepo as BranchRepository
    participant ShiftRepo as CashShiftRepository
    participant OrderRepo as OrderRepository
    participant DB as PostgreSQL

    %% Flujo 1: Filtrado Multicriterio con Huso Horario de Sucursal
    Note over Admin, DB: Flujo 1: Búsqueda Multicriterio y Conversión de Zona Horaria
    Admin->>UI: Selecciona Rango ("2026-11-01" a "2026-11-08") y Filtro de Arqueo ("FALTANTE")
    UI->>Api: GET /api/v1/finance/shifts/history?startDate=2026-11-01&endDate=2026-11-08&discrepancyType=FALTANTE
    Api->>Svc: getShiftsHistory(filterParams)
    Svc->>BranchRepo: Obtiene timezone de sucursal ('America/La_Paz')
    Svc->>Svc: Convierte 2026-11-01T00:00:00 (La Paz) a Instant UTC
    Svc->>Svc: Convierte 2026-11-08T23:59:59 (La Paz) a Instant UTC
    Svc->>ShiftRepo: findShiftsWithFilters(tenantId, branchId, startUtc, endUtc, discrepancyType)
    ShiftRepo->>DB: Consulta indexada con WHERE opened_at BETWEEN ... AND difference < 0
    DB-->>ShiftRepo: Retorna turnos coincidentes
    ShiftRepo-->>Svc: Lista de entidades CashShift
    Svc-->>Api: List<CashShiftDto>
    Api-->>UI: 200 OK (Turnos filtrados)
    UI->>Admin: Despliega tabla con métricas y alertas de faltante

    %% Flujo 2: Inspección en Profundidad (Drill-Down)
    Note over Admin, DB: Flujo 2: Drill-Down de Detalle de Turno
    Admin->>UI: Clic en "Ver Detalle" [👁️] del turno #1042
    UI->>Api: GET /api/v1/finance/shifts/{id}/audit-detail
    Api->>Svc: getShiftAuditDetail(shiftId)
    Svc->>ShiftRepo: Obtiene CashShift
    Svc->>OrderRepo: Obtiene órdenes pagadas y canceladas asociadas al cash_shift_id
    Svc->>DB: SELECT * FROM expenses WHERE cash_shift_id = :id AND approval_status = 'APPROVED'
    DB-->>Svc: Egresos y cancelaciones recuperados
    Svc-->>Api: ShiftAuditDetailDto (Ventas por medio de pago, órdenes, gastos)
    Api-->>UI: 200 OK (Detalle completo)
    UI->>Modal: Abre modal interactivo con pestañas de Auditoría
    Modal->>Admin: Muestra desglose exhaustivo de ventas y gastos

    %% Flujo 3: Exportación CSV Nativa compatible con Excel
    Note over Admin, ExportUtil: Flujo 3: Descarga de Reporte CSV sin Librerías Externas
    Admin->>UI: Clic en "Exportar a Excel (CSV)"
    UI->>ExportUtil: exportToCsv("reporte_turnos_20261108.csv", headers, dataRows)
    ExportUtil->>ExportUtil: Agrega prefijo BOM UTF-8 (\uFEFF) y formatea celdas con punto y coma
    ExportUtil->>Admin: Dispara descarga en navegador (abrible directamente en Excel)
```

---

## 2. Invariantes del Flujo

1. **Alineación Estricta de Calendario:** La consulta de fechas nunca opera en UTC directo; los instantes de inicio y fin se calculan transformando el día civil local (`00:00:00` a `23:59:59`) según `branches.timezone` (ADR-0006).
2. **Determinismo Contable:** En el detalle de auditoría, la suma de las órdenes cobradas por método de pago debe cuadrar exactamente con los totales registrados en la cabecera del turno:
   $$\text{Ventas Totales} = \text{Ventas Efectivo} + \text{Ventas Tarjeta} + \text{Ventas QR} + \text{Ventas Transferencia}$$
3. **Exportación Ligera y Segura:** Cero librerías externas en el bundle; el archivo generado incluye `\uFEFF` para compatibilidad nativa e inmediata con Microsoft Excel.
