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
