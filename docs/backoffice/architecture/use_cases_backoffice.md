---
title: "Casos de Uso del Backoffice SaaS y Control de Cuotas"
category: "architecture"
level: "Use-Case"
module: "backoffice"
tags:
  - architecture
  - mermaid
  - use-case
  - backoffice
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# Casos de Uso del Backoffice SaaS y Control de Cuotas

> ⬅️ [Volver al Índice Backoffice](../README.md) | [Contratos API](../sdd/02_backoffice_backend_api_spec.md)

```mermaid
flowchart LR
    SA(["SuperAdmin Plataforma"])
    OP(["Operador Plataforma"])
    AT(["Admin Tenant en ERP"])

    UC1["UC-BO-001 Gestionar Perfil y ABM Admins"]
    UC2["UC-BO-002 CRUD Planes de Suscripcion (SubscriptionPlanService)"]
    UC3["UC-BO-003 Asignar Plan (Pagado o Pendiente de Pago)"]
    UC4["UC-BO-004 Registrar Pago y Conciliar Mora FIFO"]
    UC5["UC-BO-005 Otorgar Gracia Condicional (Fecha / Ventas)"]
    UC6["UC-BO-006 Configurar Bloqueos (POS_AND_SHIFT_ONLY vs TOTAL)"]
    UC7["UC-BO-007 Analizar Telemetria Multi-Tenant, Usuarios y Ventas"]
    UC8["UC-BO-008 Gestionar Storage, Politicas y Logs en Caliente"]
    UC9["UC-ERP-001 Resolver Exceso tras Downgrade sin Borrado"]

    SA --> UC1
    SA --> UC2
    SA --> UC3
    SA --> UC4
    SA --> UC5
    SA --> UC6
    SA --> UC7
    SA --> UC8
    OP --> UC3
    OP --> UC4
    OP --> UC5
    OP --> UC7
    AT --> UC9
```

## Resumen de Reglas Críticas por Caso
1. **`UC-BO-002` (`SubscriptionPlanService`):** Gestiona planes categorizados (`PUBLIC_ONBOARDING`, `TRIAL`, `CUSTOM_PRIVATE`) con soporte de vigencia ilimitada (`durationDays = 0`) y límites no negativos (`null` = ilimitado).
2. **`UC-BO-003` (`TenantSubscriptionService`):** Permite asignar cualquier plan marcado como pagado o `PENDING` (uso por adelantado), registrando el cargo en `subscription_charges` y habilitando la operación inmediata.
3. **`UC-BO-005` (Gracia Condicional):** Admite fecha tope (`graceUntil`) y/o tope de órdenes (`graceMaxOrders`); al cumplirse cualquiera de las condiciones (evaluado síncronamente por `BackofficeTelemetryListener`), aplica `autoSuspendModeOnExpire`.
4. **`UC-BO-008` (Storage & Observabilidad):** Prueba conectividad únicamente contra `storage.active_provider` vía `MediaStorageService.testActiveConnection()`; alterna loggers en caliente con trazabilidad en `platform_audit_logs`.
5. **`UC-ERP-001` (Downgrade No Destructivo):** Ante reducción de límites, el inquilino conserva sus registros y desactiva manualmente desde el ERP usuarios o sucursales (`is_active = false`) hasta quedar dentro de cuota (`TenantEntitlementService.getEntitlementView`).
