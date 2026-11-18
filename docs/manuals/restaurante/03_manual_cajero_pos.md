---
title: "Manual del Cajero: Punto de Venta (POS)"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - pos
  - cajero
  - ventas
  - offline
  - delivery
  - ticket
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 🧾 Manual del Cajero: Punto de Venta (POS)

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Menús del Día y Jornada](./02_menus_del_dia_y_jornada_sucursal.md) | [Control de Caja, Gastos y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)

Guía paso a paso para atender y cobrar pedidos rápidamente desde **`Punto de Venta (POS)`** en computadora, tablet o celular.

---

## 1. Antes de Empezar a Vender: Turno Abierto

Para poder cobrar en **`Punto de Venta (POS)`**, necesitas tener un **turno de caja abierto** en la sucursal actual:
- Si ingresas al POS sin turno abierto, verás el aviso para abrir caja desde **`Control de Caja`** (o directamente si tu cargo tiene permiso).
- **Aviso si el catálogo está vacío:** Si el restaurante es nuevo y todavía no cargó platos, el sistema te mostrará una advertencia con doble confirmación al intentar abrir turno para que decidas si deseas abrir igual o cargar platos primero.

---

## 2. Elegir Modalidad de Atención y Ver la Carta

En la barra superior del POS seleccionas primero **cómo se entrega el pedido**:

| Modalidad | Cuándo usarla | Datos que solicita |
| :--- | :--- | :--- |
| **`En Sala`** | Consumo en mesas dentro del local. | Opcional: nombre del cliente o número de mesa/comanda. |
| **`Para Llevar`** | Cliente pide en mostrador para llevar consigo. | Opcional: nombre del cliente para llamarlo al entregar. |
| **`Recoger`** | Cliente pasa a retirar su pedido programado. | Opcional: nombre del cliente. *(Nota: hoy esta opción permanece visible siempre en el POS).* |
| **`Delivery`** | Pedido con envío a domicilio. | **Obligatorio:** dirección de entrega. **Opcional al cobrar:** costo de envío manual en Bs. y nombre/referencia del cliente. |

> [!NOTE]
> **Modalidad al iniciar pantalla:** En la versión actual, cada vez que entras a **`Punto de Venta (POS)`** la pantalla inicia seleccionada por defecto en **`En Sala`**, y en **`Delivery`** el costo de envío comienza en `Bs. 0,00` para que lo escribas manualmente según la zona. La precarga automática por parámetro está prevista para futuras versiones.

### Modos de Vista y Filtros
- Cambia entre **`Galería`** (tarjetas grandes con fotografía 4:3), **`Compacto`** (tarjetas densas) o **`Lista`** (filas rápidas ideales cuando ya conoces los precios y quieres máxima velocidad). Tu preferencia queda guardada en ese equipo.
- Filtra por **categorías** (con sus emojis) o usa la barra de **búsqueda** por nombre o código interno.
- Si la sucursal activó **`Menú del Día`** en `Jornada de Servicio de Hoy`, verás disponibles los platos del menú con su contador de porciones restantes (o **`∞`** si es ilimitado). Si un plato muestra la etiqueta **`Solo Combo`**, únicamente se podrá elegir dentro de un combo del Menú del Día, no de forma suelta.

---

## 3. Armar el Pedido: Extras, Notas y Combos

1. **Platos simples:** Un toque agrega el plato al carrito en el panel derecho (o en el botón inferior en pantalla móvil). Usa `+` y `-` para ajustar cantidades o agrega una **nota para cocina** (ej. *"Sin cebolla"*, *"Bien cocido"*).
2. **Platos con Grupos de Extras:** Al tocar el plato se abre la ventana de opciones:
   - Completa los grupos marcados como **obligatorios** (por ejemplo, punto de cocción o guarnición).
   - Marca los adicionales opcionales con o sin costo extra.
   - Escribe notas adicionales para cocina antes de confirmar.
3. **Combos dinámicos:** Al elegir un combo verás paso a paso:
   - Los ítems **fijos** incluidos automáticamente.
   - Las elecciones de **opciones específicas** (mostrando si alguna opción suma o resta Bs. al precio base del combo).
   - Los **tiempos del Menú del Día** (por ejemplo *Sopa* y *Segundo*), listando las opciones disponibles hoy según el Menú del Día activo.

> [!TIP]
> Si la sucursal controla existencias en modo estricto y falta un insumo de la receta de algún plato, el sistema te avisará al intentar confirmar el cobro para evitar vender porciones sin inventario.

---

## 4. Cobrar y Emitir Ticket / Comanda

Al pulsar para cobrar el pedido:

1. **Elige el Medio de Pago:**
   - **`Efectivo`**: habilita la calculadora rápida de monto recibido con botones de acceso directo (**`Exacto`**, **`+5`**, **`+10`**, **`+20`**, **`+50`**, **`+100`**, **`+200`**) y calcula el **`Vuelto`** al instante.
   - **`Tarjeta`**
   - **`Pago QR`**
   - **`Transferencia`**
2. **Delivery (si aplica):** Revisa la dirección obligatoria e ingresa manualmente el monto adicional por envío antes de confirmar.
3. **Confirmar Cobro:** Al finalizar se genera el pedido y puedes abrir/imprimir el **Ticket / Comanda** (`Imprimir Ticket`):
   - Muestra el **número de comanda**, la insignia de modalidad (`En Sala`, `Para Llevar`, `Recoger`, `Delivery`), fecha y **hora local de tu sucursal**, detalle de platos, extras, notas para cocina, dirección/costo de delivery (si aplica), total pagado y vuelto.

> [!NOTE]
> **¿Cómo recibe cocina los pedidos hoy?** En esta versión actual no existe todavía una pantalla digital separada en cocina: las órdenes salen como **comanda/ticket imprimible** con el detalle de extras y notas para entregar en barra o cocina.

---

## 5. Qué Pasa si Se Corta Internet (Modo Sin Conexión Real Hoy)

El sistema guarda en la memoria local del navegador tu catálogo de platos individuales, categorías, extras y tu turno de caja abierto para que no detengas la atención si falla el Wi-Fi:

| Función | Con Internet | Sin Internet (Modo Offline hoy) |
| :--- | :---: | :---: |
| **Vender platos individuales y sus extras** | ✅ | ✅ Funciona normalmente; la venta se guarda en cola y sale con código temporal **`LOCAL-...`**. |
| **Emitir ticket con vuelto e historial local** | ✅ | ✅ Muestra etiqueta **`LOCAL-...`**; al volver internet se sincroniza automáticamente. |
| **Vender Combos y Menús del Día** | ✅ | ❌ **Requiere conexión activa** hoy para validar cupos compartidos entre cajas y elecciones del menú. |
| **Abrir/cerrar turno, gastos o anulaciones** | ✅ | ❌ Requiere conexión para validar caja y PIN con el servidor. |

*La compatibilidad completa sin internet para combos y Menús del Día está prevista para próximas versiones.*

---

## Relacionado

- [Control de Caja, Gastos Menores y Arqueo](./04_control_de_caja_gastos_y_arqueo.md)
- [Menús del Día y Jornada de Sucursal](./02_menus_del_dia_y_jornada_sucursal.md)
- [Catálogo de Platos, Combos, Extras y Fotos](./01_catalogo_platos_combos_y_fotos.md)
- [Flujo de Atención y Venta POS (Procesos de Negocio)](../../business-processes/01-pos-y-atencion/01_flujo_atencion_y_venta_pos.md)
