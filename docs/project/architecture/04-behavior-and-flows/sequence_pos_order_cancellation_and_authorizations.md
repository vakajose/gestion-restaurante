---
title: "Diagrama de Secuencia: Flujo de Cancelación de Pedidos, Autorización por PIN y Tratamiento de Kardex"
category: "architecture"
level: "Sequence"
module: "pos"
tags:
  - architecture
  - mermaid
  - sdd
  - sequence
  - pos
  - cancellation
  - authorizations
  - pin
  - kardex
  - finance
last_updated: "2026-11-07"
status: "draft"
---

# ⚡ Diagrama de Secuencia: Flujo de Cancelación de Pedidos, Autorización por PIN y Tratamiento de Kardex (PB-031 / TASK-503 / TASK-504)

Este documento modela el flujo integral de anulación de comandas pagadas en el Punto de Venta: captura del motivo y canal de devolución, canal dual de autorización (PIN rápido in-situ vs bandeja asíncrona remota), afectación a gaveta física de caja y resolución desacoplada en Kardex (reintegro, merma o buffer de vitrina).

---

## 🧭 1. Resumen Ejecutivo del Flujo

1. **Iniciación en Terminal:** El cajero solicita la cancelación de una orden pagada desde `/pos` o `/cash-control`.
2. **Diálogo de Cancelación:** El cajero especifica el motivo tipado (`CUSTOMER_RETRACTED`, `TYPING_ERROR`, etc.), el método de devolución (`CASH` o `DIGITAL`) y si el plato ya fue elaborado en cocina (`wasPrepared`).
3. **Autorización (Canal Dual):**
   - **Canal In-Situ:** El supervisor ingresa su PIN numérico de 4 a 6 dígitos directamente en la pantalla del cajero. Se valida la credencial y se ejecuta la anulación en tiempo real.
   - **Canal Remoto:** Se crea una solicitud `ShiftAuthorization` en estado `PENDING_APPROVAL`. El supervisor la aprueba remotamente desde `/finance`.
4. **Persistencia y Finanzas:**
   - La orden pasa a estado `CANCELLED`.
   - Si `refundMethod == 'CASH'`, se registra un egreso `REFUND` en el turno activo (`cash_shifts`), reduciendo el saldo esperado en gaveta para cuadre exacto.
5. **Deducción y Kardex Desacoplado:** Spring Modulith despacha `OrderCancelledEvent`:
   - Si `wasPrepared == false` $\rightarrow$ `CANCELLATION_REVERSAL` devuelve insumos a `branch_stock`.
   - Si `wasPrepared == true` $\rightarrow$ Evalúa `pos.cancellation_prepared_waste_policy`:
     - `ALLOW_AUTO_BUFFER_REUSE`: Transfiere a `prepared_dish_buffer` (sin merma).
     - `STRICT_WASTE`: Registra `WASTE_ADJUSTMENT` en `kardex_movements` (pérdida económica).

---

## 📊 2. Diagrama de Secuencia Mermaid

```mermaid
sequenceDiagram
    autonumber
    actor Cashier as "👤 Cajero"
    actor Supervisor as "👮 Supervisor / Admin"
    participant UI as "💻 PosComponent / CashControl<br/>(Angular Zoneless)"
    participant AuthQueue as "🧩 <app-authorization-queue>"
    participant OrderCtrl as "🌐 OrderController / FinanceController"
    participant OrderSvc as "📦 OrderServiceImpl"
    participant AuthSvc as "🛡️ ShiftAuthorizationService"
    participant CashShiftSvc as "💵 CashShiftService"
    participant DB as "🗄️ PostgreSQL"
    participant Modulith as "📢 Spring Modulith EventPublisher"
    participant Kardex as "📦 KardexService (@ApplicationModuleListener)"

    Cashier->>UI: Solicita cancelar Orden #T-100X
    UI->>Cashier: Muestra diálogo de motivo, vía de reintegro (CASH/DIGITAL) y estado cocina

    alt Canal Rápido In-Situ (PIN Supervisor)
        Supervisor->>UI: Digita PIN numérico (4-6 dígitos) en modal táctil
        UI->>OrderCtrl: POST /api/v1/pos/orders/{id}/cancel (con supervisorPin, motivo, refundMethod, wasPrepared)
        OrderCtrl->>AuthSvc: validateSupervisorPin(pin, branchId)
        AuthSvc->>DB: Verifica BCrypt hash en app_users.pin_hash
        AuthSvc-->>OrderCtrl: Autorización Aprobada (supervisorId)
    else Canal Asíncrono Remoto (Bandeja en Cola)
        Cashier->>UI: Envía a cola de aprobación
        UI->>OrderCtrl: POST /api/v1/finance/authorizations (type: ORDER_CANCELLATION, status: PENDING)
        Supervisor->>AuthQueue: Ve solicitud en /finance y pulsa [Aprobar]
        AuthQueue->>OrderCtrl: PUT /api/v1/finance/authorizations/{id}/approve
    end

    rect rgb(240, 248, 255)
        Note over OrderSvc,DB: Transacción de Anulación de Orden
        OrderCtrl->>OrderSvc: cancelOrder(...)
        OrderSvc->>DB: Actualiza orders (status: CANCELLED, cancelled_by, authorized_by)
        alt refundMethod == CASH
            OrderSvc->>CashShiftSvc: registerRefundEgress(shiftId, amount)
            CashShiftSvc->>DB: Incrementa cash_shifts.total_cash_refunds
        end
        OrderSvc->>Modulith: publishEvent(OrderCancelledEvent)
    end

    OrderSvc-->>UI: HTTP 200 OK (Orden cancelada y comprobante de devolución)

    rect rgb(255, 250, 240)
        Note over Kardex,DB: Tratamiento Asíncrono en Kardex
        Modulith->>Kardex: onOrderCancelled(event)
        alt wasPrepared == false
            Kardex->>DB: Movimiento CANCELLATION_REVERSAL (reintegra stock en branch_stock)
        else wasPrepared == true
            alt policy == ALLOW_AUTO_BUFFER_REUSE
                Kardex->>DB: Inserta en prepared_dish_buffer (reutilización sin merma)
            else policy == STRICT_WASTE
                Kardex->>DB: Movimiento WASTE_ADJUSTMENT en kardex_movements (pérdida económica)
            end
        end
    end
```
