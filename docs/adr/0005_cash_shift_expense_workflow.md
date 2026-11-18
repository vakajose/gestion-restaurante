---
title: "ADR 0005: Workflow de Aprobación de Gastos de Caja Chica"
category: "adr"
tags:
  - adr
  - architecture
  - finance
  - cash-shifts
  - expenses
last_updated: "2026-11-17"
status: "accepted"
---

# ADR 0005: Workflow de Aprobación de Gastos de Caja Chica


## Contexto
Durante el turno de atención en un restaurante, es frecuente que el cajero deba realizar pagos menores inmediatos en efectivo desde la gaveta de dinero (caja chica), tales como compra de hielo de emergencia, insumos menores del mercado o propinas de delivery.

Si el sistema descuenta automáticamente cualquier valor declarado por el cajero sin comprobación, se vulnera la seguridad del efectivo. Si no permite registrarlo, el arqueo de caja físico al final del turno arrojará faltantes inexactos.

## Decisión
Se establece un flujo formal de solicitud y autorización de egresos de caja chica:
1. El cajero registra la salida en `expenses` indicando `paid_from_cash_drawer = true`, vinculando el turno actual (`cash_shift_id`), adjuntando la fotografía o comprobante digital (`evidence_url`) y una descripción justificativa.
2. La solicitud queda en estado `approval_status = 'PENDING_APPROVAL'`.
3. El encargado de sucursal (`BRANCH_MANAGER`) revisa la evidencia física/digital y aprueba o rechaza la solicitud.
4. **Fórmula de Arqueo:** Únicamente los gastos con `approval_status = 'APPROVED'` y pagados en efectivo se descuentan del cálculo de `expected_cash`:
   $$\text{expected\_cash} = \text{initial\_cash} + \text{total\_cash\_sales} - \text{total\_cash\_expenses}_{\text{approved}}$$
5. Si existen gastos pendientes al momento del cierre, el sistema alerta al usuario para que el encargado resuelva las aprobaciones antes del cierre definitivo del turno.

## Consecuencias
- **Positivas:** Control financiero estricto y trazable; cuadres de caja exactos sin discrepancias artificiales; prevención de fraudes y pérdidas.
- **Negativas:** Exige la intervención del encargado de sucursal para validar egresos antes del cierre del turno.

## Enmienda y Evolución (ADR-0013 y ADR-0019)
> [!NOTE]
> Regla ampliada y refinada por [ADR-0013](./0013_pos_shift_invariant_and_expense_authorizations.md), [ADR-0016](./0016_cash_control_separation_and_multi_cashier_shifts.md) y [ADR-0019](./0019_order_cancellation_workflow_authorizations_and_waste_treatment.md):
> - La aprobación de egresos abandona el rol fijo `BRANCH_MANAGER` y se gobierna por el permiso RBAC **`finance:expenses_approve`**.
> - Se incorpora el techo de gaveta `finance.max_petty_cash_expense` (default `100.00`), la política `finance.expense_allow_self_approval` (autoaprobación siempre activa para `ADMIN_TENANT`/`SUPERADMIN`) y la bandeja unificada `shift_authorizations`.
> - La fórmula canónica de arqueo evoluciona para descontar también las devoluciones en efectivo (`REFUND`) por cancelación de órdenes:
>   $$\text{expected\_cash} = \text{initial\_cash} + \text{total\_cash\_sales} - \text{total\_cash\_expenses}_{\text{approved}} - \text{total\_cash\_refunds}$$

---

## Documentos Relacionados
- [ADR-0013: Invariante Transaccional de Turno en POS y Gobernanza de Gastos](./0013_pos_shift_invariant_and_expense_authorizations.md)
- [ADR-0016: Separación de Control de Caja y Turnos Multi-Cajero](./0016_cash_control_separation_and_multi_cashier_shifts.md)
- [ADR-0019: Flujo de Cancelación de Pedidos, Autorizaciones y PIN](./0019_order_cancellation_workflow_authorizations_and_waste_treatment.md)
- [ADR-0023: Histórico de Turnos, Auditoría Multicriterio y Exportación CSV](./0023_shift_history_multicriteria_audit_and_csv_export.md)
- [Máquina de Estados de Turnos y Gastos](../project/architecture/04-behavior-and-flows/state_shift_and_expense_workflow.md)
- [Casos de Uso de Finanzas](../project/architecture/03-use-cases/use_cases_finance.md)

