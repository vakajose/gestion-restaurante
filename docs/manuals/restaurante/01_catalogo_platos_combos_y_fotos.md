---
title: "Manual de Catálogo: Platos, Combos, Extras y Fotos"
category: "end-user-manual"
module: "erp"
tags:
  - manual
  - catalogo
  - platos
  - combos
  - extras
  - fotos
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 📋 Manual de Catálogo: Platos, Combos, Extras y Fotos

> ⬅️ **Índice:** [Manuales del Restaurante](./README.md) | [Guía de Inicio Rápido](./00_guia_inicio_rapido_restaurante.md) | [Menús del Día y Jornada](./02_menus_del_dia_y_jornada_sucursal.md)

En **`Catálogo y Recetas`** organizas todo lo que vende y consume tu sucursal. La pantalla se divide en **5 pestañas** para mantener en orden la carta, las promociones en combo, los agregados opcionales, los insumos de cocina y las categorías.

---

## 1. Cómo Funciona el Catálogo por Sucursal

De forma predeterminada, el sistema opera con **catálogo aislado por sucursal**:
- Cada sucursal es dueña de sus propios platos, precios, fotos y disponibilidad, sin alterar la carta de otros locales.
- Cuando necesitas repetir un plato parecido o llevar un plato exitoso de Casa Matriz a otra sucursal, usa el botón **`Clonar / Duplicar`**: podrás copiar ese plato (con su precio, categoría y receta) dentro de la **misma sucursal** o enviarlo directamente a **otra sucursal**.
- Si alguna vez administras un catálogo bajo modalidad compartida global heredada, verás la acción **`Ajustar`** sobre platos globales para personalizar su precio o disponibilidad en una sucursal puntual sin romper la definición original.

---

## 2. Pestaña de Categorías: Orden, Emojis y Códigos

Antes de cargar platos nuevos, organiza tus categorías (por ejemplo: *Hamburguesas*, *Sopas*, *Bebidas*, *Postres*):

1. Crea o edita una categoría ingresando su **Nombre** y **Orden de visualización** (controla en qué secuencia aparecen los filtros en el POS).
2. Elige un **Emoji identificatorio** con el selector integrado (🍔, 🍕, 🥤, 🥗) para que el cajero reconozca cada sección al instante.
3. Define un **Prefijo de Código** corto (por ejemplo `HAM`, `BEB`, `SOP`). Este prefijo alimenta el asistente automático de códigos de platos.

---

## 3. Pestaña de Platos y Carta: Alta, Edición y Fotos 4:3

### Crear o Editar un Plato
1. Pulsa para crear un plato nuevo o edita cualquier plato existente desde la tarjeta/fila del listado.
2. Completa:
   - **Nombre** y **Categoría**.
   - **Código Interno (SKU):** puedes escribirlo o pulsar **`Sugerir Código`**; el sistema tomará el prefijo de la categoría elegida (por ejemplo `HAM-001`, `HAM-002`) para evitar duplicados.
   - **Precio de Venta (Bs.)** y estado (**Activo / Disponible**).
   - **Mostrar en Carta Pública / Estándar** o disponibilidad para **Menú del Día** según corresponda.

### Subir y Recortar Fotografías
Un catálogo visual acelera la toma de pedidos en el POS:
1. Dentro del formulario del plato, selecciona una imagen desde tu dispositivo.
2. Se abrirá el **recortador visual integrado en proporción 4:3**: encuadra y ajusta el zoom sobre el centro del plato antes de confirmar.
3. Al guardar, el sistema optimiza automáticamente la foto a un formato web liviano para que cargue rápido en cajas y tablets sin ocupar espacio innecesario.

