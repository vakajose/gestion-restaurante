---
title: "Secuencia de Resolución Multinivel de Parámetros, Catálogo JSON y Hot-Reload"
category: "behavior-flow"
module: "configuration"
tags: ["sequence", "configuration", "parameters", "sparse-storage", "etag", "hot-reload", "catalog-json", "audit"]
last_updated: "2026-11-17"
status: "verified"
---

# Secuencia de Resolución Multinivel de Parámetros, Catálogo JSON y Hot-Reload

Flujo completo para consultar parámetros efectivos en cascada (**`BRANCH -> TENANT -> GLOBAL -> parameters-catalog.json`**), servir metadatos humanizados y ayuda contextual de los **20 parámetros** registrados, optimizar lecturas mediante `ETag` HTTP `304 Not Modified`, exigir reautenticación ante cambios de criticidad `HIGH` y propagar invalidación inmediata de caché vía `ParameterUpdatedEvent`.

> [!NOTE]
> Este diagrama consolida el catálogo JSON humanizado (`parameters-catalog.json`, 20 claves activas en Sprint 6) con el ciclo de actualización en caliente y reautenticación forense (`ADR-0008`, `ADR-0025`).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin Tenant / Gerente
    participant ParamUI as ParametersComponent (Angular)
    participant ConfigCtrl as ConfigurationController (/api/v1/parameters)
    participant ConfigSvc as ConfigurationServiceImpl
    participant Catalog as SystemParameterCatalog (20 claves JSON)
    participant Resolver as CascadingParameterResolver + Cache
    participant AuthSvc as AuthService
    participant Outbox as Spring EventPublisher

    %% 1. Consulta con catálogo humanizado y ETag HTTP
    Admin->>ParamUI: Abrir Consola de Parámetros (scope, branchId, category)
    ParamUI->>ConfigCtrl: GET /api/v1/parameters?scope=BRANCH&branchId={id} (If-None-Match: ETag previo)
    ConfigCtrl->>ConfigSvc: getParameters(scope, category, effectiveBranchId, tenantId)

    loop Por cada ParameterDefinition del catálogo (20 claves)
        ConfigSvc->>Catalog: Obtener name, description, criticality, options, help
        ConfigSvc->>Resolver: resolveDetails(key, scope, branchId, tenantId)
        Note right of Resolver: Precedencia cascada BRANCH -> TENANT -> GLOBAL -> defaultValue (parameters-catalog.json)
    end

    ConfigCtrl->>ConfigCtrl: Calcular SHA-256 ETag sobre lista efectiva
    alt ETag coincide con If-None-Match
        ConfigCtrl-->>ParamUI: HTTP 304 Not Modified
    else Valores modificados o primera carga
        ConfigCtrl-->>ParamUI: HTTP 200 OK + ETag + List SystemParameterDto (metadata + ayuda + valor)
        ParamUI-->>Admin: Renderizar controles humanizados (toggles, selects, ayuda)
    end

    %% 2. Modificación con reautenticación si criticality == HIGH
    Admin->>ParamUI: Modificar parámetro y guardar
    alt criticality == HIGH
        ParamUI->>Admin: Solicitar contraseña actual + auditReason (mínimo 10 caracteres)
    end

    ParamUI->>ConfigCtrl: PATCH /api/v1/parameters/{key} (scope, branchId, value, passwordConfirmation, auditReason)
    ConfigCtrl->>ConfigSvc: updateParameterFromRequest(...)

    opt def.criticality() == HIGH
        ConfigSvc->>ConfigSvc: Validar passwordConfirmation no vacío y auditReason >= 10 chars
        ConfigSvc->>AuthSvc: login(tenantSlug, username, passwordConfirmation)
        alt Contraseña incorrecta
            AuthSvc-->>ConfigSvc: BadCredentialsException -> HTTP 422 BusinessRuleException
        end
    end

    ConfigSvc->>ConfigSvc: Upsert disperso en system_parameter_values (Sparse Storage)
    ConfigSvc->>Resolver: cacheService.evictForParameter(key, scope, tenantId, branchId)
    ConfigSvc->>Outbox: publishEvent(ParameterUpdatedEvent(key, scope, oldValue, newValue))
    ConfigSvc-->>ConfigCtrl: SystemParameterDto actualizado
    ConfigCtrl-->>ParamUI: HTTP 200 OK
    ParamUI-->>Admin: Confirmar cambio activo en caliente (sin reinicio)

    %% 3. Reset a valor heredado
    opt Restablecer override al nivel padre
        Admin->>ConfigCtrl: DELETE /api/v1/parameters/{key}?scope=BRANCH&branchId={id}
        ConfigCtrl->>ConfigSvc: resetOverride(...) -> DELETE en system_parameter_values + evict + ParameterUpdatedEvent
        ConfigCtrl-->>ParamUI: HTTP 204 No Content
    end
```

## Catálogo Activo Verificado (`parameters-catalog.json`, 20 Parámetros)

| Categoría | Claves Activas | Default Base |
| :--- | :--- | :--- |
| **`INVENTORY`** (`2`) | `inventory.deduction_mode`, `inventory.allow_negative_stock` | `AUTOMATIC_PERMISSIVE`, `false` |
| **`POS`** (`6`) | `pos.display_dish_images`, `pos.default_order_mode`, `pos.allow_scheduled_pickup`, `pos.delivery_fee_enabled`, `pos.delivery_fee_default`, `pos.cancellation_prepared_waste_policy` | `true`, `DINE_IN`, `true`, `false`, `0.00`, `ALLOW_AUTO_BUFFER_REUSE` |
| **`FINANCE`** (`6`) | `finance.max_petty_cash_expense`, `finance.require_supervisor_pin_cancel`, `finance.shift_opening_policy`, `finance.shift_concurrency_mode`, `finance.expense_allow_self_approval`, `finance.auto_open_full_catalog_session` | `100.00`, `true`, `ALL_AUTHORIZED`, `SINGLE_PER_BRANCH`, `false`, `true` |
| **`SECURITY`** (`3`) | `security.require_user_email`, `security.enforce_password_change_on_reset`, `security.session_inactivity_timeout_minutes` | `true`, `true`, `15` |
| **`COMPANY` / `CATALOG`** (`3`) | `company.currency`, `company.timezone`, `catalog.scope_mode` | `BOB`, `America/La_Paz`, `BRANCH_ISOLATED` |

## Enlaces Relacionados

- [Casos de Uso de Configuración](../03-use-cases/use_cases_configuration.md)
- [Resolución RBAC y Políticas de Seguridad](./sequence_rbac_resolution_and_enforcement.md)
- [Ciclo de Vida de Sesión y Refresh](./sequence_session_lifecycle_and_refresh.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](../../../adr/0008_unified_multilevel_parameter_system.md)
- [ADR-0025: Consola Humanizada de Parámetros y Catálogo JSON](../../../adr/0025_parameter_console_humanization_json_catalog_and_contextual_help.md)
- [ADR-0030: Aislamiento por Sucursal del Catálogo (`catalog.scope_mode`)](../../../adr/0030_catalog_branch_isolation_by_default.md)
