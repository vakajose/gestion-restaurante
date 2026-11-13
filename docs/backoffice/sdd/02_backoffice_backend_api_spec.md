---
title: "02. Especificación Backend & Contratos REST del Backoffice"
category: "sdd"
module: "backoffice"
tags:
  - sdd
  - backoffice
  - api
  - rest
  - actuator
  - obsidian
last_updated: "2026-11-12"
status: "approved"
---

# 02. Especificación Backend & Contratos REST del Backoffice

> ⬅️ [Volver al Índice Backoffice](../README.md) | [Arquitectura](./01_backoffice_architecture.md)

Base path exclusivo de plataforma: `/api/v1/backoffice/**` (segregado por audiencia JWT `platform-backoffice`, `ADR-0027`). Todos los errores siguen **RFC 7807 (`ProblemDetail`)**.

---

## 1. Autenticación y ABM de Administradores de Plataforma (`platform_admins`)
*(Endpoints `/auth/login`, `/auth/refresh`, `/auth/logout` ya provistos en `TASK-602`)*

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/auth/me` | Perfil actual (`id`, `username`, `email`, `fullName`, `role`, `mustChangePassword`, `lastLoginAt`) |
| `POST` | `/api/v1/backoffice/auth/change-password` | Cambio de contraseña propio (mínimo 12 chars); limpia `mustChangePassword = false` |
| `GET` | `/api/v1/backoffice/admins` | Lista paginada/completa de cuentas `platform_admins` |
| `POST` | `/api/v1/backoffice/admins` | Alta de administrador (`SUPERADMIN` \| `PLATFORM_OPERATOR`) con `mustChangePassword = true` |
| `PATCH` | `/api/v1/backoffice/admins/{adminId}/status` | Activar/desactivar cuenta (prohíbe desactivar al último `SUPERADMIN` activo o a sí mismo) |

---

## 2. Catálogo de Planes, Suscripciones, Gracia y Pagos

### 2.1 Planes Base (`subscription_plans`)
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/plans` | Lista todos los planes configurados (activos e históricos) |
| `POST` | `/api/v1/backoffice/plans` | Crear plan (`code`, `name`, `billingCycle`, `durationDays`, `price`, `currency`, cuotas por defecto) |
| `PUT` | `/api/v1/backoffice/plans/{planId}` | Actualizar nombre, precio, duración y cuotas base del plan |
| `PATCH` | `/api/v1/backoffice/plans/{planId}/status` | Activar/desactivar plan para nuevas asignaciones |

### 2.2 Suscripción de un Tenant, Bloqueos, Gracia y Overrides
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/assign-plan` | Asigna un plan existente (`planId`), opcionalmente con pago inmediato (`markAsPaid: true` + datos de pago) o **pendiente de pago / uso por adelantado** (`markAsPaid: false`), generando un cargo en `subscription_charges` y activando el período |
| `PUT` | `/api/v1/backoffice/tenants/{tenantId}/subscription/quotas` | Configura o limpia *overrides* por tenant (`maxBranches`, `maxUsers`, `maxDishes`, `maxInventoryItems`, `maxMonthlyOrders`) |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/grace` | Otorga período de gracia condicional (`graceUntil`, `graceMaxOrders`, nota administrativa) |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/suspend` | Suspende explícitamente eligiendo modo (`POS_AND_SHIFT_ONLY` \| `TOTAL`) y motivo |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/reactivate` | Reactiva suscripción (revoca bloqueo; opcionalmente evalúa vigencia actual) |
| `PATCH` | `/api/v1/backoffice/tenants/{tenantId}/subscription/expire-policy` | Configura modo automático al vencer (`POS_AND_SHIFT_ONLY` \| `TOTAL`) |

