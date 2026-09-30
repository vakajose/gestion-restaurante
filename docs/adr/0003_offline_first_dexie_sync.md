# ADR 0003: Estrategia Offline-First en POS con Dexie.js e Idempotencia

## Contexto
En un punto de venta gastronómico (POS), la atención al cliente no puede detenerse bajo ninguna circunstancia por fluctuaciones en la conexión a internet o fallas momentáneas de red local. Los cajeros deben poder cobrar y emitir comandas de forma ininterrumpida. Al mismo tiempo, la reconexión automática posterior no debe duplicar ventas ni descuadrar los cobros financieros.

## Decisión
Se implementa una arquitectura **Offline-First** en el cliente web mediante **Angular 22** y **Dexie.js** (IndexedDB).

### Protocolo de Operación:
1. El catálogo de productos y precios se cachea localmente en IndexedDB.
2. Toda orden creada sin conexión se persiste de inmediato en la tabla local `pendingOrders` con un UUID único generado por el cliente (`client_transaction_id`).
3. El servicio `SyncManagerService` detecta el retorno de la conexión y sincroniza los pedidos mediante peticiones HTTP idempotentes enviando la cabecera `X-Client-Transaction-Id`.
4. El backend impone una restricción única `UNIQUE(client_transaction_id)` en la tabla `orders`. Si la orden ya había ingresado por un reintento anterior, responde `409 Conflict` o `200 OK` confirmado, y el cliente actualiza el estado a `SYNCED` sin generar cobros dobles.
5. Ante déficit de stock en sincronizaciones offline con modo estricto, la venta financiera se registra de forma prioritaria (ya ocurrió físicamente) y se levanta una alerta `STOCK_DEFICIT_WARNING` en auditoría para conciliación administrativa.

## Consecuencias
- **Positivas:** Continuidad operativa total en el restaurante; cero pérdida de ventas; prevención absoluta de duplicidades.
- **Negativas:** La interfaz debe gestionar estados visuales de sincronización y almacenamiento local en el navegador del terminal POS.
