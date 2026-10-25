---
title: "Diagrama de Actividad: Strategy Pattern de Deducción de Inventario y Costeo Kardex"
category: "architecture"
level: "Activity"
module: "inventory"
tags:
  - architecture
  - mermaid
  - sdd
  - activity
  - inventory
  - strategy-pattern
  - kardex
  - recipes-bom
last_updated: "2026-10-25"
status: "verified"
---

# 📦 Diagrama de Actividad: Strategy Pattern de Deducción de Inventario y Costeo Kardex

Este documento modela el flujo de actividad del procesamiento de deducciones de insumos y registro en el **Kardex** derivado del evento de venta `OrderPaidEvent`. Detalla la resolución de fichas técnicas de platos (**Bill of Materials - BOM**), la agregación de cantidades requeridas, la aplicación dinámica del patrón **Strategy** según la configuración de la empresa (`INVENTORY_DEDUCTION_MODE`) y el asentamiento inmutable de movimientos de almacén.

---

## 🧭 1. Resumen Ejecutivo del Algoritmo

1. **Evento Desencadenante:** `InventoryEventListener` recibe el evento de dominio `OrderPaidEvent` despachado por el módulo POS.
2. **Resolución y Agregación de Recetas (BOM):** Para cada plato (`dishId`) de la comanda, se consulta su receta mediante `CatalogPublicApi`. Si el pedido incluye múltiples platos que comparten insumos (por ejemplo, tres platos diferentes que usan cebolla), el sistema consolida las cantidades brutas requeridas en un único mapa agregado `neededQuantities: Map<UUID, BigDecimal>`.
3. **Validación de Aplicabilidad:** Si la comanda no contiene platos con receta configurada (por ejemplo, productos revendidos directos sin despiece), el flujo finaliza inmediatamente sin consumir recursos.
4. **Selección Polimórfica de Estrategia:** Se consulta la configuración `INVENTORY_DEDUCTION_MODE` a través de `TenantConfigService`:
   - **`MANUAL_ISOLATED`:** Modo aislado. No descuenta stock automáticamente. Diseñado para restaurantes tipo buffet, eventos o modelos donde el inventario se cuadra mediante conteos físicos periódicos.
   - **`AUTOMATIC_PERMISSIVE` (Predeterminado):** Modo permisivo para gastronomía ágil. Descuenta los insumos inmediatamente. Si las existencias físicas no alcanzan, permite saldos negativos temporales y genera una alerta de auditoría (`STOCK_DEFICIT_WARNING`), garantizando que la operación comercial nunca se detenga.
   - **`AUTOMATIC_STRICT`:** Modo estricto para alta precisión. Valida previamente la disponibilidad de todos los insumos. Si alguno no cuenta con stock suficiente, registra una advertencia estricta de déficit antes de aplicar la deducción.
5. **Persistencia Inmutable en Kardex:** Por cada insumo descontado, se actualiza el saldo actual en `branch_stock` y se inserta un registro histórico inmutable en `kardex_movements` con tipo `SALE_OUT`, valorizado al **Costo Promedio Ponderado (CPP)** vigente.

---

## 📊 2. Diagrama de Actividad Mermaid

