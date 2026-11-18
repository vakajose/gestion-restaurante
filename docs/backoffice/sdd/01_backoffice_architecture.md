---
title: "01. Arquitectura del Módulo Backoffice & Desacoplamiento Event-Driven"
category: "sdd"
module: "backoffice"
tags:
  - sdd
  - backoffice
  - architecture
  - spring-modulith
  - event-driven
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 01. Arquitectura del Módulo Backoffice & Desacoplamiento Event-Driven

> ⬅️ [Volver al Índice Backoffice](../README.md) | [ADR-0029](../../adr/0029_backoffice_subscriptions_quotas_and_telemetry.md)

## 1. Principio de Aislamiento Dual (Backend + Monorepo)

El ecosistema opera con **dos aplicaciones frontend hermanas** y un único backend modular en Spring Modulith:

| Componente | Ubicación en Monorepo | Tecnología | Rol |
|---|---|---|---|
| **Backend API Modular** | `backend/` | Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1 | Expone `/api/v1/**` (ERP) y `/api/v1/backoffice/**` (Control Plane) |
| **ERP / POS Restaurante** | `frontend/` | Angular 22 Zoneless + Dexie.js | Operación diaria de restaurantes (`ADMIN_TENANT`, `CASHIER`, etc.) |
| **Backoffice SaaS Admin** | `backoffice-frontend/` *(hermana al mismo nivel)* | React 19 LTS + Vite + TypeScript + Tailwind CSS 3.4 | Administración multi-tenant, planes, pagos, telemetría y logs |

---

## 2. Arquitectura Interna de Comunicación (Sin Dependencias Circulares)

En Spring Modulith (`ApplicationModules.verify()`), los módulos de negocio (`company`, `catalog`, `inventory`, `finance`, `pos`) **no deben depender de `modules.backoffice`**, ya que `modules.backoffice` consume sus `.api` para gestión y telemetría. Para evitar ciclos se aplican tres patrones:

### A. Dependency Inversion + Caché Reactivo (`core.entitlements`)
1. `core.entitlements.api` declara contratos públicos:
   - `TenantEntitlementService`: verifica acceso (`assertLoginAllowed`, `assertPosAndShiftOperationsAllowed`), valida cuotas antes de crear/activar recursos (`assertQuotaAvailable`) y expone la vista consolidada (`getEntitlementView(UUID tenantId)`).
   - `TenantEntitlementPort`: puerto SPI implementado por `TenantEntitlementPortImpl` en `modules.backoffice.internal`.
   - `TenantEntitlementChangedEvent`: evento de dominio emitido por `TenantSubscriptionService` y `SubscriptionPlanService` cada vez que cambia suscripción, plan, pago, gracia, suspensión u *override*.
   - Enums de dominio: `SubscriptionStatus`, `SuspensionMode` (`NONE`, `POS_AND_SHIFT_ONLY`, `TOTAL`), `QuotaResource` (`BRANCHES`, `USERS`, `DISHES`, `INVENTORY_ITEMS`, `MONTHLY_ORDERS`).
2. `core.entitlements.internal.TenantEntitlementServiceImpl` mantiene un caché `ConcurrentHashMap<UUID, TenantEntitlementSnapshot>` con invalidación ante `TenantEntitlementChangedEvent`, logrando verificación **O(1)** en `AuthService`, `JwtAuthenticationFilter`, `Finance` y `POS`.

### B. Proyección CQRS y Gracia por Eventos (`BackofficeTelemetryListener`)
1. Cada orden cobrada o anulada en `modules.pos` publica `OrderPaidEvent` o `OrderCancelledEvent`.
2. `BackofficeTelemetryListener` (`modules.backoffice.internal`) combina dos listeners:
   - `@EventListener onOrderPaidGraceTracking(OrderPaidEvent)`: incremento síncrono ACID de `grace_orders_used` en `GRACE_PERIOD`, evaluando transición automática inmediata según `auto_suspend_mode_on_expire` sin race conditions.
   - `@ApplicationModuleListener on(OrderPaidEvent)` y `on(OrderCancelledEvent)`: proyección asíncrona respaldada por outbox hacia `tenant_daily_telemetry` (agregado por `tenant_id`, `branch_id` y `stat_date` calculado en `branches.timezone`, `ADR-0006`).

### C. Conteo Estructural por `.api` y Nota de `OnboardingController`
- `TenantUsageSnapshotReader` y `BackofficeTenantService` consultan cifras estructurales, usuarios y órdenes vía APIs públicas: `UserPublicApi`, `CompanyPublicApi`, `BranchPublicApi`, `CatalogPublicApi`, `InventoryPublicApi` y `PosPublicApi`.
- **Nota sobre alta desde Backoffice (`POST /api/v1/backoffice/tenants`):** Este endpoint reside en `OnboardingController` (`modules.company.internal`) delegando en `OnboardingPublicApi.executeOnboarding(request, false)` (para no registrar primer login hasta el ingreso real en ERP), evitando duplicar lógica de onboarding en `modules.backoffice`.

---

## 3. Reglas de Seguridad Independientes
- **Cadena JWT `@Order(1)` (`BackofficeSecurityConfig`):** Cubre `/api/v1/backoffice/**` y `/actuator/**`; permite sin token `/api/v1/backoffice/auth/{login,refresh,logout}` y `/actuator/health/**`. El resto exige JWT firmado con audiencia `platform-backoffice` y rol `ROLE_SUPERADMIN` o `ROLE_PLATFORM_OPERATOR`.
- **Auditoría Obligatoria:** Las mutaciones administrativas escriben trazabilidad inmutable en `platform_audit_logs` (`details_json TEXT`) vía `PlatformAuditService`.
