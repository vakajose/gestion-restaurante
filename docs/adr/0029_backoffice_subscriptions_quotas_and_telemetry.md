---
title: "ADR-0029: Motor SaaS de Backoffice — Suscripciones, Bloqueos Configurables, Cuotas y Telemetría Event-Driven"
category: "adr"
module: "backoffice"
tags:
  - adr
  - backoffice
  - subscriptions
  - spring-modulith
  - event-driven
  - obsidian
last_updated: "2026-11-12"
status: "accepted"
---

# ADR-0029: Motor SaaS de Backoffice — Suscripciones, Bloqueos Configurables, Cuotas y Telemetría Event-Driven

- **Estado:** Aceptado
- **Fecha:** 2026-11-12
- **Sprint / Tareas:** Sprint 6 — `TASK-604` (`PB-002`), `TASK-605` (`PB-003`)
- **Autores:** Equipo de Arquitectura (Co-diseño interactivo con Product Owner)
- **Especificación Relacionada:** [Sub-Bóveda Backoffice SaaS](../backoffice/README.md)

---

## 1. Contexto

El sistema multi-tenant necesita evolucionar de un ERP gastronómico puro a una plataforma SaaS administrable comercial y operativamente desde un **Backoffice independiente** (`modules.backoffice` en backend y aplicación hermana `backoffice-frontend/` en React LTS):
1. Conforme a `ADR-0027`, las cuentas de plataforma viven aisladas en `platform_admins` y las configuraciones globales en `platform_settings` (`ADR-0028`), requiriendo ABM completo de administradores y cambio de contraseña propia.
2. El negocio requiere **planes de suscripción configurables**, límites por inquilino (*quotas*) con *overrides* individuales, registro histórico de pagos, otorgamiento de **días de gracia condicionales** (por fecha y/o cantidad de ventas) y **uso por adelantado** mediante asignación de un plan existente en estado pendiente de pago / mora.
3. Ante vencimiento, mora o decisión administrativa, debe existir granularidad para permitir al inquilino consultar su ERP sin poder operar en caja/POS o bloquear su acceso completamente.
4. Ante un *downgrade* de plan, está **prohibido eliminar o desactivar datos arbitrariamente**; el cliente debe decidir qué recursos permanecen activos hasta cumplir la cuota.
5. Para toma de decisiones comerciales y soporte técnico en producción, el Backoffice requiere **telemetría de uso por tenant** (ventas por día/sucursal, usuarios, sucursales, platos, inventario), **prueba de conexión al proveedor de storage activo** (`storage.active_provider`) y **observabilidad Actuator** (salud, métricas, logs en caliente y bitácora de auditoría).

---

## 2. Decisión

### 2.1 Arquitectura Event-Driven + SPI `core.entitlements` (Cero Dependencias Circulares)
Para cumplir `RestaurantModularityTests` (`ApplicationModules.verify()`) y evitar consultas SQL pesadas en cada request HTTP del ERP:
- Se introduce el paquete transversal **`core.entitlements`** (`Type.OPEN` dentro de `core`) con:
  - **`TenantEntitlementService` (API pública):** evaluado por `core.security`, `company`, `catalog`, `inventory`, `finance` y `pos`.
  - **Puerto SPI `TenantEntitlementLoader`** implementado por `modules.backoffice`, alimentando un caché `ConcurrentHashMap<UUID, TenantEntitlementSnapshot>`.
  - **Evento `TenantEntitlementChangedEvent`:** emitido ante cualquier mutación en suscripciones, cuotas, pagos, gracia o suspensión, invalidando el caché al instante (**latencia O(1) en filtros JWT y POS**).
- Para métricas de ventas sin escanear tablas transaccionales del POS en cada consulta del Dashboard, `modules.backoffice` consume **`OrderPaidEvent`** y **`OrderCancelledEvent`** vía `@ApplicationModuleListener` para proyectar un read-model CQRS en **`tenant_daily_telemetry`** (con fecha calculada en la zona horaria local de cada sucursal conforme a `ADR-0006`) e incrementar `grace_orders_used` de forma desacoplada.

### 2.2 Dos Niveles de Bloqueo Configurable (`SuspensionMode`)
- **`NONE`:** Operación normal del tenant dentro de su período vigente, gracia o plan asignado pendiente de pago.
- **`POS_AND_SHIFT_ONLY`:** Permite autenticar en el ERP (`/api/v1/auth/login`), consultar reportes históricos, ver su estado de cuenta y resolver excesos de cuota (*downgrade*), pero bloquea con HTTP `403` (`TENANT_OPERATIONS_SUSPENDED`) la apertura/operación de turnos de caja (`modules.finance`) y la creación/sincronización/cobro de comandas (`modules.pos`).
- **`TOTAL`:** Bloqueo total crítico. Impide login y refresh (`403 TENANT_SUSPENDED`), revoca tokens de refresco activos y rechaza solicitudes JWT vigentes en `JwtAuthenticationFilter`.

