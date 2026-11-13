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
last_updated: "2026-11-12"
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
    UC2["UC-BO-002 CRUD Planes de Suscripcion"]
    UC3["UC-BO-003 Asignar Plan (Pagado o Pendiente de Pago)"]
    UC4["UC-BO-004 Registrar Pago y Conciliar Mora"]
    UC5["UC-BO-005 Otorgar Gracia Condicional (Fecha / Ventas)"]
    UC6["UC-BO-006 Configurar Bloqueos (POS_AND_SHIFT_ONLY vs TOTAL)"]
    UC7["UC-BO-007 Analizar Telemetria Multi-Tenant y Ventas"]
    UC8["UC-BO-008 Gestionar Storage y Logs en Caliente"]
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
1. **`UC-BO-003` (Uso por Adelantado):** Permite asignar cualquier plan del catálogo (por ejemplo, plan semanal al vencer el mensual) marcado como `PENDING_PAYMENT`, habilitando de inmediato la operación mientras queda registrado el cargo exigible.
2. **`UC-BO-005` (Gracia Condicional):** Admite fecha tope (`graceUntil`) y/o tope de órdenes (`graceMaxOrders`); al cumplirse cualquiera de las condiciones, entra en vigor el modo de bloqueo configurado (`autoSuspendModeOnExpire`).
3. **`UC-BO-008` (Storage & Observabilidad):** Prueba conexión únicamente contra `storage.active_provider`; permite alternar loggers en caliente (`DEBUG`/`INFO`/`TRACE`) con trazabilidad en `platform_audit_logs`.
4. **`UC-ERP-001` (Downgrade No Destructivo):** Ante reducción de límites, el inquilino conserva todos sus registros y elige manualmente desde el ERP qué usuarios o sucursales desactivar (`is_active = false`) para quedar dentro de cuota y reabrir turnos POS.
