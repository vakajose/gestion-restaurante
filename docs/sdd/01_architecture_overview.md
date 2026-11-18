---
title: "01. Vista General de Arquitectura"
category: "sdd"
tags:
  - sdd
  - architecture
  - modulith
  - multi-tenant
last_updated: "2026-11-17"
status: "verified"
---

# 01. Vista General de Arquitectura

## 1. Topología del Ecosistema y Modelo Modular

El sistema combina **tres aplicaciones** en monorepo con un backend estructurado como **Monolito Modular** mediante **Spring Modulith 2.1.x** sobre **Java 25 LTS** y **Spring Boot 4.1.x**:

1. **`backend/`:** API REST modular que atiende el ERP (`/api/v1/**`) y el plano de control SaaS (`/api/v1/backoffice/**`).
2. **`frontend/`:** SPA ERP/POS en **Angular 22** (`provideZonelessChangeDetection()`), **TailwindAdmin v3.0** y **Dexie.js** (IndexedDB).
3. **`backoffice-frontend/`:** SPA independiente de plataforma en **React 19 LTS**, **Vite** y **Tailwind CSS 3.4** ([Sub-Bóveda Backoffice](../backoffice/README.md)).

### Módulos Activos en Backend (`com.restaurant.app`)
- **`core.common`:** Clases base `BaseTenantEntity` / `BaseBranchEntity`, entidades compartidas `Tenant` / `Branch`, `GlobalExceptionHandler` (`ProblemDetail` RFC 7807), `WebConfig` (`/uploads/**`), `DataInitializer` (`@Profile({"dev", "test"})`) y excepciones de dominio tipadas.
- **`core.security`:** Autenticación JWT nativa local (HMAC-SHA256 + Argon2id, sin proveedores externos), identidad adaptativa (`username` obligatorio, `email` opcional), RBAC dinámico (`roles`, `role_permissions`, `permissions`), asignación multi-sucursal (`user_branches`), rotación de refresh tokens (`refresh_tokens`) y `TenantContextHolder`.
- **`core.config`:** Configuración operativa heredada (`TenantConfigService`, `BranchConfigService`), `FlywayConfig` y configuración global de plataforma (`PlatformSettingsService` sobre `platform_settings`).
- **`core.entitlements`:** Puerto SPI (`TenantEntitlementPort`), servicio y caché en memoria `O(1)` (`TenantEntitlementService`) para validar estado de suscripción, bloqueos y cuotas sin acoplar módulos ERP a `modules.backoffice`.
- **`core.storage`:** Contrato agnóstico `MediaStorageService` con Strategy Pattern (`LocalStorageStrategy` y `S3CompatibleStorageStrategy` para OCI Object Storage / S3 / R2 / MinIO).
- **`core.audit`:** Paquete reservado (`package-info.java`) para futura auditoría transversal ERP sobre `audit_logs`. *(Actualmente la auditoría inmutable activa reside en `platform_audit_logs` dentro de `modules.backoffice` y en `system_parameter_values` para parametría).*
- **`modules.company`:** Gestión de empresa, sucursales y asistente de onboarding (`OnboardingController` / `OnboardingPublicApi`).
- **`modules.configuration`:** Motor de **20 parámetros** tipados en `SystemParameter` + catálogo JSON `parameters-catalog.json` con resolución en cascada (`BRANCH` $\rightarrow$ `TENANT` $\rightarrow$ default), caché concurrente y evento `ParameterUpdatedEvent`.
- **`modules.catalog`:** Categorías con `code_prefix`, insumos, platos maestros/sucursal y clonación, recetas BOM, `combos` (incluidos bloques dinámicos `MENU_COURSE`), `modifier_groups` / `modifiers`, `daily_menus` y jornadas de servicio (`branch_service_sessions`).
- **`modules.inventory`:** Existencias en `branch_stock`, costo promedio ponderado, `prepared_dish_buffer`, Kardex inmutable (`kardex_movements`) y deducción polimórfica ante `OrderPaidEvent` / `OrderCancelledEvent`. *(Nota de alcance: las tablas `purchases` y `purchase_items` existen en esquema `V1` como capacidad futura sin controlador ni UI activos; ver `PB-021` / `TASK-636`).*
- **`modules.pos`:** Comandas (`orders`, `order_items`, `order_item_modifiers`, `order_item_selections`), modalidades (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`), `promotions`, cancelaciones con PIN/autorización y sincronización idempotente (`client_transaction_id`).
- **`modules.finance`:** Turnos de caja (`cash_shifts`) con soporte único por sucursal o por cajero (`SINGLE_PER_BRANCH` | `INDIVIDUAL_PER_CASHIER`), gastos menores, cola de autorizaciones (`shift_authorizations`), histórico multicriterio y arqueo.
- **`modules.backoffice`:** Plano de control SaaS multi-tenant (administradores de plataforma, planes, suscripciones, cargos FIFO, gracia condicional, telemetría CQRS `BackofficeTelemetryListener` y observabilidad Actuator). Ver [01_backoffice_architecture.md](../backoffice/sdd/01_backoffice_architecture.md).
- **`modules.analytics`:** Módulo *stub* (`package-info.java` únicamente), planificado en backlog futuro (`PB-024`). *(Al igual que la vista KDS `/kds`, aún no forma parte del alcance implementado).*

---

## 2. Aislamiento Multi-Tenant y Multi-Sucursal

- **Aislamiento Lógico en BD:** Las tablas de empresa incluyen `tenant_id` (`tenants`, `categories`, `ingredients`, `roles`, etc.); las tablas operativas y transaccionales (`orders`, `order_items`, `branch_stock`, `kardex_movements`, `cash_shifts`, `expenses`, `branch_dishes`, `branch_service_sessions`) denormalizan `tenant_id` y `branch_id` con índices compuestos y bloqueo optimista `@Version` en entidades críticas.
- **Superclases y Entidades:** `core.common.domain` provee `BaseTenantEntity` y `BaseBranchEntity` como referencia arquitectónica, mientras las entidades JPA concretas declaran explícitamente sus campos `tenantId` / `branchId` / `version` mapeados al esquema Flyway.
- **Propagación de Contexto (`TenantContextHolder`):** `JwtAuthenticationFilter` valida el JWT ERP, extrae `tenant_id`, `user_id` y `branch_id` (cabecera `X-Branch-Id` validada contra sucursales autorizadas) y los deposita en `ThreadLocal`. En `/api/v1/backoffice/**`, `BackofficeSecurityConfig` (`@Order(1)`) exige audiencia `platform-backoffice`.

---

## 3. Principios de Diseño y Gobernanza

- **Encapsulación Modulith:** Comunicación intermodular exclusivamente por paquetes `.api` o eventos Spring Modulith (`ApplicationModules.verify()`).
- **Offline-First & Idempotencia:** Ninguna venta POS se bloquea por red; Dexie.js persiste primero localmente con `clientTransactionId`.
- **Fechas y Zonas Horarias (`ADR-0006`):** `DATE`/`LocalDate` para fechas puras y `TIMESTAMPTZ`/`Instant` UTC convertidos a `branches.timezone` (default `'America/La_Paz'`).
- **SDD-SDLC (`ADR-0007`):** Especificación viva sincronizada con el código.

---

## 4. Diagramas Vivos de Arquitectura

Consulta la bóveda de diagramas Mermaid en [docs/project/architecture/](../project/architecture/README.md):
- **C4 Nivel 2 y 3:** [c4_level2_containers.md](../project/architecture/01-c4-system/c4_level2_containers.md) | [c4_level3_backend_modulith.md](../project/architecture/01-c4-system/c4_level3_backend_modulith.md) | [c4_level3_frontend_angular.md](../project/architecture/01-c4-system/c4_level3_frontend_angular.md)
- **Dominio, ERD y Dexie:** [domain_model.md](../project/architecture/02-domain-and-data/domain_model.md) | [database_erd.md](../project/architecture/02-domain-and-data/database_erd.md) | [dexie_local_schema.md](../project/architecture/02-domain-and-data/dexie_local_schema.md)
- **Casos de Uso:** [use_cases_overview.md](../project/architecture/03-use-cases/use_cases_overview.md)
- **Comportamiento (14 flujos):** [sequence_pos_order_deduction.md](../project/architecture/04-behavior-and-flows/sequence_pos_order_deduction.md) | [sequence_pos_offline_sync.md](../project/architecture/04-behavior-and-flows/sequence_pos_offline_sync.md) | [state_order_lifecycle.md](../project/architecture/04-behavior-and-flows/state_order_lifecycle.md)
