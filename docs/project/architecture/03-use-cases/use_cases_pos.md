---
title: "Casos de Uso - Módulo Punto de Venta (POS & Ventas)"
category: "architecture"
level: "Use-Case"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - use-cases
  - pos
  - offline-first
  - dexie
  - signals
last_updated: "2026-10-25"
status: "verified"
---

# 🛒 Casos de Uso - Módulo Punto de Venta (POS & Ventas)

El módulo de **Punto de Venta (POS & Ventas)** es el núcleo de captura transaccional del restaurante. Diseñado bajo el paradigma **Offline-First**, permite la venta ininterrumpida frente a pérdidas de conectividad mediante persistencia local en **Dexie.js (IndexedDB)** y garantiza la consistencia del servidor con transacciones idempotentes (`client_transaction_id`).

---

## 🗺️ 1. Diagrama de Casos de Uso del Módulo POS

```mermaid
flowchart LR
    %% Actores
    subgraph Actores["Actores del Módulo POS"]
        direction TB
        Act_CS["👤 CASHIER<br/>(Cajero / Operador POS)"]
        Act_KT["👤 KITCHEN<br/>(Personal de Cocina / KDS)"]
        Act_SYS["⚙️ Backend Modulith<br/>(Motor de Eventos)"]
    end

    %% Casos de Uso
    subgraph Mod_POS["Límites del Módulo POS (Angular 22 Zoneless & Spring Modulith)"]
        direction TB
        UC_POS_01["UC-POS-01: Cargar y Cachear Catálogo Local<br/>(Dexie.js / IndexedDB)"]
        UC_POS_02["UC-POS-02: Armar Carrito de Ventas Reactivo<br/>(Angular Signals y Totales)"]
        UC_POS_03["UC-POS-03: Checkout y Cobro Idempotente<br/>(UUID client_transaction_id)"]
        UC_POS_04["UC-POS-04: Transmisión Online de Comanda<br/>(Header X-Client-Transaction-Id)"]
        UC_POS_05["UC-POS-05: Reconciliación Batch Offline-to-Online<br/>(POST /pos/orders/sync)"]
        UC_POS_06["UC-POS-06: Emisión de OrderPaidEvent<br/>(Spring Modulith Coreografía)"]
        UC_POS_07["UC-POS-07: Atención Móvil y Comanda Deslizable<br/>(Floating Bottom Sheet & Touch UX)"]
    end

    %% Asociaciones de Actores
    Act_CS --> UC_POS_01
    Act_CS --> UC_POS_02
    Act_CS --> UC_POS_03
    Act_CS --> UC_POS_04
    Act_CS --> UC_POS_05
    Act_CS --> UC_POS_07

    Act_KT --> UC_POS_03
    Act_SYS --> UC_POS_06

    %% Relaciones entre Casos de Uso
    UC_POS_02 -.->|"<<precede>>"| UC_POS_01
    UC_POS_07 -.->|"<<extends en móvil < 768px>>"| UC_POS_02
    UC_POS_03 -.->|"<<include>>"| UC_POS_02
    UC_POS_03 -->|"Si está Online"| UC_POS_04
    UC_POS_03 -->|"Si está Offline"| UC_POS_05
    UC_POS_04 -.->|"<<triggers>>"| UC_POS_06
    UC_POS_05 -.->|"<<triggers por cada orden>>"| UC_POS_06
```

---

## 📝 2. Especificación Detallada de Casos de Uso

---

