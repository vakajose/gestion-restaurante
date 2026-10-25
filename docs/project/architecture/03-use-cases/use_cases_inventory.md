---
title: "Casos de Uso - Módulo de Inventario y Kardex"
category: "architecture"
level: "Use-Case"
module: "inventory"
tags:
  - architecture
  - mermaid
  - sdd
  - use-cases
  - inventory
  - kardex
  - stock
  - deduction-strategy
last_updated: "2026-10-25"
status: "verified"
---

# 📦 Casos de Uso - Módulo de Inventario y Kardex

El módulo de **Inventario y Kardex** gobierna el control de existencias físicas por sucursal, la trazabilidad granular de movimientos de almacén bajo el método de **Costo Promedio Ponderado (CPP)** y el descuento automatizado de materias primas mediante recetas técnicas (BOM) ante eventos de venta.

---

## 🗺️ 1. Diagrama de Casos de Uso del Módulo Inventario

```mermaid
flowchart LR
    %% Actores
    subgraph Actores["Actores del Módulo Inventario"]
        direction TB
        Act_BM["👤 BRANCH_MANAGER<br/>(Encargado de Sucursal)"]
        Act_AT["👤 ADMIN_TENANT<br/>(Propietario / Gerente General)"]
        Act_SYS["⚙️ InventoryEventListener<br/>(Consumidor de Eventos)"]
    end

    %% Casos de Uso
    subgraph Mod_Inventory["Límites del Módulo Inventario & Kardex (Spring Modulith)"]
        direction TB
        UC_INV_01["UC-INV-01: Registro de Inventario Inicial<br/>(INITIAL_INVENTORY por Sucursal)"]
        UC_INV_02["UC-INV-02: Consulta de Existencias Físicas<br/>(BranchStock y Alertas de Mínimo)"]
        UC_INV_03["UC-INV-03: Deducción Automática de Recetas<br/>(OrderPaidEvent y Strategy Pattern)"]
        UC_INV_04["UC-INV-04: Consulta de Libro Mayor de Kardex<br/>(Trazabilidad CPP y Movimientos)"]
        UC_INV_05["UC-INV-05: Ajustes Manuales de Stock<br/>(Mermas y Conteos Físicos)"]
    end

    %% Estrategias de Deducción
    subgraph Strategies["Patrón de Estrategia de Deducción"]
        direction TB
        Strat_Perm["AUTOMATIC_PERMISSIVE<br/>(Permite saldo negativo con alerta)"]
        Strat_Strict["AUTOMATIC_STRICT<br/>(Valida stock antes de deducir)"]
        Strat_Manual["MANUAL_ISOLATED<br/>(Aislado de ventas POS)"]
    end

    %% Asociaciones de Actores
    Act_BM --> UC_INV_01
    Act_BM --> UC_INV_02
    Act_BM --> UC_INV_04
    Act_BM --> UC_INV_05

    Act_AT --> UC_INV_02
    Act_AT --> UC_INV_04

    Act_SYS --> UC_INV_03

    %% Relaciones entre Casos de Uso
    UC_INV_01 -.->|"<<inicializa>>"| UC_INV_02
    UC_INV_03 -.->|"<<actualiza saldo>>"| UC_INV_02
    UC_INV_03 -.->|"<<registra SALE_OUT>>"| UC_INV_04
    UC_INV_05 -.->|"<<ajusta saldo>>"| UC_INV_02
    UC_INV_05 -.->|"<<registra PHYSICAL_COUNT_ADJUSTMENT>>"| UC_INV_04

    UC_INV_03 --> Strat_Perm
    UC_INV_03 --> Strat_Strict
    UC_INV_03 --> Strat_Manual
```

---

## 📝 2. Especificación Detallada de Casos de Uso

---

