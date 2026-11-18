---
title: "Manual de Insumos, Recetas, Conteos y Kardex"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - inventario
  - kardex
  - insumos
  - recetas
  - mermas
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 📦 Manual de Insumos, Recetas, Conteos y Kardex

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Catálogo, Combos y Fotos](./01_catalogo_platos_combos_y_fotos.md) | [Control de Caja, Gastos y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)

Esta guía explica el ciclo completo de control de almacén y costos de cocina entre dos pantallas complementarias: **`Catálogo y Recetas`** (donde se definen los insumos y cuánto consume cada plato) e **`Inventario y Kardex`** (donde se cargan las existencias reales, conteos y auditorías de movimientos).

---

## 1. Paso 1: Crear Insumos y Armar Recetas (`Catálogo y Recetas`)

Los insumos de cocina se crean y editan dentro de **`Catálogo y Recetas`** → pestaña **`Insumos y Recetas`**:

1. **Crear un Insumo:** Define el **Nombre** (ej. *Carne Molida*, *Queso Cheddar*, *Aceite*), la **Unidad de Medida** (`kg`, `g`, `L`, `ml`, `unidad`, etc.) y el **Stock Mínimo de Alerta** (cantidad bajo la cual el sistema te avisará con alerta visual).
2. **Asignar Receta a Cada Plato:** Indica la cantidad exacta de cada insumo que consume **una porción vendida** del plato. Con esa receta y el costo unitario del insumo, el catálogo calcula automáticamente el **costo de producción y margen estimado**.
3. **Vincular Insumo a Extras (Opcional):** En la pestaña de **`Grupos de Extras`**, si ofreces agregados con materia prima relevante (ej. *"Doble Carne"*, *"Porción extra de queso"*), puedes vincular cada opción a un insumo y cantidad para que también descuente del almacén al venderse.

---

## 2. Paso 2: Cargar Existencias Iniciales (`Inventario y Kardex`)

Una vez creados los insumos, ingresa a **`Inventario y Kardex`** → **`Existencias Actuales`**:

1. Para cualquier insumo que todavía nunca haya tenido saldo cargado, pulsa **`Alta Stock Inicial`** / **`Cargar Stock Inicial`**.
2. **Carga única por insumo:** El stock inicial se registra **una sola vez** por ingrediente para establecer su punto de partida y costo base.
3. **Calculadora Bidireccional de Costos (`Valor Total Pagado <-> Costo Unitario`):**
   - Si sabes cuánto pagaste por todo el lote (ej. `50 kg` de harina por `Bs. 280,00`), escribe la cantidad y el **`Valor Total Pagado`**: el sistema calcula automáticamente el **`Costo Unitario`** (`Bs. 5,60 / kg`).
   - Si prefieres ingresar directamente el **`Costo Unitario`**, calcula automáticamente el valor total invertido.

---

## 3. Paso 3: Ajustes por Conteo Físico y Mermas

En **`Inventario y Kardex`** → botón **`Ajustar / Conteo`** mantienes alineado el sistema con lo que realmente hay en estantería y refrigeradores:
- **Conteo Físico (Ingreso o Salida de ajuste):** Cuando realizas un inventario semanal, o cuando repones mercadería nueva en esta versión actual, registras la diferencia o la cantidad real contada con una nota justificativa.
- **Registro de Merma (`Merma / Desperdicio`):** Cuando un insumo se echa a perder, se derrama o se quema en cocina, regístralo como ajuste de merma para que quede descontado del stock y trazado en costos sin confundirlo con ventas.

### Vista Rápida y Alertas en Celular o Tablet
- En pantallas pequeñas o móviles, cada insumo se muestra como una **tarjeta desplegable tipo acordeón** con borde de semáforo según su nivel frente al mínimo (stock saludable, stock bajo o stock negativo). Toca la tarjeta para desplegar detalle, costo y botones rápidos de ajuste.

---

## 4. Cómo Leer el `Libro Mayor de Kardex` y Reglas de Descuento

En **`Inventario y Kardex`** → pestaña **`Libro Mayor de Kardex`** ves el historial cronológico auditable de cada entrada y salida, mostrado siempre en la **hora local de tu sucursal**. Los tipos de movimiento reales que genera la operación son:

| Movimiento visible | Cuándo se registra |
| :--- | :--- |
| **Stock Inicial** | Al cargar por primera vez las existencias iniciales de un insumo. |
| **Salida por Venta** | Al cobrar en el POS platos o combos cuyas recetas incluyen el insumo. |
| **Consumo por Extra** | Al cobrar un extra/complemento que tenía insumo vinculado. |
| **Ajuste por Conteo Físico** | Al corregir manualmente diferencias tras contar almacén. |
| **Ajuste por Merma** | Por desperdicio registrado en inventario o por anular un pedido cuyo plato ya estaba cocinado (cuando la sucursal registra merma estricta). |
| **Reversión por Anulación** | Cuando se anula un pedido cuyo plato todavía no se había preparado en cocina y los insumos regresan al almacén. |

### Modos de Descuento al Vender (configurables en `Parámetros del Sistema`)
1. **Automático Permisivo *(predeterminado)*:** Descuenta recetas al cobrar en POS; según la regla de stock negativo, puede avisar o permitir continuar sin frenar la caja.
2. **Automático Estricto:** Impide completar la venta en POS si algún insumo de la receta no alcanza para cubrir las porciones pedidas.
3. **Manual Aislado:** Desactiva el descuento automático al vender; el control se lleva por conteos físicos periódicos.

---

## 5. Funciones Previstas para Futuras Versiones

Para evitar confusiones en el uso diario de almacén:
- **Compras a Proveedores (Facturas de Compra) y Traspasos entre Sucursales:** Aunque en algunas listas de referencia veas mencionados estos conceptos, **todavía no están habilitados como pantallas operativas completas** en la versión actual. Hoy en día, toda reposición manual de existencias posteriores al stock inicial se registra mediante **`Ajustar / Conteo`** documentando el número de comprobante o motivo en las notas del movimiento.

---

## Relacionado

- [Catálogo de Platos, Combos, Extras y Fotos](./01_catalogo_platos_combos_y_fotos.md)
- [Control de Caja, Gastos Menores y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)
- [Guía del Dueño: Finanzas, Personal y Reglas](./06_dueno_finanzas_personal_y_reglas.md)
- [Flujo de Recetas y Kardex (Procesos de Negocio)](../../business-processes/03-cocina-e-inventario/01_flujo_recetas_y_kardex.md)
- [Flujo de Atención Móvil y Conteo de Inventario](../../business-processes/01-pos-y-atencion/02_flujo_atencion_movil_y_conteo_inventario.md)