### UC-POS-01: Cargar y Cachear Catálogo en Memoria Local (Dexie.js)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-01` |
| **Nombre** | Cargar y Cachear Catálogo en Memoria Local (Dexie.js) |
| **Actor Principal** | `CASHIER` |
| **Actores Secundarios** | Sistema Local (Dexie.js / IndexedDB) |
| **Objetivo** | Descargar el catálogo de categorías y productos activos de la sucursal y persistirlos en la base de datos local del navegador para permitir búsquedas instantáneas y operación 100% offline. |
| **Precondiciones** | El usuario debe haber iniciado sesión y tener una sucursal asignada (`branchId`). |
| **Flujo Principal** | 1. El cajero abre la pantalla de ventas `/pos`.<br/>2. El servicio `PosService.loadCatalog(branchId)` comprueba la conectividad mediante `isOnline()`.<br/>3. **Estrategia Online (Cache-Refresh):**<br/>   a. Solicita en paralelo `GET /api/v1/catalog/categories` y `GET /api/v1/catalog/dishes?branchId={branchId}`.<br/>   b. Limpia las tablas locales `cachedCategories` y `cachedProducts`.<br/>   c. Inserta en bloque los registros descargados (`bulkPut`).<br/>   d. Actualiza los signals de interfaz `categories` y `products`.<br/>4. **Estrategia Offline (Fallback):**<br/>   a. Si la red no responde o `isOnline() == false`, consulta directamente `appDb.cachedCategories.toArray()` y `appDb.cachedProducts.toArray()`.<br/>   b. Carga los productos en los signals sin bloquear la pantalla.<br/>5. La interfaz del catálogo queda lista para interacción inmediata. |
| **Flujos Alternativos / Excepciones** | - **Primer inicio sin internet y sin caché previa:** Si Dexie está vacío y no hay conexión, se muestra una alerta informativa solicitando conectarse al menos una vez para la descarga inicial de datos.<br/>- **Error en IndexedDB:** Si el navegador restringe el almacenamiento local (modo incógnito estricto), los datos se mantienen en memoria volátil de Angular Signals. |
| **Postcondiciones** | El menú completo de la sucursal está disponible en memoria local del navegador sin latencia de red. |
| **Contrato Técnico** | - Backend: `GET /api/v1/catalog/categories`, `GET /api/v1/catalog/dishes?branchId={branchId}`<br/>- Local DB: `cachedCategories: 'id, name, sortOrder'`, `cachedProducts: 'id, categoryId, code, name, salePrice, isAvailable'` |
| **Entidades Afectadas** | Dexie.js: `cachedCategories`, `cachedProducts` |

---

### UC-POS-02: Armar Carrito de Ventas Reactivo (Angular Signals)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-02` |
| **Nombre** | Armar Carrito de Ventas Reactivo |
| **Actor Principal** | `CASHIER` |
| **Actores Secundarios** | Ninguno |
| **Objetivo** | Permitir la selección ágil de platos, modificación de cantidades, adición de notas especiales de comanda (ej. "sin cebolla") y cálculo reactivo en tiempo real de subtotales y total a pagar sin disparar re-renderizados innecesarios (Zoneless). |
| **Precondiciones** | El catálogo local debe estar cargado en Dexie (`UC-POS-01`). |
| **Flujo Principal** | 1. El cajero pulsa una tarjeta de producto o escanea/busca su código.<br/>2. Se invoca `PosService.addToCart(dish, quantity, notes)`:<br/>   - Si el ítem ya está en el carrito (`dishId`), se incrementa `quantity` y se recalcula `subtotal = Number((quantity * unitPrice).toFixed(2))`.<br/>   - Si es nuevo, se crea un nuevo objeto `CartItem` y se inserta en el signal `cartItems`.<br/>3. Los signals computados (`computed`) recalculan automáticamente en microtareas:<br/>   - `totalAmount`: Suma de todos los subtotales con redondeo a 2 decimales.<br/>   - `totalItemsCount`: Suma total de unidades.<br/>4. El cajero puede incrementar/decrementar cantidades (`updateQuantity`), eliminar ítems (`removeFromCart`) o vaciar el pedido (`clearCart`). |
| **Flujos Alternativos / Excepciones** | - **Disminución a cero:** Si el usuario presiona "-" cuando la cantidad es 1, el ítem se elimina automáticamente del carrito.<br/>- **Producto no disponible:** Si `isAvailable == false`, el botón de la UI se bloquea impidiendo la adición al carrito. |
| **Postcondiciones** | El signal `cartItems` contiene los ítems listos para el cobro y `totalAmount` refleja el monto exacto a cobrar. |
| **Contrato Técnico** | Estado reactivo frontend en `PosService`: `cartItems = signal<CartItem[]>([])`, `totalAmount = computed(...)`. |
| **Entidades Afectadas** | Memoria volátil del cliente Angular 22 (Zoneless). |

---

