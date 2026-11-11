# ADR-0018: Suite de Modificadores, Complementos, Notas de Cocina y Deducción Kardex (PB-020)

- **Estado:** `ACEPTADO`
- **Fecha:** 2026-11-07
- **Autores:** Squad Multi-Agente / Orquestador de Arquitectura
- **Alcance:** POS, Catálogo, Inventario / Kardex, Offline-First Dexie
- **Requerimiento:** `PB-020` (Motor de Precios, Modificadores, Extras y Personalización)
- **Tareas Relacionadas:** `TASK-501` [POS] [NEGOCIO], `TASK-502` [POS] [UI/UX]

---

## 1. Contexto y Problemática

En la operativa real de un restaurante, un plato del menú rara vez se vende sin personalización:
1. **Modificadores con costo adicional:** Porciones extras de ingredientes costosos ("Extra Queso Cheddar" +Bs. 5.00, "Doble Carne" +Bs. 8.00).
2. **Modificadores sin costo / Opciones obligatorias de preparación:** Puntos de cocción de carne ("Término Medio", "Bien Cocido"), elección de guarnición incluida ("Papas Fritas" o "Arroz con Queso"), o tipo de salsa ("Mayonesa Casera", "Ají de Maní").
3. **Modificadores por exclusión:** Preferencias o restricciones alimentarias ("Sin Cebolla", "Sin Mayonesa", "Sin Sal").
4. **Notas abiertas de cocina por ítem:** Instrucciones particulares ("Salsa aparte", "El cliente es alérgico al gluten").

El sistema requiere:
- **Consistencia de Precios:** Calcular el subtotal del ítem sumando el precio base del plato más la sumatoria de precios adicionales de los modificadores seleccionados:  
  $$\text{Item Unit Price} = \text{Base Price} + \sum_{m \in \text{Modifiers}} \text{Additional Price}_m$$
  $$\text{Item Subtotal} = \text{Item Unit Price} \times \text{Quantity}$$
- **Integridad de Kardex:** Deducir automáticamente los insumos consumidos por modificadores que tengan asociado un `ingredient_id` y `quantity_consumed > 0`, sin alterar insumos si el modificador es puramente comercial (`ingredient_id = null`).
- **Ergonomía POS:** Si un plato posee grupos de modificadores, abrir automáticamente el selector interactivo al agregarlo al pedido, validando límites (`minSelectable` y `maxSelectable`) y permitiendo redactar una nota específica de cocina.
- **Soporte Offline-First:** Almacenar de forma consistente los modificadores y notas en Dexie.js (`local_orders`) para sincronización idempotente sin pérdida de información.
- **Visualización en Comanda y Visor:** Mostrar con claridad los extras y notas en el carrito lateral, ticket térmico y en el visor de comandas en vivo (`/cash-control`).

---

## 2. Decisiones Arquitectónicas

### A. Tipología y Reglas de Grupos de Modificadores (`modifier_groups`)
- Cada grupo define:
  - `name`: Nombre descriptivo (ej. "Término de la Carne", "Salsas Incluidas", "Extras Gourmet").
  - `minSelectable`: Cantidad mínima requerida (0 para opcionales, $\ge 1$ para selecciones obligatorias).
  - `maxSelectable`: Cantidad máxima permitida ($\ge 1$).
  - `isRequired`: Flag booleano (`minSelectable \ge 1`).
- Los modificadores (`modifiers`) pertenecen a un grupo y contienen:
  - `name`: Nombre de la opción (ej. "Término Medio", "Extra Cheddar", "Sin Cebolla").
  - `additionalPrice`: Monto adicional a sumar al precio base ($\ge 0.00$).
  - `ingredientId`: UUID opcional del ingrediente en `ingredients`.
  - `quantityConsumed`: Cantidad física consumida del ingrediente (ej. `0.0500 kg`).

