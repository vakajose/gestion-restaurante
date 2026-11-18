---
title: "Manual de Control de Caja, Gastos Menores y Arqueo"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - caja
  - arqueo
  - gastos
  - anulaciones
  - turno
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 💰 Manual de Control de Caja, Gastos Menores y Arqueo

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Manual del Cajero POS](./03_manual_cajero_pos.md) | [Guía del Dueño: Finanzas y Personal](./06_dueno_finanzas_personal_y_reglas.md)

En **`Control de Caja`** administras la operación **en vivo** del turno actual: abrir caja, revisar ventas del turno, anular pedidos con autorización, registrar gastos menores de caja chica y realizar el arqueo de cierre. Para revisar cierres pasados o consolidar días anteriores, el dueño o encargado usa **`Finanzas y Arqueos`**.

---

## 1. Abrir Turno de Caja

Antes de cobrar en el POS debes abrir turno en **`Control de Caja`**:

1. Ingresa el **Fondo Inicial de Efectivo (Bs.)** (el sencillo/cambio con el que arranca el cajón).
2. Según cómo esté configurado el restaurante en `Parámetros del Sistema`:
   - **Una sola caja por sucursal vs Caja individual por cajero:** Si trabajan con caja compartida, hay un único turno abierto por sucursal; si trabajan con caja individual, cada cajero abre y rinde su propio turno independiente (y el encargado puede elegir en pantalla qué turno activo supervisar).
   - **Quién puede abrir:** Puede estar habilitado para todos los cajeros autorizados o restringido únicamente a supervisores.
   - **Jornada de servicio:** Si la apertura automática con `Toda la Carta` está activa, tu primer turno habilita de inmediato la venta en el POS sin pasos extra.

---

## 2. Pestaña `Comandas y Ventas del Turno` y Anulación de Pedidos

Aquí ves en tiempo real todos los pedidos emitidos durante el turno seleccionado, sus montos, modalidad y medio de pago.

### Cómo Anular un Pedido Paso a Paso
Si un cliente cancela a tiempo o hubo un error de cobro en un pedido pagado del turno:

1. Ubica el pedido en **`Comandas y Ventas del Turno`** y pulsa la opción para **Anular**.
2. Completa los datos requeridos:
   - **Motivo de Anulación:** explica por qué se cancela el pedido.
   - **Devolución de Dinero:** indica si devolviste el importe al cliente en **`Efectivo`** *(se restará automáticamente del efectivo esperado en el cajón)* o por medio **`Digital / Transferencia`**.
   - **Estado en Cocina (si aplica inventario):** indica si el plato **aún no se preparó** (se devuelven los insumos al Kardex) o si **ya fue preparado** (dependiendo de la regla de tu sucursal, se registrará como merma definitiva en inventario o pasará a reserva reutilizable).
3. **Autorización Obligatoria del Supervisor:** Toda anulación exige respaldo de un supervisor mediante una de estas dos vías:
   - **Vía Presencial (PIN):** el supervisor ingresa en el teclado en pantalla su **PIN numérico de 4 a 6 dígitos** (configurado previamente en su pantalla `Mi Perfil`). La anulación se ejecuta de inmediato.
   - **Vía Remota (`Enviar a Cola Remota`):** si el encargado no está junto a la caja, el cajero envía la solicitud. El pedido permanece activo hasta que el supervisor entre a la pestaña **`Autorizaciones de Turno`** desde su teléfono o computadora y apruebe o rechace la anulación.

---

## 3. Pestaña `Gastos de Caja Menor`

Sirve para registrar compras urgentes o salidas menores del día (hielo, bolsas, gas, taxis, propinas operativas) sin perder el cuadre:

1. Registra un nuevo gasto indicando **Descripción**, **Categoría**, **Monto (Bs.)** y si se pagó con **efectivo del cajón (Caja Chica)** o **pago externo**.
2. **Comprobante (`URL / Comprobante de Evidencia (Opcional)`):** Puedes escribir un número de factura, recibo o enlace web del comprobante. *(Nota: hoy este campo acepta texto o URL; la captura directa de foto desde cámara no está incluida en el formulario de gastos actual).*
3. **Tope y Control Dual:**
   - Tu sucursal define un monto máximo por gasto de caja chica (por defecto **Bs. 100,00**).
   - Según la regla de auto-aprobación: cuando un cajero registra un gasto, normalmente queda **`Pendiente`** para que un supervisor lo apruebe en **`Autorizaciones de Turno`** (evitando salidas sin respaldo); cuando el **Administrador de Empresa** registra un gasto, queda aprobado automáticamente.
   - Únicamente los gastos **aprobados** pagados con efectivo de caja descuentan del efectivo esperado al cerrar.

---

## 4. Pestaña `Autorizaciones de Turno`

Es la bandeja del encargado o supervisor durante la jornada. Desde aquí aprueba o rechaza con un toque:
- **Gastos menores pendientes** registrados por los cajeros.
- **Solicitudes remotas de anulación de pedidos** enviadas con `Enviar a Cola Remota` (al aprobar una anulación remota, el sistema ejecuta de inmediato la cancelación, ajusta el inventario y descuenta la devolución en efectivo si corresponde).

---

## 5. Cerrar Turno y Arqueo de Caja

Al terminar el turno o la jornada:

1. Revisa que no queden autorizaciones pendientes que afecten tu cajón y que no tengas ventas sin conexión pendientes de subir.
2. Pulsa para iniciar el **Cierre / Arqueo de Turno**.
3. Cuenta físicamente todos los billetes y monedas dentro del cajón.
4. En el formulario verás la referencia del **Efectivo Esperado por el Sistema**, calculado con precisión contable:

$$\text{Efectivo Esperado} = \text{Fondo Inicial} + \text{Ventas en Efectivo} - \text{Gastos en Efectivo Aprobados} - \text{Devoluciones en Efectivo}$$

5. Verifica y corrige el campo **`Efectivo Físico Contado`** con el monto real que contaste en mano *(ten en cuenta que el sistema precarga el valor esperado como guía numérica inicial: reemplázalo siempre por tu conteo físico real)* y añade tus observaciones si existe **Sobrante** o **Faltante**.
6. Confirma el cierre. El turno pasa a estado cerrado y queda disponible para auditoría, exportación CSV y consolidación en **`Finanzas y Arqueos`**.

---

## Relacionado

- [Manual del Cajero: Punto de Venta (POS)](./03_manual_cajero_pos.md)
- [Guía del Dueño: Finanzas, Personal y Reglas](./06_dueno_finanzas_personal_y_reglas.md)
- [Insumos, Recetas, Conteos y Kardex](./05_insumos_recetas_y_kardex.md)
- [Flujo de Jornada y Arqueo de Caja (Procesos de Negocio)](../../business-processes/02-caja-y-finanzas/01_flujo_jornada_y_arqueo_caja.md)
- [Flujo de Gastos y Autorizaciones (Procesos de Negocio)](../../business-processes/02-caja-y-finanzas/02_flujo_gastos_y_autorizaciones.md)