### UC-POS-03: Checkout y Cobro de Comanda con Idempotencia

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-03` |
| **Nombre** | Checkout y Cobro de Comanda con Idempotencia (`client_transaction_id`) |
| **Actor Principal** | `CASHIER` |
| **Actores Secundarios** | `KITCHEN` (Recibe comanda local o impresa) |
| **Objetivo** | Finalizar la venta registrando el método de pago (`CASH`, `CARD`, `QR`, `TRANSFER`), asociando la orden al turno de caja abierto (`cash_shifts`), garantizando idempotencia mediante un UUID único originado en el cliente y persistiendo inmediatamente en Dexie.js antes de cualquier intento de red. |
| **Precondiciones** | El carrito debe tener al menos un ítem (`cartItems.length > 0`). Debe existir un turno de caja abierto (`cashShiftId`). |
| **Flujo Principal** | 1. El cajero pulsa "Cobrar" y selecciona el método de pago (`CASH`, `CARD`, `QR`, `TRANSFER`).<br/>2. El sistema genera un identificador de transacción único universal (`client_transaction_id = crypto.randomUUID()`).<br/>3. Se genera un número de comprobante provisional `ticketNumber = LOCAL-{clientTxId[:8]}`.<br/>4. **Persistencia Local Inmediata (Offline-Safe):**<br/>   - Se crea una entidad `PendingOrderEntity` con `syncStatus = 'PENDING'` y se guarda en `appDb.pendingOrders`.<br/>   - Se incrementa el contador `pendingCount`.<br/>   - Se limpia el carrito de venta (`clearCart`).<br/>5. **Derivación de Envío:**<br/>   - Si `isOnline() == true`, ejecuta de inmediato `UC-POS-04` (Transmisión Online).<br/>   - Si `isOnline() == false`, finaliza el flujo local emitiendo el comprobante offline. |
| **Flujos Alternativos / Excepciones** | - **Carrito vacío:** Arroja excepción "El carrito de venta está vacío" bloqueando el botón.<br/>- **Sin turno abierto:** Se alerta al usuario de que debe abrir caja antes de proceder con el cobro. |
| **Postcondiciones** | La venta queda registrada de forma persistente e indestructible en el navegador. Si hay conexión, se envía al servidor; de lo contrario, queda encolada para sincronización batch posterior. |
| **Contrato Técnico** | - Local: `appDb.pendingOrders.add(pendingEntity)`<br/>- Modelo: `PendingOrderEntity` (`localId`, `clientTransactionId`, `syncStatus`, `createdAt`, `orderData`, `ticketNumber`, `totalAmount`) |
| **Entidades Afectadas** | Dexie.js: `pendingOrders` |

---

### UC-POS-04: Transmisión Online de Comanda con Cabecera `X-Client-Transaction-Id`

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-04` |
| **Nombre** | Transmisión Online de Comanda con cabecera `X-Client-Transaction-Id` |
| **Actor Principal** | `CASHIER` (Desencadenado por `UC-POS-03`) |
| **Actores Secundarios** | Backend Spring Modulith (`OrderController`) |
| **Objetivo** | Enviar la comanda cobrada al backend mediante petición HTTP POST segura, adjuntando la cabecera de idempotencia para evitar cobros duplicados ante reintentos de red. |
| **Precondiciones** | `isOnline() == true` y token JWT presente en cabeceras (`Authorization: Bearer ...`). |
| **Flujo Principal** | 1. El frontend envía `POST /api/v1/pos/orders` con:<br/>   - Cabecera: `X-Client-Transaction-Id: {clientTransactionId}`.<br/>   - Cuerpo: `CreateOrderRequest` (`clientTransactionId`, `branchId`, `cashShiftId`, `paymentMethod`, `notes`, `items`).<br/>2. El backend intercepta la solicitud y verifica si ya existe una orden con ese `client_transaction_id` en el tenant:<br/>   - **Si ya existe (Reintento de Red):** Recupera la orden previa sin duplicar cobro ni eventos y retorna `200 OK`.<br/>   - **Si es nueva:**<br/>     a. Persiste `Order` con estado `PAID` y sus `order_items` en PostgreSQL.<br/>     b. Genera el número secuencial de ticket oficial (ej. "TKT-001042").<br/>     c. Ejecuta `UC-POS-06` (Publicación de `OrderPaidEvent`).<br/>     d. Retorna `201 Created` con el `OrderDto` consolidado.<br/>3. Al recibir `200` o `201`, el frontend actualiza el registro en Dexie `pendingOrders` marcándolo como `syncStatus = 'SYNCED'`.<br/>4. Se decrementa `pendingCount`. |
| **Flujos Alternativos / Excepciones** | - **Caída de conexión durante el envío (Timeout o Network Error):** La orden permanece en Dexie como `syncStatus = 'PENDING'`. No se pierde la venta y será sincronizada por `UC-POS-05`.<br/>- **Turno de caja cerrado en servidor:** Retorna `400 Bad Request` informando que el turno fue cerrado remotamente. |
| **Postcondiciones** | La orden queda grabada en PostgreSQL y se disparan los procesos en segundo plano de inventario y caja. |
| **Contrato de API** | - `POST /api/v1/pos/orders`<br/>- Headers: `X-Client-Transaction-Id: UUID`<br/>- Response: `201 Created` / `200 OK` (`OrderDto`) |
| **Entidades Afectadas** | PostgreSQL: `orders`, `order_items`. Dexie.js: `pendingOrders` (actualización a `SYNCED`) |

