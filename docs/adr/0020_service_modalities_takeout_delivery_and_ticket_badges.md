---
title: "ADR-0020: Modalidades de Servicio (DINE_IN, TAKEOUT, DELIVERY, SCHEDULED_PICKUP), Selector Dual, Delivery Fee y Distintivos en Tickets (PB-032)"
category: "adr"
tags:
  - adr
  - pos
  - service-modalities
  - delivery
  - ux
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0020: Modalidades de Servicio (DINE_IN, TAKEOUT, DELIVERY, SCHEDULED_PICKUP), Selector Dual, Delivery Fee y Distintivos en Tickets (PB-032)

- **Alcance:** POS, Configuración (`modules.configuration`), Finanzas (`modules.finance`), Impresión Térmica — `PB-032` (`TASK-505`)

---

## 1. Contexto y Problemática

En un establecimiento gastronómico conviven diversas dinámicas de despacho que exigen tratamientos operativos, de empaque y financieros diferenciados:
1. **Consumo en Salón (`DINE_IN`):** Vajilla física, asignación de mesa y servicio atendido en sala.
2. **Para Llevar en Mostrador (`TAKEOUT`):** Pedido presencial donde el cliente espera en el local y requiere empaque descartable rápido para retiro inmediato.
3. **Encargo para Recoger (`SCHEDULED_PICKUP`):** Pedido solicitado con anticipación (vía telefónica o encargo previo) para ser retirado en una fecha/hora pactada. No es un pedido presencial común de mostrador; requiere coordinación horaria e instrucciones de empaque previo.
4. **Envío a Domicilio (`DELIVERY`):** Despacho hacia la ubicación del cliente mediante personal propio de reparto o mensajería externa, involucrando dirección de destino, teléfono de contacto y, frecuentemente, un recargo por servicio de flete/envío (`delivery_fee`).

**Retos de Usabilidad y Control:**
- **Ergonomía de Captura en POS:** El cajero a menudo conoce la modalidad desde el primer instante en que el cliente se acerca al mostrador ("Para llevar, por favor"), pero en otros casos la modalidad se define o rectifica en el último momento del cobro. Forzar la selección únicamente al inicio o únicamente al final ralentiza la atención.
- **Riesgo de Errores en Cocina:** Si la comanda de cocina o el ticket impreso no exhiben un distintivo visual inequívoco, el cocinero emplatará en vajilla de porcelana en lugar de empaquetar en bandejas descartables, generando pérdidas de tiempo, costos de empaque y demoras al cliente.
- **Transparencia Financiera del Flete:** El restaurante debe poder configurar si cobra o no costo de delivery (`pos.delivery_fee_enabled`) y cuál es la tarifa sugerida (`pos.delivery_fee_default`), sumándose con total transparencia al arqueo del turno.

---

## 2. Decisiones Arquitectónicas

### A. Tipado de las Modalidades de Servicio del Dominio
Se amplía el dominio transaccional de órdenes para soportar 4 modalidades mutuamente excluyentes:
- **`DINE_IN`** (En Salón / Mesa): Modalidad tradicional de consumo presencial.
- **`TAKEOUT`** (Para Llevar): Venta en mostrador para retiro inmediato por el cliente.
- **`SCHEDULED_PICKUP`** (Encargo para Recoger): Pedido pactado para retiro futuro en mostrador con instrucciones horarias registradas en notas.
- **`DELIVERY`** (A Domicilio): Pedido despachado a la dirección del cliente con costo de flete opcional.

### B. Selector Dual de Modalidad en Frontend (Barra Superior + Checkout Modal)
Para maximizar la ergonomía y velocidad operativa:
1. **Preselección en Barra Superior (`PosComponent`):** Botones rápidos / segmented control en el header de la terminal de venta (`[🍽️ Sala]`, `[🥡 Llevar]`, `[🛍️ Recoger]`, `[🛵 Delivery]`). Al seleccionarse, fija el signal reactivo `selectedOrderType`.
2. **Confirmación y Edición Contextual en Checkout Modal:** Al abrir la pasarela de cobro, el modal refleja la modalidad activa y permite conmutarla con un solo toque. Al seleccionar `DELIVERY` o `SCHEDULED_PICKUP`, se despliegan automáticamente los campos contextuales correspondientes.

### C. Campos Contextuales y Metadatos de la Orden (`orders`)
Se agregan los siguientes atributos en la tabla `orders` (migración Flyway `V13`):
- `order_type`: `VARCHAR(20) NOT NULL DEFAULT 'DINE_IN'` restringido por `CHECK (order_type IN ('DINE_IN', 'TAKEOUT', 'DELIVERY', 'SCHEDULED_PICKUP'))`.
- `customer_name`: `VARCHAR(100)` (Opcional; sugerido para `TAKEOUT` / `SCHEDULED_PICKUP` / `DELIVERY`).
- `customer_phone`: `VARCHAR(30)` (Opcional; recomendado para contacto en `DELIVERY` y `SCHEDULED_PICKUP`).
- `delivery_address`: `TEXT` (Requerido por validación de formulario si `order_type == 'DELIVERY'`).
- `delivery_fee`: `NUMERIC(10,2) NOT NULL DEFAULT 0.00` (Mayor o igual a 0.00; si es `0.00` indica flete gratuito).
- `delivery_notes`: `TEXT` (Instrucciones específicas de reparto o franja horaria pactada para recogida).

