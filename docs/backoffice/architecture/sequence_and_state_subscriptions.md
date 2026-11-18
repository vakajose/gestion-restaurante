---
title: "Estados de Suscripción y Secuencias Event-Driven"
category: "architecture"
level: "Sequence & State"
module: "backoffice"
tags:
  - architecture
  - mermaid
  - sequence
  - state
  - event-driven
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# Estados de Suscripción y Secuencias Event-Driven

> ⬅️ [Volver al Índice Backoffice](../README.md) | [ADR-0029](../../adr/0029_backoffice_subscriptions_quotas_and_telemetry.md)

## 1. Máquina de Estados de Suscripción y Modos de Bloqueo

```mermaid
stateDiagram-v2
    [*] --> TRIAL: Onboarding / Alta Inicial (TRIAL_14D)
    TRIAL --> ACTIVE: Asigna Plan (Pagado o Pendiente)
    ACTIVE --> GRACE_PERIOD: Vence Periodo & Concede Gracia Condicional
    GRACE_PERIOD --> ACTIVE: Registra Pago o Asigna Nuevo Plan
    ACTIVE --> SUSPENDED: Suspende Manual o Vence sin Gracia
    GRACE_PERIOD --> SUSPENDED: Expira graceUntil o graceOrdersUsed >= graceMaxOrders
    SUSPENDED --> ACTIVE: Reactiva o Asigna Plan Habilitante
    ACTIVE --> CANCELLED: Baja Definitiva
    SUSPENDED --> CANCELLED: Baja Definitiva

    note right of SUSPENDED
        Dos Modos Configurables:
        1) POS_AND_SHIFT_ONLY (Login OK, Caja y POS 403)
        2) TOTAL (Login, Refresh y ERP 403 + Revoca Sesiones)
    end note
```

---

## 2. Secuencia A — Venta POS, Proyección de Telemetría y Consumo de Gracia

```mermaid
sequenceDiagram
    actor Cajero
    participant POS as "modules.pos"
    participant Bus as "Spring ApplicationEventPublisher"
    participant Tel as "BackofficeTelemetryListener"
    participant Cache as "core.entitlements (TenantEntitlementService)"

    Cajero->>POS: Pagar comanda (POST /api/v1/pos/orders)
    POS->>Cache: assertPosAndShiftOperationsAllowed(tenantId)
    Cache-->>POS: OK (ACTIVE, TRIAL o GRACE_PERIOD vigente)
    POS->>Bus: Publica OrderPaidEvent (tenantId, branchId, totalAmount, paidAt)
    POS-->>Cajero: 201 Created / 200 OK

    Bus->>Tel: @EventListener onOrderPaidGraceTracking (síncrono ACID)
    alt Tenant en GRACE_PERIOD
        Tel->>Tel: Incrementa grace_orders_used
        opt grace_orders_used >= grace_max_orders
            Tel->>Tel: Transiciona a SUSPENDED (autoSuspendModeOnExpire)
            Tel->>Bus: Publica TenantEntitlementChangedEvent
            Bus-->>Cache: Evict/Reload Snapshot O(1) del Tenant
        end
    end

    Bus-->>Tel: @ApplicationModuleListener on(OrderPaidEvent) (outbox asíncrono)
    Tel->>Tel: Upsert tenant_daily_telemetry (fecha local sucursal ADR-0006)
```

---

## 3. Secuencia B — Asignación de Plan a Crédito, Suspensión y Reseteo de Caché

```mermaid
sequenceDiagram
    actor Admin as "SuperAdmin (React Backoffice)"
    participant Ctrl as "TenantSubscriptionController"
    participant Svc as "TenantSubscriptionService"
    participant Sec as "SessionPublicApi (core.security)"
    participant Bus as "ApplicationEventPublisher"
    participant Cache as "TenantEntitlementService"

    Admin->>Ctrl: POST /api/v1/backoffice/tenants/{id}/subscription/assign-plan (markAsPaid=false)
    Ctrl->>Svc: assignPlan(...)
    Svc->>Svc: Crea cargo PENDING en subscription_charges
    Svc->>Svc: Extiende current_period_end, status=ACTIVE, suspension_mode=NONE
    Svc->>Svc: Registra evento en platform_audit_logs
    Svc->>Bus: Publica TenantEntitlementChangedEvent(tenantId)
    Bus-->>Cache: Evict/Reload Snapshot O(1)
    Ctrl-->>Admin: 200 OK (TenantEntitlementView actualizado)

    Admin->>Ctrl: POST /api/v1/backoffice/tenants/{id}/subscription/suspend (mode=TOTAL)
    Ctrl->>Svc: suspend(...)
    Svc->>Svc: Actualiza status=SUSPENDED, suspension_mode=TOTAL
    Svc->>Sec: revokeAllTenantSessions(tenantId)
    Svc->>Bus: Publica TenantEntitlementChangedEvent(tenantId)
    Bus-->>Cache: Evict/Reload Snapshot O(1)
    Ctrl-->>Admin: 200 OK (Bloqueo Total Inmediato)
```
