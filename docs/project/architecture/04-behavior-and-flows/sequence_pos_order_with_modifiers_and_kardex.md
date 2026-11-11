---
title: "Diagrama de Secuencia: Personalización de Comanda, Modificadores y Deducción Atómica en Kardex"
category: "architecture"
level: "Sequence"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - pos
  - modifiers
  - inventory
  - kardex
  - sprint-5
last_updated: "2026-11-07"
status: "draft"
---

# ⚡ Diagrama de Secuencia: Personalización de Comanda, Modificadores y Deducción Atómica en Kardex (PB-020 / TASK-501 / TASK-502)

Este documento modela el flujo end-to-end de personalización de comandas en el Punto de Venta: selección interactiva de extras/modificadores, notas específicas de cocina por ítem, validación de reglas de selección, persistencia en `order_item_modifiers` y deducción atómica desacoplada en el Kardex.

---

## 🧭 1. Resumen del Flujo

1. **Interacción en POS:** El cajero selecciona un plato que posee grupos de modificadores asociados (`dish.modifierGroups`).
2. **Selector Interactivo (`PosComponent`):** Se despliega el modal interactivo de modificadores (`<app-modal>`), permitiendo elegir opciones y escribir notas abiertas de cocina.
3. **Validación Reactiva:** Se comprueban los límites `minSelectable` y `maxSelectable` de cada grupo de modificadores antes de permitir la confirmación.
4. **Agregado al Carrito:** El plato se añade al carrito con precio unitario incrementado por los extras seleccionados:  
   $$\text{unitPrice} = \text{basePrice} + \sum \text{additionalPrice}$$
5. **Persistencia Transaccional:** `OrderServiceImpl` guarda la cabecera en `orders`, los ítems en `order_items` con sus notas de cocina, y cada extra seleccionado en `order_item_modifiers`.
6. **Publicación y Deducción Desacoplada:** `OrderPaidEvent` viaja vía Spring Modulith a `KardexService`. Por cada modificador con `ingredient_id != null` y `quantity_consumed > 0`, se decrementa `branch_stock` de forma atómica y se registra el movimiento de Kardex.

---

## 📊 2. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as "👤 Cajero"
    participant UI as "💻 PosComponent<br/>(Angular Zoneless)"
    participant PosSvc as "⚙️ PosService"
    participant Dexie as "🗄️ Dexie.js (DAL)<br/>(local_orders)"
    participant OrderCtrl as "🌐 OrderController<br/>(/api/v1/pos/orders)"
    participant OrderSvc as "📦 OrderServiceImpl"
    participant OrderRepo as "🗄️ PostgreSQL<br/>(orders, order_items, modifiers)"
    participant Publisher as "📢 Spring EventPublisher"
    participant Kardex as "📦 KardexService<br/>(@ApplicationModuleListener)"

    Cashier->>UI: Clic en plato con extras (ej. Hamburguesa Clásica)
    UI->>UI: Detecta dish.modifierGroups > 0
    UI->>Cashier: Abre modal interactivo de modificadores
    Cashier->>UI: Selecciona "Doble Carne" (+Bs 8), "Sin Cebolla" y nota "Salsa aparte"
    Cashier->>UI: Clic en "Agregar a la Comanda"
    UI->>UI: Valida minSelectable <= selecciones <= maxSelectable
    UI->>PosSvc: addToCart(dish, 1, notes, selectedModifiers)
    PosSvc-->>UI: Carrito reactivo actualizado (total con extras)

    Cashier->>UI: Clic en "Cobrar" y confirma pago en Efectivo
    UI->>PosSvc: checkout(CreateOrderRequest)
    PosSvc->>Dexie: Persiste LocalOrder (idempotente)
    PosSvc->>OrderCtrl: POST /api/v1/pos/orders (con SelectedModifierRequest y notes)
    OrderCtrl->>OrderSvc: placeOrder(request)

    rect rgb(240, 248, 255)
        Note over OrderSvc,OrderRepo: Transacción Principal de Orden
        OrderSvc->>OrderRepo: Inserta orders y order_items (con notes)
        OrderSvc->>OrderRepo: Inserta order_item_modifiers (inmutable)
        OrderSvc->>Publisher: publishEvent(OrderPaidEvent)
    end

    OrderSvc-->>OrderCtrl: OrderDto (con desglose de modificadores y notas)
    OrderCtrl-->>PosSvc: HTTP 201 Created
    PosSvc->>Dexie: Marca orden como SYNCED
    PosSvc-->>UI: Limpia carrito y emite ticket con extras

    rect rgb(255, 250, 240)
        Note over Kardex,OrderRepo: Transacción Asíncrona de Kardex
        Publisher->>Kardex: onOrderPaid(event)
        loop Por cada item y cada modificador
            alt modifier.ingredientId != null AND quantityConsumed > 0
                Kardex->>OrderRepo: Decrementa branch_stock (qty * item.quantity)
                Kardex->>OrderRepo: Inserta kardex_movements (SALE_CONSUMPTION)
            else modifier.ingredientId == null
                Note over Kardex: Extra comercial o exclusión (sin impacto en stock)
            end
        end
    end
```

---

## 📋 3. Contratos de Datos Clave

### A. DTO de Modificador Seleccionado en Venta (`SelectedModifierRequest`)
```json
{
  "modifierId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Doble Carne Smash",
  "additionalPrice": 8.00,
  "ingredientId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "quantityConsumed": 0.1200
}
```

### B. DTO de Ítem de Comanda con Modificadores y Notas (`CreateOrderItemRequest`)
```json
{
  "dishId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "quantity": 2,
  "unitPrice": 43.00,
  "notes": "Salsa golf aparte, bien crujiente",
  "selectedModifiers": [
    {
      "modifierId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Doble Carne Smash",
      "additionalPrice": 8.00,
      "ingredientId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "quantityConsumed": 0.1200
    }
  ]
}
```