### UC-INV-01: Registro de Inventario Inicial por Sucursal

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-INV-01` |
| **Nombre** | Registro de Inventario Inicial por Sucursal (`INITIAL_INVENTORY`) |
| **Actor Principal** | `BRANCH_MANAGER`, `ADMIN_TENANT` |
| **Actores Secundarios** | Ninguno |
| **Objetivo** | Establecer el saldo inicial de apertura y el costo unitario de partida para cada insumo en una sucursal específica, sentando la base para el cálculo del Costo Promedio Ponderado. |
| **Precondiciones** | El insumo debe estar registrado en el Catálogo Maestro (`UC-CAT-02`). La sucursal debe existir y pertenecer al tenant. |
| **Flujo Principal** | 1. El encargado de sucursal accede a la pantalla de inventario en la sección "Carga Inicial".<br/>2. Selecciona el insumo (`ingredientId`), e ingresa la cantidad física disponible (`quantity`) y su costo unitario de adquisición (`unitCost`).<br/>3. El cliente emite `POST /api/v1/inventory/stock/initial?branchId={branchId}` con `InitialStockRequest`.<br/>4. El backend busca o instancia la entidad `BranchStock` para la terna `(tenantId, branchId, ingredientId)`.<br/>5. Actualiza `currentQuantity = request.quantity` y `averageUnitCost = request.unitCost`.<br/>6. Inserta un asiento formal en `kardex_movements` con `movementType = 'INITIAL_INVENTORY'`, `quantity`, `unitCost`, `totalCost = quantity * unitCost` y `balanceQuantity = quantity`.<br/>7. El sistema responde con código HTTP `201 Created`. |
| **Flujos Alternativos / Excepciones** | - **Cantidad o costo negativo:** Rechazado por validación `@Positive` (`400 Bad Request`).<br/>- **Insumo inexistente:** Retorna `404 Not Found`.<br/>- **Sin contexto de sucursal:** Si no se especifica `branchId` en URL ni en JWT, retorna `400 Bad Request`. |
| **Postcondiciones** | La sucursal dispone de existencias iniciales valorizadas en `branch_stocks` y el primer registro de auditoría en `kardex_movements`. |
| **Contrato de API** | - `POST /api/v1/inventory/stock/initial?branchId={branchId}` → `201 Created` (`InitialStockRequest`) |
| **Entidades Afectadas** | `branch_stocks`, `kardex_movements` (`movement_type = 'INITIAL_INVENTORY'`) |

---

### UC-INV-02: Consulta de Existencias Físicas y Valorización de Stock

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-INV-02` |
| **Nombre** | Consulta de Existencias Físicas y Valorización de Stock (`BranchStock`) |
| **Actor Principal** | `BRANCH_MANAGER`, `ADMIN_TENANT` |
| **Actores Secundarios** | Ninguno |
| **Objetivo** | Monitorear el inventario disponible de materias primas por sucursal, identificando insumos críticos por debajo del umbral de alerta (`isLowStockAlert`) y obteniendo la valorización monetaria total del almacén. |
| **Precondiciones** | Usuario autenticado con rol gerencial o administrativo. |
| **Flujo Principal** | 1. El usuario solicita la consulta de existencias en el panel de inventario.<br/>2. El frontend ejecuta `GET /api/v1/inventory/stock?branchId={branchId}`.<br/>3. El backend obtiene todos los insumos activos del tenant (`CatalogPublicApi.getAllIngredients`).<br/>4. Cruza los insumos con las existencias registradas en `branch_stocks` para esa sucursal.<br/>5. Para cada insumo calcula:<br/>   - `currentStock`: Cantidad actual disponible (default 0).<br/>   - `averageUnitCost`: Costo unitario promedio ponderado actual.<br/>   - `totalValue`: `currentStock * averageUnitCost` redondeado a 2 decimales.<br/>   - `minStockAlert`: Umbral configurado en catálogo.<br/>   - `isLowStockAlert`: Booleano `(currentStock <= minStockAlert)`.<br/>6. Retorna `200 OK` con `List<StockBalanceDto>`.<br/>7. La interfaz resalta en color ámbar/rojo los insumos que requieren orden de reposición inmediata. |
| **Flujos Alternativos / Excepciones** | - **Sucursal sin stock previo:** Los insumos sin movimientos se reportan con cantidad 0 y bandera de alerta activada si el umbral mínimo es mayor a cero. |
| **Postcondiciones** | Los encargados disponen de una fotografía exacta del almacén para toma de decisiones y compras. |
| **Contrato de API** | - `GET /api/v1/inventory/stock?branchId={branchId}` → `200 OK` (`List<StockBalanceDto>`) |
| **Entidades Consultadas** | `branch_stocks`, `ingredients` |

---