---

### UC-POS-05: Reconciliación Asíncrona Offline-to-Online (Batch `/sync`)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-05` |
| **Nombre** | Reconciliación Asíncrona Offline-to-Online (Batch `/sync` de pendingOrders) |
| **Actor Principal** | Sistema Frontend / `CASHIER` |
| **Actores Secundarios** | Backend Spring Modulith (`OrderController`) |
| **Objetivo** | Sincronizar en bloque todas las ventas acumuladas localmente mientras la terminal estuvo fuera de línea tan pronto se detecta la restauración de la conexión a internet. |
| **Precondiciones** | Existen registros en Dexie con `syncStatus = 'PENDING'` y el navegador emite el evento global `window.addEventListener('online')` o el cajero pulsa manualmente "Sincronizar". |
| **Flujo Principal** | 1. `PosService` detecta la reconexión y activa el signal `isSyncing.set(true)`.<br/>2. Consulta en Dexie: `appDb.pendingOrders.where('syncStatus').equals('PENDING').toArray()`.<br/>3. Si hay órdenes pendientes, arma el payload `SyncOrdersRequest` con la lista de `CreateOrderRequest`.<br/>4. Emite `POST /api/v1/pos/orders/sync`.<br/>5. El backend procesa el lote de manera atómica:<br/>   - Para cada orden, verifica idempotencia por `client_transaction_id`.<br/>   - Inserta órdenes nuevas, publica los correspondientes `OrderPaidEvent` e ignora duplicados.<br/>   - Retorna `SyncOrdersResponse` con conteos: `processed`, `synced`, `duplicates`, `failed` y detalles de error si aplican.<br/>6. El cliente actualiza en Dexie todos los registros procesados a `syncStatus = 'SYNCED'`.<br/>7. Se actualiza el signal `pendingCount` a 0 y `isSyncing.set(false)`. |
| **Flujos Alternativos / Excepciones** | - **Fallo parcial en lote:** Si alguna orden puntual contiene un `dishId` eliminado, el backend reporta el fallo en `SyncOrdersResponse.errors` sin abortar el resto del lote.<br/>- **Desconexión en plena sincronización:** La sincronización se suspende y se reintentará en el siguiente ciclo `online` sin duplicar transacciones gracias a la idempotencia. |
| **Postcondiciones** | Todas las comandas acumuladas offline se encuentran conciliadas en PostgreSQL sin diferencias monetarias ni pérdidas de stock. |
| **Contrato de API** | - `POST /api/v1/pos/orders/sync` → `200 OK` (`SyncOrdersRequest` → `SyncOrdersResponse`) |
| **Entidades Afectadas** | PostgreSQL: `orders`, `order_items`. Dexie.js: `pendingOrders` |

---

