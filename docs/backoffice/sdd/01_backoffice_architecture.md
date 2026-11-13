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
last_updated: "2026-11-12"
status: "approved"
---

# 01. Arquitectura del Módulo Backoffice & Desacoplamiento Event-Driven

> ⬅️ [Volver al Índice Backoffice](../README.md) | [ADR-0029](../../adr/0029_backoffice_subscriptions_quotas_and_telemetry.md)

## 1. Principio de Aislamiento Dual (Backend + Monorepo)

El ecosistema opera con **dos aplicaciones frontend hermanas** y un único núcleo modular en Spring Modulith:

| Componente | Ubicación en Monorepo | Tecnología | Rol |
|---|---|---|---|
| **Backend API Modular** | `backend/` | Java 25, Spring Boot 4.1, Spring Modulith 2.1 | Expone `/api/v1/**` (ERP) y `/api/v1/backoffice/**` (Control Plane) |
| **ERP / POS Restaurante** | `frontend/` | Angular 22 Zoneless + Dexie.js | Operación diaria de restaurantes (`ADMIN_TENANT`, `CASHIER`, etc.) |
| **Backoffice SaaS Admin** | `backoffice-frontend/` *(hermana al mismo nivel)* | React LTS + Vite + TypeScript + Tailwind CSS | Administración multi-tenant, planes, pagos, telemetría y logs |

---

## 2. Arquitectura Interna de Comunicación (Sin Dependencias Circulares)

En Spring Modulith (`ApplicationModules.verify()`), los módulos de negocio (`company`, `catalog`, `inventory`, `finance`, `pos`) **no deben depender de `modules.backoffice`**, ya que `modules.backoffice` consume sus `.api` para gestión y telemetría. Para evitar un ciclo de dependencias se aplican tres patrones:

### A. Dependency Inversion + Caché Reactivo (`core.entitlements`)
1. `core.entitlements.api` (`Type.OPEN`) declara contratos públicos:
   - `TenantEntitlementService`: verifica acceso (`assertLoginAllowed`, `assertPosOperationsAllowed`), verifica cuotas antes de crear/reactivar (`assertQuotaAvailable`, `assertNoBlockingOverQuota`), y expone `getSnapshot(tenantId)`.
   - `TenantEntitlementChangedEvent`: evento de dominio emitido por `modules.backoffice` cada vez que cambia suscripción, plan, pago, gracia, suspensión u *override*.
   - Enums de dominio: `SubscriptionStatus`, `SuspensionMode` (`NONE`, `POS_AND_SHIFT_ONLY`, `TOTAL`), `QuotaResource` (`BRANCHES`, `USERS`, `DISHES`, `INVENTORY_ITEMS`, `MONTHLY_ORDERS`).
2. `core.entitlements.internal` (o SPI implementado por `modules.backoffice`) mantiene un caché en memoria `ConcurrentHashMap<UUID, TenantEntitlementSnapshot>` con refresco automático ante `TenantEntitlementChangedEvent`, logrando verificación **O(1)** en `JwtAuthenticationFilter`, `AuthService`, `Finance` y `POS`.

### B. Proyección CQRS por Eventos (`OrderPaidEvent` & `OrderCancelledEvent`)
1. Cada orden cobrada o anulada en `modules.pos` ya publica eventos vía `ApplicationEventPublisher`.
2. `modules.backoffice` escucha ambos eventos mediante `@ApplicationModuleListener`:
   - Actualiza/incrementa atómicamente la tabla `tenant_daily_telemetry` (agregado diario por `tenant_id`, `branch_id` y `stat_date` calculado en la zona horaria de la sucursal — `ADR-0006`).
   - Incrementa `grace_orders_used` cuando el tenant se encuentra en `GRACE_PERIOD`, evaluando transición automática al alcanzar `grace_max_orders`.
3. De este modo, los reportes de ventas del Backoffice leen un *read-model* preagregado de altísima velocidad sin ejecutar consultas analíticas pesadas contra las tablas vivas del POS.

### C. Conteo Estructural por `.api`
Las cifras estructurales activas y totales (usuarios, sucursales, platos, inventario con valor/stock) se consultan de forma desacoplada a través de métodos de solo lectura expuestos en los paquetes `.api` respectivos (`UserPublicApi`, `CompanyPublicApi`, `CatalogPublicApi`, `InventoryPublicApi`, `PosPublicApi`).

---

## 3. Reglas de Seguridad Independientes
- **Cadena JWT `@Order(1)`:** En `BackofficeSecurityConfig`, toda ruta `/api/v1/backoffice/**` (excepto `/auth/login` y `/auth/refresh`) exige tokens firmados con audiencia `platform-backoffice` y rol `ROLE_SUPERADMIN` (o `ROLE_PLATFORM_OPERATOR` en rutas operativas no destructivas donde aplique).
- **Auditoría Obligatoria:** Las mutaciones críticas (cambio de plan, registro de pago, gracia, suspensión, edición de `platform_settings`, cambios de loggers en caliente, gestión de `platform_admins`) escriben un registro inmutable en `platform_audit_logs`.