### UC-INV-03: Deducción Automática de Recetas ante `OrderPaidEvent`

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-INV-03` |
| **Nombre** | Deducción Automática de Recetas ante `OrderPaidEvent` (Strategy Pattern) |
| **Actor Principal** | Sistema Asíncrono (`InventoryEventListener`) |
| **Actores Secundarios** | Spring Modulith Event Bus |
| **Objetivo** | Descargar automáticamente del inventario de la sucursal las cantidades exactas de materias primas consumidas en cada plato vendido, de acuerdo a su ficha técnica (BOM), ejecutando la estrategia configurada por el tenant. |
| **Precondiciones** | La venta ha sido cobrada y confirmada en POS, emitiendo `OrderPaidEvent`. |
| **Flujo Principal** | 1. `InventoryEventListener.on(OrderPaidEvent)` recibe el evento desacoplado vía `@ApplicationModuleListener`.<br/>2. Para cada plato en `event.items()`, consulta su receta en `CatalogPublicApi.getRecipeForDish`.<br/>3. Consolida las cantidades totales requeridas por ingrediente: $\sum (\text{item.quantity} \times \text{recipe.ingredientQuantity})$.<br/>4. Lee la configuración del tenant `INVENTORY_DEDUCTION_MODE` (`AUTOMATIC_PERMISSIVE`, `AUTOMATIC_STRICT`, `MANUAL_ISOLATED`).<br/>5. **Ejecución de la Estrategia:**<br/>   - **`AUTOMATIC_PERMISSIVE` (Default):** Resta la cantidad al saldo de `branch_stocks`. Si el saldo resultante es negativo, lo permite y registra en bitácora `STOCK_DEFICIT_WARNING`. Inserta movimiento `SALE_OUT` en `kardex_movements`.<br/>   - **`AUTOMATIC_STRICT`:** Valida previamente que todos los ingredientes tengan stock suficiente. Si alguno no alcanza, aborta o escala notificación de quiebre de stock.<br/>   - **`MANUAL_ISOLATED`:** Omite la deducción automática en tiempo real, delegando el cuadre a partes manuales de producción en cocina.<br/>6. Se guarda el nuevo saldo y se cierra la transacción modular. |
| **Flujos Alternativos / Excepciones** | - **Platos sin receta BOM:** Se omiten pacíficamente registrando un log informativo.<br/>- **Concurrencia en actualización de saldo:** Controlado por el motor transaccional de Spring. |
| **Postcondiciones** | Las existencias físicas reflejan el consumo de la venta y queda registrada la trazabilidad completa en Kardex con referencia al `orderId`. |
| **Contrato Técnico** | - Evento: `OrderPaidEvent` (`orderId`, `ticketNumber`, `items`, `branchId`, `tenantId`)<br/>- Listener: `InventoryEventListener`<br/>- Modos: `InventoryDeductionMode` |
| **Entidades Afectadas** | `branch_stocks`, `kardex_movements` (`movement_type = 'SALE_OUT'`) |

---

### UC-INV-04: Consulta de Libro Mayor de Kardex y Costo Promedio (CPP)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-INV-04` |
| **Nombre** | Consulta de Libro Mayor de Kardex (Trazabilidad y Costo Promedio Ponderado) |
| **Actor Principal** | `BRANCH_MANAGER`, `ADMIN_TENANT` |
| **Actores Secundarios** | Auditores Contables |
| **Objetivo** | Auditar cronológicamente cada ingreso, egreso o ajuste que ha afectado a un insumo, visualizando la cantidad movida, el costo unitario aplicado, el costo total y el saldo resultante para garantizar transparencia fiscal y contable. |
| **Precondiciones** | Usuario autenticado con privilegios de consulta. |
| **Flujo Principal** | 1. El usuario ingresa a la vista "Movimientos de Kardex".<br/>2. Opcionalmente filtra por insumo (`ingredientId`) o rango de fechas.<br/>3. El cliente invoca `GET /api/v1/inventory/kardex?branchId={branchId}&ingredientId={ingredientId}`.<br/>4. El backend recupera los movimientos ordenados por `movementDate` descendente desde `kardex_movements`.<br/>5. Enriquece cada movimiento con el nombre y unidad de medida oficial del catálogo.<br/>6. Retorna `200 OK` con `List<KardexMovementDto>` conteniendo:<br/>   - `movementType`: `INITIAL_INVENTORY`, `SALE_OUT`, `PHYSICAL_COUNT_ADJUSTMENT`, etc.<br/>   - `quantity`: Cantidad ingresada o deducida.<br/>   - `unitCost`: Costo unitario registrado en el momento de la transacción.<br/>   - `totalCost`: Valor total monetario del movimiento.<br/>   - `balanceQuantity`: Saldo final en almacén tras dicho movimiento.<br/>   - `referenceId`: UUID de la orden o ajuste de origen.<br/>   - `movementDate`: Timestamp UTC.<br/>7. La tabla de auditoría se despliega en pantalla con opción de exportación. |
| **Flujos Alternativos / Excepciones** | - **Insumo sin movimientos:** Retorna lista vacía `[]` con código `200 OK`. |
| **Postcondiciones** | Los administradores disponen de trazabilidad inmutable de todas las mutaciones de stock. |
| **Contrato de API** | - `GET /api/v1/inventory/kardex?branchId={branchId}&ingredientId={ingredientId}` → `200 OK` (`List<KardexMovementDto>`) |
| **Entidades Consultadas** | `kardex_movements`, `ingredients` |

