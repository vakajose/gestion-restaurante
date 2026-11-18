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
last_updated: "2026-11-17"
status: "verified"
---

# 02. Especificación Backend & Contratos REST del Backoffice

> ⬅️ [Volver al Índice Backoffice](../README.md) | [Arquitectura](./01_backoffice_architecture.md)

Base path exclusivo de plataforma: `/api/v1/backoffice/**` (segregado por audiencia JWT `platform-backoffice`, `ADR-0027`). Todos los errores siguen **RFC 7807 (`ProblemDetail`)**.

---

## 1. Autenticación (`PlatformAuthController`) y ABM de Administradores (`PlatformAdminController`)

| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `POST` | `/api/v1/backoffice/auth/login` | `permitAll` — Autentica `platform_admins` por `username`/`email` + password y emite access JWT (`platform-backoffice`) + refresh token opaco |
| `POST` | `/api/v1/backoffice/auth/refresh` | `permitAll` — Rota `platform_refresh_tokens` (`replaced_by_id`) y emite nuevo par de tokens |
| `POST` | `/api/v1/backoffice/auth/logout` | `permitAll` — Revoca el refresh token de plataforma presentado |
| `GET` | `/api/v1/backoffice/auth/me` | `SUPERADMIN \| PLATFORM_OPERATOR` — Perfil actual (`adminId`, `username`, `email`, `fullName`, `role`, `mustChangePassword`) |
| `GET` | `/api/v1/backoffice/auth/password-policy` | `SUPERADMIN \| PLATFORM_OPERATOR` — Política vigente (`minLength`, `requireComplexity`) desde `platform_settings` |
| `POST` | `/api/v1/backoffice/auth/change-password` | `SUPERADMIN \| PLATFORM_OPERATOR` — Cambio de contraseña propio cumpliendo política; limpia `mustChangePassword = false` |
| `GET` | `/api/v1/backoffice/admins` | `SUPERADMIN` — Lista cuentas `platform_admins` |
| `POST` | `/api/v1/backoffice/admins` | `SUPERADMIN` — Alta de administrador (`SUPERADMIN` \| `PLATFORM_OPERATOR`) con `email` opcional y `mustChangePassword = true` |
| `PATCH` | `/api/v1/backoffice/admins/{adminId}/status` | `SUPERADMIN` — Activa/desactiva cuenta (prohíbe desactivar al último `SUPERADMIN` activo o a sí mismo) |

---

## 2. Catálogo de Planes (`SubscriptionPlanService`), Suscripciones (`TenantSubscriptionService`) y Pagos

### 2.1 Planes Base (`SubscriptionPlanController` — `/api/v1/backoffice/plans`)
| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/plans` | `SUPERADMIN \| PLATFORM_OPERATOR` — Lista planes con filtro opcional `?category=PUBLIC_ONBOARDING\|TRIAL\|CUSTOM_PRIVATE` |
| `POST` | `/api/v1/backoffice/plans` | `SUPERADMIN` — Crea plan (`code`, `name`, `planCategory`, `billingCycle`, `durationDays >= 0`, `price >= 0`, `currency`, límites base) |
| `PUT` | `/api/v1/backoffice/plans/{planId}` | `SUPERADMIN` — Actualiza detalles/cuotas del plan e invalida caché de tenants vinculados |
| `PATCH` | `/api/v1/backoffice/plans/{planId}/status` | `SUPERADMIN` — Activa/desactiva disponibilidad del plan |

### 2.2 Suscripción de un Tenant, Gracia, Overrides y Bloqueos (`TenantSubscriptionController`)
| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/assign-plan` | `SUPERADMIN \| PLATFORM_OPERATOR` — Asigna plan (`markAsPaid: true` o **uso por adelantado** `false`), crea cargo en `subscription_charges` y emite `TenantEntitlementChangedEvent` |
| `PUT` | `/api/v1/backoffice/tenants/{tenantId}/subscription/quotas` | `SUPERADMIN` — Configura o limpia *overrides* (`maxBranches`, `maxUsers`, `maxDishes`, `maxInventoryItems`, `maxMonthlyOrders`) |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/grace` | `SUPERADMIN \| PLATFORM_OPERATOR` — Otorga gracia condicional (`graceUntil`, `graceMaxOrders`, nota) |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/suspend` | `SUPERADMIN` — Suspende manualmente (`POS_AND_SHIFT_ONLY` \| `TOTAL`) y revoca sesiones vía `SessionPublicApi` si es `TOTAL` |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/subscription/reactivate` | `SUPERADMIN` — Reactiva suscripción levantando el bloqueo |
| `PATCH` | `/api/v1/backoffice/tenants/{tenantId}/subscription/expire-policy` | `SUPERADMIN` — Define `autoSuspendModeOnExpire` (`POS_AND_SHIFT_ONLY` \| `TOTAL`) |

### 2.3 Cargos Pendientes (Mora) e Histórico de Pagos (`TenantSubscriptionController`)
| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/charges` | `SUPERADMIN \| PLATFORM_OPERATOR` — Lista cargos históricos y saldo pendiente total |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/payments` | `SUPERADMIN \| PLATFORM_OPERATOR` — Histórico de pagos registrados |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/payments` | `SUPERADMIN \| PLATFORM_OPERATOR` — Registra pago y concilia FIFO cargos `PENDING`/`PARTIAL` |
| `PATCH` | `/api/v1/backoffice/charges/{chargeId}/waive` | `SUPERADMIN` — Condona/anula cargo pendiente con auditoría |