```mermaid
flowchart TD
    Start(["Inicio: Recepción de OrderPaidEvent"]) --> ExtractItems["1. Extraer ítems de la orden<br/>(event.items)"]
    
    ExtractItems --> LoopItems["2. Iterar por cada ítem del pedido"]
    LoopItems --> FetchRecipe["Consultar receta a CatalogPublicApi<br/>(getRecipeForDish)"]
    
    FetchRecipe --> HasRecipe{"¿Tiene receta<br/>asociada?"}
    HasRecipe -->|No| NextItem["Siguiente ítem"]
    HasRecipe -->|Sí| Accumulate["Multiplicar: dish.quantity × ingredient.quantity<br/>Acumular en mapa neededQuantities[ingredientId]"]
    
    Accumulate --> NextItem
    NextItem --> MoreItems{"¿Más ítems<br/>en orden?"}
    MoreItems -->|Sí| LoopItems
    MoreItems -->|No| CheckNeeded{"¿neededQuantities<br/>está vacío?"}

    CheckNeeded -->|Sí| NoDeduction["Log: 'Orden no requiere deducción de insumos'<br/>(Sin recetas configuradas)"]
    NoDeduction --> Terminate(["Fin de Actividad"])

    CheckNeeded -->|No| ReadConfig["3. Leer configuración del Tenant:<br/>INVENTORY_DEDUCTION_MODE<br/>(TenantConfigService)"]

    ReadConfig --> BranchStrategy{"Evaluar Modo<br/>de Deducción"}

    %% RAMA 1: MANUAL_ISOLATED
    BranchStrategy -->|MANUAL_ISOLATED| StratManual["<b>Estrategia Manual / Aislada</b><br/>(ManualIsolatedStrategy)"]
    StratManual --> LogManual["Log: 'Estrategia MANUAL_ISOLATED activa.<br/>Se omite descuento automático'"]
    LogManual --> Terminate

    %% RAMA 2: AUTOMATIC_PERMISSIVE
    BranchStrategy -->|AUTOMATIC_PERMISSIVE<br/>(Default)| StratPermissive["<b>Estrategia Automática Permisiva</b><br/>(AutomaticPermissiveStrategy)"]
    
    StratPermissive --> LoopPermissive["Iterar cada insumo en deductionItems"]
    LoopPermissive --> GetStockPerm["Consultar BranchStock en BD<br/>(branchStockRepository)"]
    GetStockPerm --> StockExistsPerm{"¿Existe registro<br/>en sucursal?"}
    StockExistsPerm -->|No| CreateZeroStock["Crear BranchStock inicial con saldo 0.00"]
    StockExistsPerm -->|Sí| CalcNewBalance["Calcular nuevo saldo:<br/><b>newBalance = currentQuantity - neededQuantity</b>"]
    CreateZeroStock --> CalcNewBalance
    
    CalcNewBalance --> SaveStockPerm["Actualizar y guardar BranchStock<br/>(stock.currentQuantity = newBalance)"]
    
    SaveStockPerm --> CheckNegative{"¿newBalance < 0?"}
    CheckNegative -->|Sí| LogDeficitPerm["⚠️ Log warn: STOCK_DEFICIT_WARNING<br/>(Insumo quedó en saldo negativo tras ticket)"]
    CheckNegative -->|No| CalcCostPerm["Obtener unitCost = stock.averageUnitCost<br/>totalCost = neededQuantity × unitCost"]
    LogDeficitPerm --> CalcCostPerm

    CalcCostPerm --> InsertKardexPerm["Insertar movimiento inmutable en Kardex:<br/>- movement_type = 'SALE_OUT'<br/>- quantity = neededQuantity<br/>- unit_cost = unitCost, total_cost = totalCost<br/>- balance_after = newBalance<br/>- reference_id = orderId, created_at = paidAt"]
    
    InsertKardexPerm --> MorePermissive{"¿Más insumos?"}
    MorePermissive -->|Sí| LoopPermissive
    MorePermissive -->|No| DonePermissive["Deducción permisiva finalizada exitosamente"]
    DonePermissive --> Terminate

    %% RAMA 3: AUTOMATIC_STRICT
    BranchStrategy -->|AUTOMATIC_STRICT| StratStrict["<b>Estrategia Automática Estricta</b><br/>(AutomaticStrictStrategy)"]
    
    StratStrict --> PreCheckLoop["Fase 1: Pre-verificación de disponibilidad"]
    PreCheckLoop --> CheckAvail{"¿currentQuantity >=<br/>neededQuantity?"}
    CheckAvail -->|No| WarnStrict["⚠️ Log warn: STOCK_DEFICIT_WARNING<br/>(Déficit detectado en modo estricto)"]
    CheckAvail -->|Sí| NextPreCheck["Siguiente insumo"]
    WarnStrict --> NextPreCheck
    
    NextPreCheck --> MorePreCheck{"¿Más insumos<br/>por validar?"}
    MorePreCheck -->|Sí| PreCheckLoop
    MorePreCheck -->|No| ApplyDeductionStrict["Fase 2: Aplicar deducción y registro en Kardex"]
    
    ApplyDeductionStrict --> LoopStrict["Iterar y descontar cada insumo"]
    LoopStrict --> DeductAndSaveStrict["Actualizar branch_stock y registrar KardexMovement<br/>(movement_type = 'SALE_OUT')"]
    DeductAndSaveStrict --> MoreStrict{"¿Más insumos?"}
    MoreStrict -->|Sí| LoopStrict
    MoreStrict -->|No| DoneStrict["Deducción estricta completada"]
    DoneStrict --> Terminate
```

---

## 🧩 3. Detalle de las Estrategias de Deducción