### Vincular Receta y Grupos de Extras al Plato
- **Receta (Consumo de Insumos):** Desde la ficha del plato (o la pestaña **`Insumos y Recetas`**) puedes indicar cuántos gramos, mililitros o unidades de cada insumo descuenta ese plato al venderse. El sistema te muestra el **costo calculado de receta y margen estimado** como guía comercial.
- **Grupos de Extras asignados:** Marca qué grupos de personalización (términos de cocción, salsas, bebidas incluidas, agregados pagos) se preguntarán al cajero al tocar este plato en el POS.

---

## 4. Pestaña de Grupos de Extras y Complementos

Sirven tanto para **preguntas obligatorias** (ej. *"Elige tu guarnición"*, *"Término de la carne"*) como para **adicionales opcionales** (ej. *"Extras con costo: Huevo frito +Bs. 4, Queso cheddar +Bs. 5"*).

Al crear o editar un grupo configuras:
1. **Nombre del Grupo** y si es **Obligatorio u Opcional** al vender el plato.
2. **Mínimo y Máximo de selecciones:**
   - Mínimo `1` / Máximo `1` para selección única obligatoria.
   - Mínimo `0` / Máximo libre o tope numérico para varios toppings opcionales.
3. **Opciones del Grupo:** para cada opción defines su nombre, su **precio extra en Bs.** (`0.00` si viene incluido), y **opcionalmente un vínculo a un insumo de inventario** (por ejemplo, elegir *"Extra Tocino"* descuenta automáticamente 30 g de tocino en el Kardex al cobrar).

---

## 5. Pestaña de Combos y Promociones

Los combos permiten vender un conjunto de platos o tiempos a un precio cerrado (con posibles variaciones según lo que elija el cliente). Cada componente dentro de un combo puede ser de uno de estos **3 tipos**:

| Tipo de Componente | Cómo funciona para el cliente y el cajero | Ejemplo práctico |
| :--- | :--- | :--- |
| **`Plato Fijo`** | Siempre entrega ese plato específico sin pedir elección al cajero. | *Papas medianas* incluidas siempre en el combo. |
| **`Opciones Específicas`** | El cajero elige una opción entre una lista definida por ti. Cada opción puede tener **ajuste de precio `+ Bs.` o `- Bs.`** respecto al precio base del combo. | Bebida a elegir: *Agua* (`+Bs. 0`), *Gaseosa Personal* (`+Bs. 2`), *Jugo Natural* (`+Bs. 4`). |
| **`Tiempo del Menú del Día`** | Toma dinámicamente las opciones cargadas para ese **tiempo** (ej. *Sopa*, *Segundo*, *Postre*) en el **Menú del Día activo de hoy**. Si una opción del día tiene suplemento, aplica el ajuste en Bs. | *Almuerzo Ejecutivo* compuesto por: 1 tiempo *Sopa*, 1 tiempo *Segundo* y 1 tiempo *Postre*. |

> [!TIP]
> Si creas un combo con componentes de tipo **`Tiempo del Menú del Día`**, también podrás vincularlo directamente desde la pantalla **`Menús del Día`** para activarlo junto con tus platos del día.

---

## 6. Limitaciones Actuales y Mejoras Previstas

- **Importación masiva por Excel/CSV:** No está disponible en esta versión; el alta inicial de platos e insumos se realiza desde los formularios del catálogo.
- **Promociones automáticas por horario (*Happy Hour*):** En la versión actual no existe aún una pantalla visual en **`Catálogo y Recetas`** para crear o programar descuentos automáticos por rango de horas. Si requieres precios especiales por combo, créalos como combos dedicados mientras se habilita la consola de promociones en futuras versiones.

---

## Relacionado

- [Menús del Día y Jornada de Sucursal](./02_menus_del_dia_y_jornada_sucursal.md)
- [Manual del Cajero: Punto de Venta (POS)](./03_manual_cajero_pos.md)
- [Insumos, Recetas, Conteos y Kardex](./05_insumos_recetas_y_kardex.md)
- [Flujo de Atención y Venta POS (Procesos de Negocio)](../../business-processes/01-pos-y-atencion/01_flujo_atencion_y_venta_pos.md)