---

### UC-INV-05: Ajustes Manuales de Stock (Mermas / Conteos Físicos)

| Dimensión | Especificación Formal |
| :--- | :--- |
| **Identificador** | `UC-INV-05` |
| **Nombre** | Ajustes Manuales de Stock (Mermas / Conteos Físicos) |
| **Actor Principal** | `BRANCH_MANAGER`, `ADMIN_TENANT` |
| **Actores Secundarios** | Ninguno |
| **Objetivo** | Cuadrar las existencias teóricas del sistema con el recuento físico real realizado en almacén, asentando diferencias por mermas, caducidad, roturas o desvíos operativos. |
| **Precondiciones** | El insumo debe pertenecer a la sucursal y el usuario debe tener rol `BRANCH_MANAGER` o `ADMIN_TENANT`. |
| **Flujo Principal** | 1. El encargado realiza un recuento físico en bodega y detecta una discrepancia.<br/>2. Abre el diálogo "Ajuste de Stock", selecciona el insumo e ingresa el nuevo stock físico real contado (`newQuantity`) y opcionalmente un costo unitario actualizado (`unitCost`).<br/>3. El cliente envía `POST /api/v1/inventory/stock/adjust?branchId={branchId}` con `StockAdjustmentRequest`.<br/>4. El backend calcula la diferencia neta: $\Delta = \text{newQuantity} - \text{previousQuantity}$.<br/>5. Actualiza `currentQuantity` en `branch_stocks`.<br/>6. Inserta un asiento en `kardex_movements` con:<br/>   - `movementType = 'PHYSICAL_COUNT_ADJUSTMENT'`.<br/>   - `quantity = diff` (positiva si hubo sobrante, negativa si hubo merma/faltante).<br/>   - `totalCost = |diff| * unitCost`.<br/>   - `balanceQuantity = newQuantity`.<br/>   - `createdBy = userId` (trazabilidad del usuario responsable del ajuste).<br/>7. Retorna `200 OK` con el `StockBalanceDto` actualizado. |
| **Flujos Alternativos / Excepciones** | - **Nueva cantidad negativa:** La validación rechaza existencias negativas en conteo físico manual (`400 Bad Request`).<br/>- **Usuario no autorizado:** Cajeros intentando invocar este endpoint reciben `403 Forbidden`. |
| **Postcondiciones** | El inventario teórico queda perfectamente sincronizado con el conteo físico y el ajuste queda firmado por el usuario responsable en el libro de Kardex. |
| **Contrato de API** | - `POST /api/v1/inventory/stock/adjust?branchId={branchId}` → `200 OK` (`StockAdjustmentRequest` → `StockBalanceDto`) |
| **Entidades Afectadas** | `branch_stocks`, `kardex_movements` (`movement_type = 'PHYSICAL_COUNT_ADJUSTMENT'`) |

---

## 🔗 3. Relación con Otros Subsistemas

- **Punto de Venta (POS):** Recibe el evento `OrderPaidEvent` originado en [use_cases_pos.md](./use_cases_pos.md).
- **Catálogo:** Consume las recetas BOM y los insumos estructurados en [use_cases_catalog.md](./use_cases_catalog.md).
- **Finanzas:** Los costos valorizados alimentan los balances de costos en [use_cases_finance.md](./use_cases_finance.md).
- **Índice General:** [README](../README.md)