```text
+------------------------+---------------------------------+---------------------------------+---------------------------------+
| Dimensión / Criterio   | MANUAL_ISOLATED                 | AUTOMATIC_PERMISSIVE (Default)  | AUTOMATIC_STRICT                |
+------------------------+---------------------------------+---------------------------------+---------------------------------+
| **Filosofía Operativa**| Desacoplamiento total de ventas | Continuidad del negocio primero | Control y auditoría rigurosa    |
| **Deducción de Stock** | No descuenta de forma automática| Descuenta inmediatamente        | Descuenta tras verificación     |
| **Saldos Negativos**   | No aplica                       | Permitidos (con alerta)         | Genera advertencia de déficit   |
| **Movimiento Kardex**  | Ninguno (solo inventario físico)| Registra SALE_OUT obligatorio   | Registra SALE_OUT obligatorio   |
| **Riesgo Operativo**   | Cero fricción; stock desfasado  | Cero bloqueo en caja; auditable | Requiere inventario al día      |
| **Caso de Uso Típico** | Buffets, eventos, food trucks   | Restaurantes comerciales, bares | Franquicias, insumos de alto costo|
+------------------------+---------------------------------+---------------------------------+---------------------------------+
```

---

## 📈 4. Modelo de Costeo y Valorización de Inventario

### 4.1. Principio del Costo Promedio Ponderado (CPP)
El sistema valoriza las existencias bajo el método de **Costo Promedio Ponderado** (Weighted Average Cost). El costo unitario de un insumo se recalcula exclusivamente durante los movimientos de **entrada de stock** (compras a proveedores o inventario inicial):

$$\text{Nuevo Costo Promedio} = \frac{(\text{Stock Actual} \times \text{Costo Actual}) + (\text{Cantidad Entrante} \times \text{Costo de Compra})}{\text{Stock Actual} + \text{Cantidad Entrante}}$$

### 4.2. Costo Unitario en la Salida por Venta (`SALE_OUT`)
Al momento de producirse una deducción por venta POS:
- El movimiento de salida **no altera el costo promedio unitario del insumo**.
- El insumo se descarga de almacén valorizado al **Costo Promedio Ponderado vigente** en ese instante exacto (`stock.getAverageUnitCost()`).
- El importe total deducido se calcula como:
  $$\text{total\_cost} = \text{needed\_quantity} \times \text{average\_unit\_cost}$$
- Este valor alimenta directamente los reportes de **Costo de Mercancía Vendida (Cost of Goods Sold - COGS)** para el cálculo de la rentabilidad real de cada plato.

---

## 🗄️ 5. Invariantes de la Tabla `kardex_movements`

La tabla relacional `kardex_movements` es un libro contable digital con las siguientes reglas inviolables:
1. **Inmutabilidad Absoluta (Append-Only):** Queda estrictamente prohibido ejecutar sentencias `UPDATE` o `DELETE` sobre las filas de `kardex_movements`.
2. **Trazabilidad Transaccional:** Cada registro de tipo `SALE_OUT` guarda la referencia unívoca a la orden de venta:
   ```java
   KardexMovement movement = new KardexMovement(
       tenantId,
       branchId,
       item.ingredientId(),
       "SALE_OUT",
       item.totalQuantityNeeded(),
       unitCost,
       totalCost,
       newBalance,
       event.orderId(),
       event.paidAt(),
       null
   );
   ```
3. **Auditoría de Saldos:** Cada movimiento almacena el saldo resultante (`balance_after = newBalance`), permitiendo reconstruir la curva histórica de inventario de cualquier insumo en cualquier punto del tiempo.

---

## 🔗 Enlaces Cruzados a Bóveda y Documentación

- **Diagrama de Secuencia POS a Kardex:** [sequence_pos_order_deduction.md](./sequence_pos_order_deduction.md)
- **Ciclo de Vida de Comandas:** [state_order_lifecycle.md](./state_order_lifecycle.md)
- **Casos de Uso de Inventario:** [use_cases_inventory.md](../03-use-cases/use_cases_inventory.md)
- **Casos de Uso de Catálogo y Recetas (BOM):** [use_cases_catalog.md](../03-use-cases/use_cases_catalog.md)
- **ERD PostgreSQL (Tablas `branch_stock` y `kardex_movements`):** [database_erd.md](../02-domain-and-data/database_erd.md)
- **ADR-0004: Arquitectura Basada en Eventos:** [docs/adr/0004-event-driven-architecture.md](../../../adr/)
