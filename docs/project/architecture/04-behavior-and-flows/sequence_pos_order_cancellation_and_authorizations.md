---
title: "Flujo de Anulación de Pedidos, Autorizaciones Dual-Control y Rescate/Merma"
category: "behavior-flow"
module: "pos"
tags: ["sequence", "pos", "finance", "cancellation", "pin", "authorizations", "kardex", "buffer", "waste"]
last_updated: "2026-11-17"
status: "verified"
---

# Flujo de Anulación de Pedidos, Autorizaciones Dual-Control y Rescate/Merma

Secuencia unificada para anular un pedido en estado `PAID` cumpliendo el control dual de supervisión (PIN presencial in-situ o aprobación remota en sesión), registro de devolución de efectivo en caja y resolución de inventario según si el plato fue preparado (`PreparedDishBuffer` / `WASTE_ADJUSTMENT`) o revertido al stock (`CANCELLATION_REVERSAL`).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero (POS)
    actor Supervisor as Supervisor / Gerente
    participant PosUI as PosComponent (Modal Cancelación)
    participant AuthCtrl as AuthorizationController (/api/v1/finance/authorizations)
    participant OrderCtrl as OrderController (/api/v1/pos/orders)
    participant OrderSvc as OrderServiceImpl
    participant FinApi as FinancePublicApi (ShiftAuthorizationService / CashShiftService)
    participant ConfigApi as ConfigurationPublicApi
    participant Outbox as Spring Modulith Outbox
    participant InvListener as InventoryEventListener + KardexService

    Cajero->>PosUI: Solicitar anulación (motivo, notas, refundMethod, wasPrepared)

    alt Canal A: Autorización Presencial con PIN Supervisor
        Supervisor->>PosUI: Ingresar PIN numérico de 4-6 dígitos
        PosUI->>OrderCtrl: POST /api/v1/pos/orders/{orderId}/cancel (supervisorPin)
        OrderCtrl->>OrderSvc: cancelOrder(tenantId, orderId, request, userId)
        OrderSvc->>FinApi: validateSupervisorPin(tenantId, branchId, pin)
        FinApi-->>OrderSvc: ValidatePinResponse(valid: true, supervisorId)
    else Canal B: Solicitud de Autorización Remota
        PosUI->>AuthCtrl: POST /api/v1/finance/authorizations (requestType: ORDER_CANCELLATION, referenceId: orderId)
        AuthCtrl-->>PosUI: HTTP 201 Created (ShiftAuthorization status: PENDING)
        Supervisor->>AuthCtrl: POST o PUT /api/v1/finance/authorizations/{id}/approve
        AuthCtrl-->>Supervisor: ShiftAuthorization status: APPROVED (decisionChannel: REMOTE_SESSION)
        PosUI->>OrderCtrl: POST /api/v1/pos/orders/{orderId}/cancel (authorizationRequestId)
        OrderCtrl->>OrderSvc: cancelOrder(tenantId, orderId, request, userId)
        OrderSvc->>FinApi: getAuthorization(tenantId, authorizationRequestId)
        FinApi-->>OrderSvc: ShiftAuthorizationDto (status: APPROVED, decidedBy)
    end

    %% Validación de estado y política de merma
    OrderSvc->>OrderSvc: Verificar order.orderStatus == PAID
    alt wasPrepared == false
        OrderSvc->>OrderSvc: wastePolicy = CANCELLATION_REVERSAL
    else wasPrepared == true
        OrderSvc->>ConfigApi: getString(POS_CANCELLATION_PREPARED_WASTE_POLICY, branchId, tenantId)
        ConfigApi-->>OrderSvc: ALLOW_AUTO_BUFFER_REUSE (u otra política configurada)
    end

    %% Reintegro Financiero
    opt refundMethod == CASH
        OrderSvc->>FinApi: recordRefund(tenantId, branchId, cashShiftId, totalAmount)
        FinApi->>FinApi: Sumar a cash_shifts.total_cash_refunds
    end

    %% Persistencia y Evento
    OrderSvc->>OrderSvc: order.cancel(...) -> orderStatus = CANCELLED
    OrderSvc->>Outbox: publishEvent(OrderCancelledEvent)
    OrderSvc-->>OrderCtrl: OrderDto actualizado
    OrderCtrl-->>PosUI: HTTP 200 OK

    %% Impacto Asíncrono en Inventario
    Outbox-)InvListener: OrderCancelledEvent (wasPrepared, wastePolicy, items)
    InvListener->>InvListener: KardexService.processOrderCancellation(event)
    alt wasPrepared == false (CANCELLATION_REVERSAL)
        InvListener->>InvListener: Reintegrar stock en branch_stock y registrar CANCELLATION_REVERSAL
    else wasPrepared == true y ALLOW_AUTO_BUFFER_REUSE
        InvListener->>InvListener: Insertar unidades rescatables en prepared_dish_buffer
    else wasPrepared == true sin reuso en buffer
        InvListener->>InvListener: Registrar movimiento de pérdida WASTE_ADJUSTMENT en kardex_movements
    end
```

## Validaciones y Reglas de Negocio Verificadas

1. **Precondición de Estado:** Solo órdenes en estado `PAID` admiten cancelación (`OrderServiceImpl.cancelOrder`).
2. **Motivos de Anulación Tipados (`V12`):** `CUSTOMER_RETRACTED`, `TYPING_ERROR`, `KITCHEN_ISSUE`, `QUALITY_COMPLAINT`, `DUPLICATE_ORDER`.
3. **Validación Dual-Control (`AuthorizationController`):**
   - El canal presencial valida el hash BCrypt (`app_users.pin_hash`) de un supervisor autorizado para la sucursal (`POST /api/v1/finance/authorizations/validate-pin` o directamente en `cancelOrder`).
   - El canal remoto registra un `ShiftAuthorization` (`PENDING`) y exige aprobación explícita mediante `POST` o `PUT` sobre `/api/v1/finance/authorizations/{id}/approve` (requiere privilegio `finance:shifts_supervisor`, `finance:expenses_approve` o rol gerencial).
4. **Impacto en Arqueo de Caja (`V12`):** Únicamente los reintegros con `refundMethod == "CASH"` incrementan `cash_shifts.total_cash_refunds`, reduciendo el efectivo esperado (`expected_cash`).

## Enlaces Relacionados

- [Ciclo de Vida de la Orden](./state_order_lifecycle.md)
- [Flujo de Turno de Caja y Gastos](./state_shift_and_expense_workflow.md)
- [Resolución RBAC y Autorizaciones](./sequence_rbac_resolution_and_enforcement.md)
- [Casos de Uso POS](../03-use-cases/use_cases_pos.md)
- [Casos de Uso Finanzas](../03-use-cases/use_cases_finance.md)
- [ADR-0019: Flujo de Cancelación de Pedidos, Autorizaciones y Merma](../../../adr/0019_order_cancellation_workflow_authorizations_and_waste_treatment.md)