### 2.3 Cargos Pendientes (Mora) e Histórico de Pagos
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/charges` | Lista cargos/ciclos históricos y saldo pendiente total del tenant |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/payments` | Histórico completo de pagos registrados |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/payments` | Registra un pago (`amount`, `currency`, `paymentMethod`, `referenceCode`, `receiptUrl`, `chargeId` opcional, `notes`) y concilia FIFO cargos `PENDING`/`PARTIAL` |
| `PATCH` | `/api/v1/backoffice/charges/{chargeId}/waive` | Condona/anula un cargo pendiente con motivo auditado |

---

## 3. Gestión de Inquilinos y Telemetría Ejecutiva

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/dashboard/overview` | KPIs globales: total tenants, distribución por estado (`ACTIVE`/`GRACE_PERIOD`/`SUSPENDED`/`OVER_QUOTA`), ingresos cobrados del mes, deuda pendiente total (`PENDING`/`PARTIAL`) y top tenants |
| `GET` | `/api/v1/backoffice/tenants` | Listado con búsqueda/filtros (`status`, `hasDebt`, `overQuota`), mostrando plan actual, deuda, bloqueo, uso vs cuota básica y ventas recientes |
| `POST` | `/api/v1/backoffice/tenants` | Alta/aprovisionamiento directo de nuevo tenant desde Backoffice (delegando en `OnboardingPublicApi` + asignando plan inicial) |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}` | Ficha 360° del tenant: estado legal, suscripción efectiva, cuotas usadas vs límites (`branches`, `users`, `dishes`, `inventoryItems`, `monthlyOrders`), alertas *over-quota*, deuda y sucursales |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/telemetry` | Series de ventas por día (`from`, `to` ISO `DATE`), desglose por sucursal (`paidOrdersCount`, `grossSalesAmount`, `cancelledOrdersCount`), resumen de stock/inventario y usuarios |

---

## 4. Configuración Global, Storage Check y Observabilidad Actuator

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/settings` | Lista `platform_settings` (`storage.active_provider`, `storage.max_file_size_bytes`, `storage.public_url_prefix`) |
| `PUT` | `/api/v1/backoffice/settings/{settingKey}` | Actualiza valor validado e invalida caché de `PlatformSettingsService` |
| `POST` | `/api/v1/backoffice/storage/test-connection` | Prueba lectura/escritura/conectividad **solo del proveedor actualmente activo** (`LOCAL` o `S3`/OCI) sin exponer secretos |
| `GET` | `/api/v1/backoffice/observability/health` | Wrapper seguro de Actuator `HealthEndpoint` (PostgreSQL, HikariPool, espacio en disco, estado app) |
| `GET` | `/api/v1/backoffice/observability/metrics` | Métricas clave JVM/Hikari/HTTP desde `MetricsEndpoint` o `MeterRegistry` (cuando `/actuator` restringe exposición HTTP a `health`) |
| `GET` | `/api/v1/backoffice/observability/loggers` | Lista loggers configurables (por prefijo o paquete `com.restaurant.app.*`) desde `LoggersEndpoint` o `LoggingSystem` |
| `PUT` | `/api/v1/backoffice/observability/loggers/{loggerName}` | Cambia nivel de log en caliente (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`, `RESET`) auditando la operación |
| `GET` | `/api/v1/backoffice/observability/environment` | Propiedades efectivas del perfil activo y endpoints configurados (secretos mascarados `***`) |
| `GET` | `/api/v1/backoffice/audit-logs` | Consulta paginada de `platform_audit_logs` con filtros por actor, acción y fecha |

---

## 5. Contratos en Módulos ERP (Consumo de Cuotas y Estados)
- **`GET /api/v1/company/entitlements`** *(autenticado ERP tenant)*: Expone al ERP Angular el estado actual del tenant (`status`, `suspensionMode`, `pendingBalance`, `periodEnd`, `graceUntil`, `graceOrdersRemaining`, mapa de uso actual vs cuota por recurso e indicador `overQuotaResources[]` para guiar al `ADMIN_TENANT` en desactivación manual tras un *downgrade*).
- **Códigos de Error Estandarizados RFC 7807:**
  - `403 TENANT_SUSPENDED`: Bloqueo `TOTAL` (rechaza `/auth/login`, `/auth/refresh` y toda API ERP).
  - `403 TENANT_OPERATIONS_SUSPENDED`: Bloqueo `POS_AND_SHIFT_ONLY` (rechaza apertura/cierre de turnos o ventas POS, permite lectura y pantalla de regularización).
  - `403 QUOTA_EXCEEDED`: Intento de crear/activar recurso (`BRANCHES`, `USERS`, `DISHES`, `INVENTORY_ITEMS`, `MONTHLY_ORDERS`) superando límite efectivo.
  - `403 OVER_QUOTA_RESOLUTION_REQUIRED`: Bloqueo de operación POS/Caja cuando el tenant tiene más sucursales o usuarios activos que los permitidos tras un *downgrade*.