---

## 3. Gestión de Inquilinos y Telemetría (`BackofficeTenantController` + `OnboardingController`)

| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/dashboard/overview` | `SUPERADMIN \| PLATFORM_OPERATOR` — KPIs globales (tenants por estado, ingresos del mes, mora total y top tenants) |
| `GET` | `/api/v1/backoffice/tenants` | `SUPERADMIN \| PLATFORM_OPERATOR` — Listado con filtros (`search`, `status`, `hasDebt`, `overQuota`), estado de primer ingreso y consumo |
| `POST` | `/api/v1/backoffice/tenants` | *(Expuesto en **`OnboardingController`**, `modules.company.internal`)* — Alta atómica de empresa, sucursal y admin con `address` y `email` opcionales (`executeOnboarding(request, false)` para preservar `last_login_at = NULL` hasta el primer login real en ERP) |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}` | `SUPERADMIN \| PLATFORM_OPERATOR` — Ficha 360°: primer ingreso (`firstLoginAt`, `hasLoggedIn`), suscripción, cuotas vs uso (`UserPublicApi`, `BranchPublicApi`, `CatalogPublicApi`, `InventoryPublicApi`, `PosPublicApi`) y alertas *over-quota* |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/telemetry` | `SUPERADMIN \| PLATFORM_OPERATOR` — Series diarias por sucursal (`from`, `to` ISO `DATE`), resumen de inventario y usuarios |
| `GET` | `/api/v1/backoffice/tenants/{tenantId}/users` | `SUPERADMIN \| PLATFORM_OPERATOR` — Lista `app_users` del tenant vía `UserPublicApi` |
| `POST` | `/api/v1/backoffice/tenants/{tenantId}/users/{userId}/reset-password` | `SUPERADMIN` — Asigna contraseña temporal y fuerza `mustChangePassword = true` |
| `PATCH` | `/api/v1/backoffice/tenants/{tenantId}/users/{userId}/status` | `SUPERADMIN` — Activa/desactiva acceso de un usuario del tenant |

---

## 4. Configuración Global, Storage Check y Observabilidad (`BackofficeOperationsController`)

| Método | Ruta | Acceso / Descripción |
|---|---|---|
| `GET` | `/api/v1/backoffice/settings` | `SUPERADMIN` — Lista `platform_settings` (`storage.*` y `security.platform_password_*`) |
| `PUT` | `/api/v1/backoffice/settings/{settingKey}` | `SUPERADMIN` — Actualiza valor validado e invalida caché de `PlatformSettingsService` |
| `POST` | `/api/v1/backoffice/storage/test-connection` | `SUPERADMIN \| PLATFORM_OPERATOR` — Prueba conectividad **solo del proveedor activo** (`MediaStorageService.testActiveConnection()`) |
| `GET` | `/api/v1/backoffice/observability/health` | `SUPERADMIN \| PLATFORM_OPERATOR` — Salud PostgreSQL, Hikari, disco y aplicación |
| `GET` | `/api/v1/backoffice/observability/metrics` | `SUPERADMIN \| PLATFORM_OPERATOR` — Métricas JVM/Hikari/HTTP |
| `GET` | `/api/v1/backoffice/observability/loggers` | `SUPERADMIN \| PLATFORM_OPERATOR` — Loggers filtrables por `?prefix=com.restaurant.app` |
| `PUT` | `/api/v1/backoffice/observability/loggers/{loggerName}` | `SUPERADMIN` — Cambia nivel de log en caliente con registro en `platform_audit_logs` |
| `GET` | `/api/v1/backoffice/observability/environment` | `SUPERADMIN` — Perfil activo y propiedades efectivas con secretos mascarados |
| `GET` | `/api/v1/backoffice/audit-logs` | `SUPERADMIN` — Consulta paginada (`actor`, `action`, `search`, `from`, `to`, `page`, `size`) |

---

## 5. Contrato ERP y Códigos RFC 7807 (`TenantEntitlementService`)
- **`GET /api/v1/company/entitlements`** *(en `CompanyController`, delega a `TenantEntitlementService.getEntitlementView(tenantId)`)*: Expone al ERP el estado actual de suscripción, saldo pendiente, gracia y cuotas (`overQuotaResources[]`).
- **Códigos HTTP 403 en `EntitlementBlockedException`:**
  - `TENANT_SUSPENDED` (bloqueo `TOTAL`)
  - `TENANT_OPERATIONS_SUSPENDED` (bloqueo `POS_AND_SHIFT_ONLY` en `assertPosAndShiftOperationsAllowed`)
  - `QUOTA_EXCEEDED` (supera límite en `assertQuotaAvailable`)
  - `OVER_QUOTA_RESOLUTION_REQUIRED` (exceso de usuarios/sucursales activos tras downgrade pendiente de desactivación manual)