### UC-POS-06: Emisión del Evento del Dominio `OrderPaidEvent`

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-06` |
| **Nombre** | Emisión del Evento del Dominio `OrderPaidEvent` |
| **Actor Principal** | Sistema Backend (`OrderServiceImpl`) |
| **Actores Secundarios** | `InventoryEventListener`, `FinanceEventListener` |
| **Objetivo** | Publicar de forma atómica y desacoplada el evento de dominio que notifica a toda la arquitectura modular que una comanda ha sido pagada y debe repercutir en inventario y caja. |
| **Precondiciones** | La orden debe haber sido persistida con éxito en PostgreSQL con estado `PAID` (`UC-POS-04` o `UC-POS-05`). |
| **Flujo Principal** | 1. El servicio `OrderServiceImpl` construye el record inmutable `OrderPaidEvent` con:<br/>   - `eventId`: UUID único de evento.<br/>   - `tenantId`, `branchId`, `orderId`, `clientTransactionId`.<br/>   - `ticketNumber`: Código de ticket secuencial.<br/>   - `items`: Lista de `OrderItemSummary` (`dishId`, `quantity`, `unitPrice`).<br/>   - `totalAmount`, `paymentMethod`, `paidAt`, `cashShiftId`.<br/>2. Publica el evento en el bus de eventos de Spring Modulith mediante `ApplicationEventPublisher.publishEvent(...)`.<br/>3. Spring Modulith intercepta el evento e inserta un registro en la tabla de outbox `event_publication` para garantizar persistencia y reintentos automáticos.<br/>4. Los listeners modulares (`InventoryEventListener` y `FinanceEventListener`) reciben el evento en hilos virtuales dedicados. |
| **Flujos Alternativos / Excepciones** | - **Fallo en Listener Asíncrono:** Si el listener de inventario falla por concurrencia, Spring Modulith mantiene el evento marcado como pendiente en `event_publication` para reintento automático sin comprometer la respuesta HTTP de la venta al cajero. |
| **Postcondiciones** | El evento queda despachado. Se inician de inmediato la deducción de recetas en Kardex ([use_cases_inventory.md](./use_cases_inventory.md)) y la acumulación de ventas en el turno de caja ([use_cases_finance.md](./use_cases_finance.md)). |
| **Contrato Técnico** | - Record: `com.restaurant.app.modules.pos.api.OrderPaidEvent`<br/>- Bus: `org.springframework.context.ApplicationEventPublisher`<br/>- Outbox: tabla `event_publication` de Spring Modulith |
| **Entidades Afectadas** | `event_publication` |

---

### UC-POS-07: Atención Móvil y Comanda Deslizable en Bottom Sheet (ADR-0012)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-07` |
| **Nombre** | Atención Móvil y Comanda Deslizable en Bottom Sheet |
| **Actor Principal** | `CASHIER` / Mesero o Garzón de Salón con Smartphone |
| **Actores Secundarios** | Dexie.js (Persistencia reactiva local) |
| **Objetivo** | Permitir la captura ágil de pedidos en mesa desde smartphones (`< 768px`) mediante un catálogo en cuadrícula o lista compacta y una comanda deslizable inferior (*Floating Bottom Sheet Bar*), eliminando el scroll vertical disruptivo y evitando la pérdida de contexto del total vendido. |
| **Precondiciones** | La aplicación se ejecuta en un viewport de smartphone o tablet pequeña (`width < 768px`). Catálogo cacheado en Dexie.js. |
| **Flujo Principal** | 1. El operador visualiza el catálogo adaptado en cuadrícula de 2 columnas o lista compacta de un toque.<br/>2. Toca un plato o botón `[+]`; el plato se agrega al carrito mediante signals reactivos en `PosService`.<br/>3. La interfaz renderiza la **Barra Flotante Inferior de Comanda** fija en el pie (`fixed bottom-0 z-30`) indicando el conteo de ítems y el importe acumulado: `🛒 3 Ítems \| Bs. 85.00 [Ver Comanda ⬆]`.<br/>4. El operador toca o arrastra hacia arriba la barra flotante.<br/>5. Se despliega el **Bottom Sheet de Comanda** ocupando el 85% de la pantalla con animación CSS acelerada por GPU y fondo oscuro semitransparente (*backdrop*).<br/>6. El operador ajusta cantidades (`+`/`-`), elimina ítems o agrega notas de cocina.<br/>7. Toca el botón de ancho completo `[💰 Proceder al Cobro]`.<br/>8. Se presenta la hoja de checkout con selector de método de pago y teclado numérico táctil de dígitos grandes.<br/>9. Confirma la venta; se almacena la orden en Dexie.js con `client_transaction_id` y se sincroniza automáticamente si hay conexión. |
| **Flujos Alternativos / Excepciones** | - **Cierre de Hoja sin Cobro:** El operador toca el backdrop o desliza hacia abajo la comanda; el panel se repliega suavemente a la barra inferior sin perder los ítems acumulados.<br/>- **Desconexión Offline:** El flujo se completa de forma idéntica; la orden queda marcada como `PENDING` en Dexie.js y se emite notificación discreta. |
| **Postcondiciones** | Orden cobrada o guardada localmente con ergonomía de una mano, sin recargar página ni desbordamientos horizontales. |
| **Contrato Técnico** | - Componentes: `PosComponent`, `BottomSheetComponent`<br/>- Especificación: [ADR-0012](../../adr/0012_mobile_responsiveness_and_touch_ux.md)<br/>- Diagrama de Estados: [state_mobile_pos_and_inventory_ux.md](../04-behavior-and-flows/state_mobile_pos_and_inventory_ux.md) |
| **Entidades Afectadas** | Dexie.js: `pendingOrders`, signals de `PosService` |