### B. Modelo de Persistencia en Órdenes (`order_item_modifiers` y `order_items`)
- `order_items` almacena la cabecera del ítem de comanda:
  - `dish_id`, `quantity`, `unit_price` (precio unitario efectivo con extras), `subtotal`, `notes` (notas de cocina).
- `order_item_modifiers` almacena la instantánea inmutable del modificador seleccionado:
  - `order_item_id`, `modifier_id`, `modifier_name`, `unit_price` (`additional_price`), `ingredient_id`, `quantity_consumed`.
- **Invariante de Trazabilidad:** Los nombres y precios se copian de forma inmutable a la tabla transaccional para que cambios posteriores en el catálogo no alteren el histórico contable.

### C. Deducción Atómica en Kardex mediante Eventos Desacoplados
- Al completar la venta en POS (`OrderServiceImpl`), se persiste la orden y se publica `OrderPaidEvent`.
- El listener del módulo de inventario (`KardexService` vía `@ApplicationModuleListener`):
  1. Deduce la receta base del plato (si aplica según estrategia BOM/unitaria de `ADR-0015`).
  2. Itera sobre cada modificador en `order_item_modifiers`: si `ingredient_id != null` y `quantity_consumed > 0`, emite un movimiento de Kardex tipo `SALE_CONSUMPTION` decrementando `branch_stock` de forma atómica:
     $$\text{Deducción Total} = \text{quantity\_consumed} \times \text{item.quantity}$$
  3. Si el modificador no tiene ingrediente asociado (`ingredient_id = null`), no genera movimiento de Kardex (concepto comercial o de servicio).
  4. Los modificadores de exclusión ("Sin X") no reintegran insumos por defecto (son directrices culinarias), registrándose como notas o modificadores informativos sin deducción.

### D. Interfaz POS y Modal Interactivo (TASK-502)
- Al pulsar un plato en la cuadrícula de catálogo:
  - Si el plato **no tiene** modificadores: se agrega directamente al carrito con cantidad 1.
  - Si el plato **tiene** grupos de modificadores: se abre automáticamente el modal `<app-modal>` / selector interactivo de extras (`data-testid="modifier-selection-modal"`).
- **Controles del Modal:**
  - Título y precio base del plato.
  - Secciones por grupo con chips/checkboxes de selección.
  - Contadores y validaciones reactivas en caliente (`minSelectable` / `maxSelectable`).
  - Campo de texto para notas especiales de cocina (`placeholder="Ej: Término medio, salsa aparte..."`).
  - Total dinámico en tiempo real (`Precio Base + Extras`).
  - Botón de confirmación habilitado únicamente si se satisfacen todas las restricciones de grupos obligatorios.
- **Carrito Lateral:**
  - El ítem en el carrito despliega de forma indentada los modificadores con su recargo (`+ Extra Queso (+Bs. 5.00)`) y la nota de cocina en texto cursivo tenue (`Notas: salsa aparte`).
  - Cada ítem con distinta combinación de modificadores o notas se maneja como una línea independiente en el carrito para no mezclar instrucciones de cocina.

### E. Integración en Visor de Comandas y Ticket Térmico
- En `/cash-control` (Visor de Comandas del Turno en Vivo):
  - El modal de detalle de comanda renderiza la lista de modificadores y notas por cada ítem.
- En la impresión de comanda (formato ticket 80mm/58mm):
  - Los extras y notas se imprimen inmediatamente debajo del nombre del plato con sangría.

---

## 3. Consecuencias y Beneficios

1. **Exactitud Operativa:** Cocina recibe exactamente lo que el cliente pidió, reduciendo devoluciones por platos mal preparados.
2. **Control Financiero Preciso:** Todo ingrediente adicional con costo se cobra y suma al arqueo de caja sin discrepancias.
3. **Control de Mermas y Kardex:** Los consumos adicionales se descuentan automáticamente del stock físico de la sucursal.
4. **Zoneless & Offline-First:** Toda la selección se gestiona con Angular Signals reactivos y se persiste localmente en Dexie.js antes de viajar al servidor.
