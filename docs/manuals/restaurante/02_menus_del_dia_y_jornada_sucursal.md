---
title: "Manual de Menús del Día y Jornada de Sucursal"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - menus-del-dia
  - jornada
  - cupos
  - combos
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 🍲 Manual de Menús del Día y Jornada de Sucursal

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Catálogo, Combos y Fotos](./01_catalogo_platos_combos_y_fotos.md) | [Manual del Cajero POS](./03_manual_cajero_pos.md)

En la opción **`Menús del Día`** administras las plantillas de almuerzos o menús ejecutivos diarios, controlas cuántas porciones quedan de cada plato y decides qué modalidad de servicio está activa hoy en todas las cajas de la sucursal.

---

## 1. El Panel `Jornada de Servicio de Hoy` (Compartido en toda la Sucursal)

En la parte superior de **`Menús del Día`** verás el panel **`Jornada de Servicio de Hoy`**. Lo que actives aquí se sincroniza para **todos los cajeros de la sucursal**:

1. **`Toda la Carta`:** Los cajeros ven y venden libremente toda la carta regular de platos y combos configurados en `Catálogo y Recetas`.
2. **`Menú del Día`:** Seleccionas una de tus plantillas de Menú del Día para activarla en la jornada actual. A partir de ese instante:
   - En el **`Punto de Venta (POS)`** aparece la sección del Menú del Día con sus tiempos, combos vinculados y cupos restantes en tiempo real.
   - Según la configuración del menú activo, los platos regulares de carta quedan ocultos o conviven según tu modo operativo.

> [!NOTE]
> Si tu sucursal tiene encendida la regla **"Apertura automática de jornada con Toda la Carta"** en `Parámetros del Sistema`, al abrir el primer turno del día el sistema inicia la jornada automáticamente en **`Toda la Carta`** si aún no elegiste un menú manualmente. Puedes cambiar de **`Toda la Carta`** a **`Menú del Día`** (y viceversa) en cualquier momento del servicio.

---

## 2. Crear y Editar Plantillas Multi-Día

Ya no hace falta duplicar un mismo menú cinco veces si repites tu menú semanal:

1. Pulsa para crear una nueva plantilla (o **Clonar** / **Editar** una existente).
2. Asigna un **Nombre descriptivo** (por ejemplo: *Almuerzo Ejecutivo Lunes-Miércoles-Viernes*).
3. **Selección Multi-Día:** Marca **uno o varios días de la semana** en los que aplica esta plantilla.
4. Elige la **Modalidad de Venta del Menú**:
   - **`Venta Individual por Plato`:** permite vender los platos del menú por separado en el POS (además de poder incluirse en combos si los vinculas).
   - **`Paquete Fijo`:** enfocado en vender el almuerzo como combo/paquete cerrado de tiempos.

---

## 3. Agregar Platos, Cupos (`∞`) y Regla `Solo Combo`

Dentro de cada plantilla agregas los platos organizados por tiempo o sección (ej. *Entrada/Sopa*, *Plato Fuerte*, *Postre*, *Refresco*) configurando tres controles clave por cada plato:

| Control | Para qué sirve | Ejemplo práctico |
| :--- | :--- | :--- |
| **`Stock ilimitado` (`∞`)** | Cuando está marcado, el plato se muestra con **`∞`** y nunca se agota por contador de menú (útil para acompañamientos o bebidas abundantes). | *Refresco del día* con **`∞`**, sin necesidad de contar vasos en el menú. |
| **Cupo numérico diario** | Si desmarcas `Stock ilimitado`, indicas exactamente cuántas porciones cocinaste hoy (ej. `35`). Cada venta o combo lo descuenta automáticamente y bloquea el plato al llegar a `0`. | *Sopa de Maní (35 porciones)* vs *Majadito (20 porciones)*. |
| **`Vender suelto en POS`** | Si está **marcado**, el cajero puede agregar ese plato individualmente al carrito. Si está **desmarcado**, el plato recibe la etiqueta **`Solo Combo`**: aparece visible para armar combos del Menú del Día, pero **no puede venderse suelto**. | Un *Postre incluido del almuerzo* marcado **`Solo Combo`** para no venderlo fuera del paquete. |

---

## 4. Vincular Combos de `Tiempo del Menú del Día`

En la misma gestión de la plantilla puedes **vincular combos** previamente armados en `Catálogo y Recetas` que utilicen componentes de tipo **`Tiempo del Menú del Día`**:
- Por ejemplo, vinculas el combo *"Almuerzo Completo Bs. 25"* (1 Sopa + 1 Segundo + 1 Postre).
- Cuando la jornada de hoy activa ese Menú del Día, al tocar *"Almuerzo Completo"* en el POS, el cajero elige en pantalla entre las sopas, segundos y postres definidos en esa plantilla que aún tengan cupo disponible.
- Si una de las opciones tiene recargo (por ejemplo *Churrasquito +Bs. 8*), el precio del combo se suma automáticamente.

---

## 5. Cierre del Día y Restricción Sin Internet

- **Cierre de jornada:** Al finalizar el día y cerrar los turnos de caja, la jornada queda cerrada y al día siguiente inicia limpia según tu regla de apertura automática o activación manual.
- **Conexión requerida:** Hoy en día, las consultas y descuentos de cupos de **`Menús del Día`** y combos requieren **conexión a internet activa** para coordinar en tiempo real los cupos compartidos entre cajas.

---

## Relacionado

- [Catálogo de Platos, Combos, Extras y Fotos](./01_catalogo_platos_combos_y_fotos.md)
- [Manual del Cajero: Punto de Venta (POS)](./03_manual_cajero_pos.md)
- [Control de Caja, Gastos Menores y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)
- [Flujo de Atención y Venta POS (Procesos de Negocio)](../../business-processes/01-pos-y-atencion/01_flujo_atencion_y_venta_pos.md)