---

### UC-POS-08: Asignación de Menú Diario al Turno de Caja y Ajuste de Cupos (PB-034)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-08` |
| **Nombre** | Asignación de Menú Diario al Turno de Caja y Ajuste de Cupos Iniciales |
| **Actor Principal** | `CASHIER`, `BRANCH_MANAGER` |
| **Actores Secundarios** | Módulo de Catálogo |
| **Objetivo** | Vincular una plantilla de menú diario al momento de abrir el turno de caja (`cash_shifts`), personalizando los cupos reales de raciones preparadas en cocina para la jornada sin mutar la plantilla maestra. |
| **Precondiciones** | El usuario debe estar autorizado para abrir turno (`FINANCE_SHIFTS` o parámetro permisivo). Deben existir plantillas de menú activas en el tenant. |
| **Flujo Principal** | 1. El operador abre el modal "Apertura de Turno" e ingresa el monto inicial en gaveta (`initial_cash`).<br/>2. El modal consulta plantillas sugeridas mediante `GET /api/v1/catalog/menus?templateOnly=true&dayOfWeek={CURRENT_DAY}`.<br/>3. El usuario selecciona la plantilla deseada (o "Sin Menú Diario").<br/>4. Se despliegan los platos clasificados por tiempo (`STARTER`, `MAIN`, `DESSERT`, `BEVERAGE`) con sus cupos sugeridos.<br/>5. El operador ajusta las raciones reales acordadas con cocina (ej. Milanesa: 30, Pollo al Horno: 20).<br/>6. Pulsa "Confirmar Apertura de Turno".<br/>7. El backend crea el `cash_shifts` e inserta `shift_menu_assignments` y `shift_menu_items` de forma atómica.<br/>8. El turno queda abierto y el catálogo activo del POS destaca el menú asignado. |
| **Flujos Alternativos / Excepciones** | - **Apertura sin Menú:** El operador elige omitir menú diario; el POS opera normalmente con la carta completa abierta.<br/>- **Cupo Cero:** Si un plato sugerido no se preparó hoy, se coloca cupo 0 y se inicializa con `is_sold_out = true`. |
| **Postcondiciones** | El turno de caja tiene cupos independientes aislados en base de datos; la plantilla corporativa base permanece intacta. |
| **Contrato de API** | - `POST /api/v1/finance/shifts/open` (con payload enriquecido `menuTemplateId`, `customQuotas`)<br/>- `GET /api/v1/catalog/shifts/{shiftId}/menu` → `200 OK` (`ShiftMenuDetailDto`) |
| **Entidades Afectadas** | `cash_shifts`, `shift_menu_assignments`, `shift_menu_items` |

---