### 2.3 Motor Genérico de Planes, Uso por Adelantado (Cargos Pendientes) y Gracia Condicional (`V19`)
Se adopta un modelo tipo **cuenta corriente de períodos/cargos** evitando aritmética rígida de "días en negativo":
1. **`subscription_plans`:** Catálogo reutilizable (`code`, `name`, `billing_cycle` `WEEKLY|MONTHLY|ANNUAL|CUSTOM`, `duration_days`, `price`, `currency`, límites `max_branches`, `max_users`, `max_dishes`, `max_inventory_items`, `max_monthly_orders`).
2. **`tenant_subscriptions`:** Estado vivo por inquilino (`plan_id`, `status`, `suspension_mode`, `auto_suspend_mode_on_expire`, `current_period_start`, `current_period_end`, ventana de gracia `grace_until` / `grace_max_orders` / `grace_orders_used`, y campos opcionales `override_max_*`).
3. **`subscription_charges`:** Cada asignación de plan crea un período exigible (`amount_due`, `paid_amount`, `charge_status`: `PENDING` | `PARTIAL` | `PAID` | `WAIVED`). Asignar un plan existente como pendiente de pago (por ejemplo, 1 semana a crédito cuando venció el mes) extiende `current_period_end` de inmediato y registra el cargo en mora hasta su cancelación.
4. **`subscription_payments`:** Registra abonos (`amount`, `method`, `reference_code`, `receipt_url`, `recorded_by_admin_id`), conciliando cargos pendientes en orden FIFO (o asignación explícita) y reflejando saldo pendiente en línea.
5. **Gracia Condicional:** Si `grace_until` expira **o** `grace_orders_used >= grace_max_orders`, la política `TenantAccessPolicy` transiciona automáticamente al bloqueo configurado.

### 2.4 Cuotas y Downgrade No Destructivo
- Se validan cuotas al crear o reactivar recursos (`BRANCHES`, `USERS`, `DISHES`, `INVENTORY_ITEMS`, `MONTHLY_ORDERS`) comparando uso activo actual vs cuota efectiva (`override_max_*` ?? `plan.max_*`, `null` = ilimitado). Si alcanza o supera la cuota, se responde `403 QUOTA_EXCEEDED`.
- Si tras un *downgrade* resulta `activeCount > limit` (*Over-Quota*):
  - **Nunca se borran ni desactivan registros automáticamente.**
  - Se bloquea crear/reactivar nuevos ítems de ese recurso.
  - Si el exceso ocurre en **`BRANCHES`** o **`USERS`**, el `ADMIN_TENANT` conserva acceso al ERP para desactivar manualmente los usuarios o sucursales elegidos (`is_active = false`), bloqueándose entretanto nuevas aperturas de turno/ventas POS (`403 OVER_QUOTA_RESOLUTION_REQUIRED`) hasta regularizar la cuota.
  - Si el exceso ocurre en **`MONTHLY_ORDERS`**, simplemente no se permiten más comandas en el mes calendario corriente.

### 2.5 Observabilidad Curada, Prueba de Storage Activo y Separación Documental
- **Observabilidad (`Propuesta A`):** Los endpoints `/api/v1/backoffice/observability/**` (protegidos por `ROLE_SUPERADMIN`) envuelven beans de **Spring Boot Actuator** (`HealthEndpoint`, `MetricsEndpoint`, `LoggersEndpoint`) más un visor de configuración con secretos enmascarados (`***`), evitando exponer `/actuator/**` completo al navegador. Toda acción sensible queda registrada en **`platform_audit_logs`**.
- **Storage Health Check:** `POST /api/v1/backoffice/storage/test-connection` verifica exclusivamente el proveedor configurado como activo en `platform_settings` (`storage.active_provider`).
- **Aislamiento de Documentación y Proyecto React:** La documentación técnica del Backoffice vive aislada del ERP en [docs/backoffice/](../backoffice/README.md) manteniendo compatibilidad total con la bóveda Obsidian (`docs/`), y la SPA administrativa se ubicará al mismo nivel en `backoffice-frontend/`.

---

## 3. Consecuencias

- **Desacoplamiento y Rendimiento:** Los módulos ERP consultan un único contrato estable `core.entitlements` en memoria sin acoplarse a JPA de facturación/suscripciones.
- **Trazabilidad Comercial y Financiera:** Cada extensión o gracia otorgada manualmente queda respaldada en `subscription_charges`, `subscription_payments` y `platform_audit_logs`.
- **Evolución Incremental Segura:** La separación de `docs/backoffice/` y `backoffice-frontend/` permite iterar la consola SaaS en React LTS sin contaminar la base de código ni los manuales del ERP Angular.
