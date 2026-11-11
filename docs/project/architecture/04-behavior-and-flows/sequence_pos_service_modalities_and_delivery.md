---
title: "Diagrama de Secuencia: Modalidades de Servicio, Delivery Fee y Distintivos en Tickets"
category: "architecture"
level: "Sequence"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - pos
  - delivery
  - takeout
  - ticket
  - sprint-5
last_updated: "2026-11-08"
status: "verified"
---

# 🛵 Diagrama de Secuencia: Modalidades de Servicio, Delivery Fee y Distintivos en Tickets (PB-032 / TASK-505)

Este documento modela el ciclo de vida y la interacción entre el terminal de venta (POS), el almacenamiento local (Dexie.js), la configuración de parámetros del sistema y la persistencia transaccional para órdenes en sus diferentes modalidades: **En Salón (`DINE_IN`)**, **Para Llevar (`TAKEOUT`)**, **Encargo para Recoger (`SCHEDULED_PICKUP`)** y **A Domicilio (`DELIVERY`)**.

---

## 1. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero / Operador
    participant Topbar as PosHeader (Topbar)
    participant Cart as PosCart / State
    participant Checkout as PosCheckoutModal
    participant PosService as PosService (Frontend)
    participant Dexie as Dexie.js (IndexedDB)
    participant Api as OrderController (Backend)
    participant OrderSvc as OrderServiceImpl
    participant Config as CascadingParameterResolver
    participant DB as PostgreSQL (orders)

    %% Paso 1: Preselección en Header
    Note over Cajero, Topbar: Fase 1: Preselección Rápida de Modalidad
    Cajero->>Topbar: Selecciona modalidad (ej. "Delivery" 🛵)
    Topbar->>Cart: selectedOrderType.set('DELIVERY')

    %% Paso 2: Selección de Platos
    Cajero->>Cart: Agrega platos y extras al carrito
    Cart->>Cart: Actualiza subtotal de platos

    %% Paso 3: Pasarela de Cobro
    Note over Cajero, Checkout: Fase 2: Checkout Contextual y Confirmación
    Cajero->>Checkout: Clic en "Cobrar Pedido"
    Checkout->>Config: Consulta pos.delivery_fee_enabled y pos.delivery_fee_default
    Config-->>Checkout: feeEnabled = true, defaultFee = 15.00
    Checkout->>Checkout: Despliega formulario contextual (Teléfono, Dirección, Fee)
    Cajero->>Checkout: Ingresa Teléfono ("71234567"), Dirección ("Av. Ballivián 456") y Notas
    Checkout->>Checkout: Total = Subtotal + DeliveryFee

    %% Paso 4: Confirmación y Persistencia Offline-First
    Note over Cajero, Dexie: Fase 3: Persistencia Local y Emisión
    Cajero->>Checkout: Clic en "Confirmar y Emitir Ticket"
    Checkout->>PosService: checkout(orderPayload)
    PosService->>Dexie: savePendingOrder(orderWithDeliveryData)
    Dexie-->>PosService: ok (LOCAL-TKT-XXXX)

    %% Paso 5: Sincronización Backend
    PosService->>Api: POST /api/v1/pos/orders (CreateOrderRequest)
    Api->>OrderSvc: createOrder(request)
    OrderSvc->>OrderSvc: Validar delivery_address presente y delivery_fee >= 0
    OrderSvc->>OrderSvc: Calcular totalAmount = subtotal + deliveryFee
    OrderSvc->>DB: INSERT INTO orders (order_type, customer_phone, delivery_address, delivery_fee, ...)
    DB-->>OrderSvc: Order persistida
    OrderSvc-->>Api: OrderDto (ticketNumber: "TKT-20261108-0042")
    Api-->>PosService: 201 Created (OrderDto)
    PosService->>Dexie: updatePendingOrderStatus(SYNCED)

    %% Paso 6: Renderizado de Ticket Térmico
    Note over Cajero, Checkout: Fase 4: Despliegue de Comanda con Distintivos
    PosService-->>Checkout: Despliega TicketModal
    Checkout->>Cajero: Muestra Ticket con banner "*** DELIVERY / DOMICILIO ***", Dirección y Fee
```

---

## 2. Invariantes y Reglas del Flujo

1. **Selector Dual:** La modalidad puede preseleccionarse en el topbar o modificarse en el modal de checkout antes de emitir el cobro.
2. **Validación de Dirección:** Si la modalidad es `DELIVERY`, el campo `deliveryAddress` no puede ser vacío.
3. **Determinismo Contable:** Si `deliveryFee > 0`, dicho monto incrementa el `totalAmount` transaccional de la comanda y debe cuadrar exactamente con el medio de pago seleccionado (`CASH`, `CARD`, `QR`, `TRANSFER`).
4. **Resiliencia Offline:** En caso de corte de red, Dexie.js almacena los atributos de delivery (`customerName`, `customerPhone`, `deliveryAddress`, `deliveryFee`, `deliveryNotes`) en `pendingOrders`, permitiendo imprimir el ticket local con sus distintivos y sincronizar al restablecerse la conectividad.