### UC-POS-09: Venta de Menú Diario con Control Atómico de Cupos y Optimistic Locking (PB-034)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-09` |
| **Nombre** | Venta de Menú Diario con Control Atómico de Cupos y Optimistic Locking |
| **Actor Principal** | `CASHIER` |
| **Actores Secundarios** | Módulo de Catálogo, Motor ACID PostgreSQL |
| **Objetivo** | Procesar pedidos en mostrador validando y decrementando de manera concurrente e infalible el cupo de raciones del menú del día, impidiendo sobreventas físicas mediante `@Version` y actualizaciones condicionales. |
| **Precondiciones** | Turno de caja abierto con menú diario asignado. Platos con cupo remanente mayor a cero. |
| **Flujo Principal** | 1. En la pantalla del POS, el cajero visualiza las pestañas del menú diario con badges de cupo restante (`Cupo: 14`).<br/>2. Agrega platos a la comanda y pulsa "Cobrar".<br/>3. El frontend emite `POST /api/v1/pos/orders`.<br/>4. Dentro de una transacción `@Transactional (READ_COMMITTED)`, el backend ejecuta para cada plato asignado al turno:<br/>   `UPDATE shift_menu_items SET current_quota = current_quota - :qty, sold_count = sold_count + :qty, version = version + 1 WHERE id = :id AND current_quota >= :qty AND version = :version;`<br/>5. Si la fila se actualiza (rows affected == 1), la orden se guarda, se emite el ticket y se publica `OrderPaidEvent`.<br/>6. Si el cupo resultante llega a cero, el flag `is_sold_out` se marca `TRUE` y se emite evento de actualización de cupos. |
| **Flujos Alternativos / Excepciones** | - **Cupo Insuficiente o Conflicto Concurrente (0 rows affected):** El backend ejecuta rollback automático y arroja `ItemSoldOutException`. Retorna HTTP 409 (`ITEM_SOLD_OUT`). El POS muestra advertencia visual y marca el botón como `AGOTADO`.<br/>- **Conmutador a Carta Completa:** El cajero puede pulsar "Ver Carta Completa" para añadir platos maestros no pertenecientes al menú del día sin validación de cupo de turno. |
| **Postcondiciones** | El cupo del plato en el turno se decrementa exactamente en las raciones vendidas; la venta queda asentada con consistencia ACID garantizada. |
| **Contrato de API** | - `POST /api/v1/pos/orders` → `201 Created` o `409 Conflict` (RFC 7807 `ITEM_SOLD_OUT`) |
| **Entidades Afectadas** | `shift_menu_items`, `orders`, `order_items` |

---

### UC-POS-10: Ampliación Supervisada de Cupos de Turno en Vivo (PB-034)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-POS-10` |
| **Nombre** | Ampliación Supervisada de Cupos de Turno en Vivo (Live Quota Expansion) |
| **Actor Principal** | `BRANCH_MANAGER`, `ADMIN_TENANT`, o Cajero con autorización |
| **Actores Secundarios** | Terminales POS conectados |
| **Objetivo** | Permitir inyectar raciones adicionales a un plato que se estaba agotando (ej. cocina horneó 15 raciones más de pastel de papa), desbloqueando inmediatamente su venta en POS con auditoría no repudiable. |
| **Precondiciones** | Turno de caja abierto con menú diario asignado. |
| **Flujo Principal** | 1. Desde el POS o desde el panel de supervisión de turno, el supervisor pulsa el badge del plato o selecciona "Ajustar Cupo".<br/>2. Ingresa la cantidad a ampliar (`quantityDelta = +15`) y el motivo ("Nueva tanda lista en cocina").<br/>3. Pulsa "Confirmar Ajuste".<br/>4. El backend incrementa `current_quota` en `shift_menu_items`, restablece `is_sold_out = FALSE` si estaba agotado, e inserta una fila inmutable en `shift_menu_quota_adjustments`.<br/>5. Responde `200 OK` con el saldo actualizado.<br/>6. Los terminales POS reactivan el botón del plato con el nuevo cupo disponible. |
| **Flujos Alternativos / Excepciones** | - **Reducción de Cupo por Merma:** El supervisor puede ingresar una cantidad negativa (`quantityDelta = -5`) si se quemó una porción en cocina, siempre que `current_quota + quantityDelta >= 0`. Si el resultado fuera negativo, arroja `400 Bad Request`. |
| **Postcondiciones** | Cupo disponible actualizado en el turno en vivo con registro histórico de quién lo autorizó y por qué. |
| **Contrato de API** | - `POST /api/v1/catalog/shifts/{shiftId}/menu-items/{itemId}/adjust-quota` → `200 OK` (`ShiftMenuItemDto`) |
| **Entidades Afectadas** | `shift_menu_items`, `shift_menu_quota_adjustments` |

---

## 🔗 3. Relación con Otros Subsistemas

- **Catálogo:** Obtiene los platos y categorías cacheados localmente mediante [use_cases_catalog.md](./use_cases_catalog.md).
- **Inventario:** Desencadena la deducción por receta y cálculo del Costo Promedio Ponderado en [use_cases_inventory.md](./use_cases_inventory.md).
- **Finanzas:** Alimenta el arqueo en tiempo real del turno de caja en [use_cases_finance.md](./use_cases_finance.md).
- **Índice General:** [README](../README.md)