### D. Sumatoria Transaccional del Gran Total
Cuando `delivery_fee > 0.00`:
$$\text{totalAmount} = \sum (\text{item.unitPrice} \times \text{item.quantity}) + \text{deliveryFee}$$
El backend (`OrderServiceImpl`) valida que `deliveryFee >= 0` y totaliza el monto garantizando que el dinero recaudado en efectivo o digital ingrese formalmente a la contabilidad del turno.

### E. Parámetros del Sistema en `modules.configuration`
Se incorporan y exponen los siguientes parámetros en `SystemParameter`:
- `pos.default_order_mode`: Modalidad predeterminada al abrir nueva orden (Default: `'DINE_IN'`).
- `pos.allow_scheduled_pickup`: Habilita la modalidad de encargo para recoger en la interfaz (Default: `'true'`).
- `pos.delivery_fee_enabled`: Flag booleano para habilitar el recargo de despacho (Default: `'false'`).
- `pos.delivery_fee_default`: Monto sugerido por defecto para flete (Default: `'0.00'`).

### F. Rótulos y Distintivos Visuales en Tickets Impresos y Pantallas
En el ticket térmico y en el modal de comanda:
- Banner superior tipográfico de alto contraste según modalidad:
  - `*** PARA LLEVAR ***` (Fondo neutro destacado / texto inverso en papel térmico).
  - `*** ENCARGO PARA RECOGER ***` (Destacado con franja horaria pactada).
  - `*** DELIVERY / DOMICILIO ***` (Destacado con dirección de entrega y teléfono).
  - `CONSUMO EN SALÓN` (Comportamiento habitual en sala).
- Sección de despacho: si existe teléfono, dirección o notas, se imprimen inmediatamente debajo de los metadatos de la orden.
- Desglose contable: si `deliveryFee > 0`, se imprime la línea `COSTO DE ENVÍO: Bs. X.XX` antes del total general.

### G. Auditoría y Arqueo en Turnos de Caja (`/cash-control`)
- El visor de órdenes de la sesión activa (`CashControlComponent`) incorpora selector de filtro por modalidad (`TODAS`, `DINE_IN`, `TAKEOUT`, `SCHEDULED_PICKUP`, `DELIVERY`).
- Cada fila y tarjeta de comanda muestra el badge visual con icono y etiqueta de modalidad.
- Permite reimprimir comanda o ticket térmico manteniendo la modalidad y distintivos exactos.

### H. Gobernanza y Evolución Futura (PB-054)
Para este sprint, la modalidad `SCHEDULED_PICKUP` se opera de manera ágil y suficiente mediante el campo de notas (`delivery_notes`).
Se cataloga el ítem **`PB-054`** en el Product Backlog (Prioridad P3) para desarrollar en futuras versiones una agenda visual de pedidos programados con temporizadores y alertas anticipadas a cocina (KDS).

---

## 3. Consecuencias y Beneficios

- **Operatividad Intuitiva:** El cajero reduce clics al preseleccionar la modalidad desde el header del POS y validar o ajustar rápidamente en el checkout.
- **Cero Confusión en Cocina:** La comandera física y el KDS identifican de inmediato el tipo de embalaje necesario, reduciendo platos devueltos o reenvasados de último momento.
- **Consistencia Contable:** El costo de delivery se transparenta en la comanda, en el ticket fiscal/interno y en el arqueo de caja sin generar descuadres de dinero.
- **Compatibilidad Offline-First:** Dexie.js almacena los atributos de modalidad de servicio dentro de `pendingOrders` hasta su sincronización al recuperar red. *(Nota de estado real: la vista KDS interactiva pertenece al backlog futuro `PB-010`, y las tablas reales en `appDb` son `cachedCategories`, `cachedProducts`, `pendingOrders` y `activeShift` según [ADR-0003](./0003_offline_first_dexie_sync.md)).*

## Documentos Relacionados
- [ADR-0003: Estrategia Offline-First con Dexie.js e Idempotencia](./0003_offline_first_dexie_sync.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0012: Adaptabilidad Móvil y Experiencia Táctil](./0012_mobile_responsiveness_and_touch_ux.md)
- [ADR-0021: Catálogo Visual y Modos de Vista en POS](./0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)
- [Casos de Uso de POS](../project/architecture/03-use-cases/use_cases_pos.md)
- [Estado del Ciclo de Vida de la Orden](../project/architecture/04-behavior-and-flows/state_order_lifecycle.md)
