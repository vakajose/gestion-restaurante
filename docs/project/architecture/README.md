---
title: "Arquitectura Viva del Sistema (MOC)"
category: "architecture"
tags: ["moc", "architecture", "c4", "erd", "dexie", "use-cases", "behavior", "sprint-6", "sdd"]
last_updated: "2026-11-17"
status: "verified"
---

# Arquitectura Viva del Sistema — Restaurante ERP SaaS (Sprint 6)

Centro de navegación (**Map of Content**) de la arquitectura de software viva conforme al estándar **SDD-SDLC** ([`ADR-0007`](../../adr/0007_sdd_software_development_lifecycle.md)). Todo cambio en contratos `.api`, esquemas Flyway (`V1`–`V24`), flujos operativos o rutas SPA se sincroniza obligatoriamente con estos artefactos.

> [!NOTE]
> **Estado Consolidado en Sprint 6:**
> - **Backend:** Monolito Modular **Spring Modulith** (Java 25 LTS, Spring Boot 4.1.x) con núcleo `core` (`common`, `config`, `entitlements`, `storage`, `security`, `audit`) y módulos de negocio (`company`, `configuration`, `catalog`, `pos`, `inventory`, `finance`, `backoffice`).
> - **Frontend ERP:** **Angular 22 Zoneless** + **TailwindAdmin v3.0** + **Dexie.js v4** (`RestaurantPosDb` `version(2)`), inyección runtime (`window.__ENV__`) y resolución multimedia cross-origin (`resolveMediaUrl` hacia `core.storage` Local u OCI S3).
> - **Frontend Backoffice:** **React 19** + **React Router 7** + Vite 6 + Tailwind CSS 3.4.
> - **Base de Datos:** PostgreSQL 16+ con **56 tablas** verificadas entre `V1` y `V24` (**47 tablas ERP/Outbox** + **9 tablas Backoffice/Suscripciones**).

---

## 1. Vista Estructural C4 (`01-c4-system/`)

| Nivel | Documento | Alcance Principal |
| :--- | :--- | :--- |
| **C4 Nivel 2** | [Contenedores del Sistema](./01-c4-system/c4_level2_containers.md) | Caddy 2.9, Angular 22 ERP SPA, React 19 Backoffice SPA, Dexie.js v4, Backend Spring Modulith, PostgreSQL (`56` tablas), Storage (`LOCAL` / `S3` OCI) y Dozzle. |
| **C4 Nivel 3** | [Componentes del Backend (Spring Modulith)](./01-c4-system/c4_level3_backend_modulith.md) | Fronteras `.api` / `internal`, eventos `OrderPaidEvent`, `OrderCancelledEvent`, `TenantEntitlementChangedEvent`, `core.storage`, `core.entitlements`, estrategias de inventario y árbol real de paquetes. |
| **C4 Nivel 3** | [Componentes del Frontend (Angular 22)](./01-c4-system/c4_level3_frontend_angular.md) | Rutas verificadas (`app.routes.ts`), pipeline HTTP (`apiBaseUrlInterceptor`, `authInterceptor`, `errorInterceptor`), `ThemeService`, `13` componentes UI Kit, DAL local y `PosService`. |
| **C4 Despliegue OCI** | [Topología Cloud OCI Zero-Trust](./01-c4-system/deployment_oci_topology.md) | VCN `10.0.0.0/16`, Subred Pública (`VM 1` Apps + Caddy) y Subred Privada (`VM 2` PostgreSQL 16), Object Storage S3 y Staging efímero (`TASK-609`). |

---

## 2. Dominio y Datos (`02-domain-and-data/`)

| Artefacto | Documento | Cobertura Verificada |
| :--- | :--- | :--- |
| **ERD Relacional (PostgreSQL)** | [Modelo Entidad-Relación ERP](./02-domain-and-data/database_erd.md) | **47 tablas ERP/Sistema** (`V1`–`V24`) distribuidas por dominio funcional, índices únicos y restricciones (`chk_branch_stock_target`, `order_item_selections`, `branch_service_sessions`, etc.). |
| **Almacenamiento Offline (Dexie)** | [Esquema Local Dexie.js](./02-domain-and-data/dexie_local_schema.md) | Stores `version(2)` de `RestaurantPosDb` (`cachedCategories`, `cachedProducts`, `pendingOrders` indexado por `tenantId`/`branchId`, `activeShift`), capa DAL y contratos `pos.models.ts`. |
| **Modelo de Objetos Java** | [Modelo de Dominio Java](./02-domain-and-data/domain_model.md) | **40 entidades JPA ERP/Config** activas, enums/records de contrato y flujo de eventos entre módulos (sin herencias ficticias ni entidades inexistentes). |

---

## 3. Casos de Uso Consolidados (`03-use-cases/`)

| Documento | Dominio / Cobertura |
| :--- | :--- |
| [Visión General y Mapa de Actores](./03-use-cases/use_cases_overview.md) | Matriz global de actores ERP, sucursales y flujos transversales. |
| [Casos de Uso de Onboarding](./03-use-cases/use_cases_onboarding.md) | Asistente de registro de inquilino en 3 pasos, validación de slug/NIT y plantillas operativas. |
| [Casos de Uso de Empresa y Sucursales](./03-use-cases/use_cases_company.md) | Administración de tenant, sucursales, timezone, moneda y branding corporativo. |
| [Casos de Uso de Catálogo y Menús](./03-use-cases/use_cases_catalog.md) | Categorías (prefijos SKU y emojis), platos (recortador de imagen `core.storage` + `resolveMediaUrl`), modificadores, combos dinámicos y menús del día. |
| [Casos de Uso de POS](./03-use-cases/use_cases_pos.md) | Ventas online/offline, modalidades (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`), promociones y anulaciones. |
| [Casos de Uso de Finanzas](./03-use-cases/use_cases_finance.md) | Turnos multi-cajero, gastos de caja menor, autorizaciones PIN/remotas, consolidación, reapertura e historial/exportación CSV. |
| [Casos de Uso de Inventario](./03-use-cases/use_cases_inventory.md) | Saldos de sucursal, Kardex, stock inicial y ajuste con calculadora total $\leftrightarrow$ unitario y estrategias por plato. |
| [Casos de Uso de Configuración](./03-use-cases/use_cases_configuration.md) | Consola de 20 parámetros multinivel (`BRANCH`/`TENANT`/`GLOBAL`), ayuda contextual JSON y reautenticación crítica. |
| [Casos de Uso de RBAC y Usuarios](./03-use-cases/use_cases_rbac.md) | Roles personalizados, vigencia temporal, multi-sucursal, reseteo delegado y autoservicio de perfil/PIN. |
| [Casos de Uso de Sesiones](./03-use-cases/use_cases_sessions.md) | Login multi-tenant, silent refresh con rotación, revocación remota y cierre por inactividad. |

---

## 4. Diagramas de Comportamiento y Flujos (`04-behavior-and-flows/` — 14 Flujos Consolidados)

### Punto de Venta, Promociones y Offline
1. [Flujo de Venta POS, Turno, Modalidades, Combos y Kardex](./04-behavior-and-flows/sequence_pos_order_deduction.md) — Checkout idempotente, `409 SHIFT_REQUIRED_ERROR`, `selectedModifiers`, `order_item_selections`, `orderType`/`deliveryFee`, `FinancePublicApi.recordSale` y `OrderPaidEvent`.
2. [Secuencia de Sincronización Offline POS (Dexie.js)](./04-behavior-and-flows/sequence_pos_offline_sync.md) — Persistencia previa en `pendingOrders` (`v2`) y sincronización batch `POST /api/v1/pos/orders/sync`.
3. [Secuencia de Evaluación de Promociones POS](./04-behavior-and-flows/sequence_pos_promotions_evaluation.md) — `PromotionEngine` por zona horaria local (`ADR-0006`), `GLOBAL`/`CATEGORY`/`DISH`, `AUTOMATIC`/`MANUAL` y cruce de medianoche.
4. [Flujo de Anulación de Pedidos, Autorizaciones y Rescate/Merma](./04-behavior-and-flows/sequence_pos_order_cancellation_and_authorizations.md) — Dual-control por PIN o `ShiftAuthorization` remota, `total_cash_refunds` y resolución `OrderCancelledEvent`.
5. [Ciclo de Vida de la Orden POS](./04-behavior-and-flows/state_order_lifecycle.md) — Estados reales `CART_DRAFT -> OFFLINE_PENDING -> PAID -> CANCELLED` (sin KDS) y derivación de inventario/buffer/merma.

### Catálogo, Inventario, Finanzas y UX
6. [Jornadas de Servicio, Menús del Día, Combos Dinámicos y Cupos](./04-behavior-and-flows/sequence_daily_menu_shift_assignment_and_quota_enforcement.md) — `BranchServiceSession`, `FULL_CATALOG` vs `DAILY_MENU`, `MENU_COURSE`, `allowIndividualSale`, `isUnlimited` y decremento atómico optimista.
7. [Estrategias de Deducción y Reversión Kardex](./04-behavior-and-flows/activity_inventory_deduction_strategy.md) — `MANUAL_ISOLATED`, `InventoryStrategyFactory` (`RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT`, `COMMERCIAL_NO_INVENTORY`), modificadores `SALE_CONSUMPTION` y `PreparedDishBuffer`.
8. [Estados de Turnos de Caja, Multi-Cajero y Gastos](./04-behavior-and-flows/state_shift_and_expense_workflow.md) — `OPEN -> CLOSED -> CONSOLIDATED` + `reopenShift`, `INDIVIDUAL_PER_CASHIER`, devoluciones en efectivo y aprobación RBAC `finance:expenses_approve`.
9. [UX Móvil Táctil POS y Ajuste de Inventario por Lote](./04-behavior-and-flows/state_mobile_pos_and_inventory_ux.md) — `app-bottom-sheet` en móvil y calculadora bidireccional lote $\leftrightarrow$ unitario hacia `/api/v1/inventory/stock/initial` y `/api/v1/inventory/stock/adjust`.

### Plataforma, Seguridad, Configuración y Tenants
10. [Resolución de Parámetros, Catálogo JSON y Hot-Reload](./04-behavior-and-flows/sequence_parameter_resolution_and_hot_reload.md) — Cascada `BRANCH -> TENANT -> GLOBAL`, 20 claves de `parameters-catalog.json`, ETag `304` y reautenticación `HIGH`.
11. [Resolución RBAC, Perfil y Cumplimiento en Endpoints](./04-behavior-and-flows/sequence_rbac_resolution_and_enforcement.md) — Permisos `recurso:acción`, reseteo delegado hacia `/profile`, `security.enforce_password_change_on_reset`, perfil y PIN BCrypt.
12. [Ciclo de Vida de Sesión y Silent Refresh](./04-behavior-and-flows/sequence_session_lifecycle_and_refresh.md) — Rotación de `RefreshToken`, detección de reúso, cola RxJS `401` y monitoreo `IdleTimerService`.
13. [Secuencia de Onboarding de Nuevo Tenant](./04-behavior-and-flows/sequence_tenant_onboarding.md) — Wizard público de 3 pasos, presets geográficos de Bolivia, plantillas (`EMPTY` default backend) y provisión Backoffice (`recordInitialLogin=false`).
14. [Ciclo de Vida, Suscripción y Entitlements del Tenant](./04-behavior-and-flows/state_tenant_lifecycle.md) — Estados `PENDING_ACTIVATION` (`last_login_at`), `TRIAL`, `ACTIVE`, `GRACE_PERIOD`, `SUSPENDED` (`POS_AND_SHIFT_ONLY` / `TOTAL`), `CANCELLED` y cuotas.

---

## 5. Arquitectura Backoffice SaaS (`docs/backoffice/`)

- [Índice del Subsistema Backoffice](../../backoffice/README.md)
- [C4 Backoffice System](../../backoffice/architecture/c4_backoffice_system.md)
- [ERD y Dominio de Suscripciones (9 tablas V17–V22)](../../backoffice/architecture/domain_and_erd_subscriptions.md)
- [Secuencia y Máquina de Estados de Suscripciones](../../backoffice/architecture/sequence_and_state_subscriptions.md)
- [Casos de Uso del Backoffice](../../backoffice/architecture/use_cases_backoffice.md)
- SDD Backoffice: [01 Arquitectura](../../backoffice/sdd/01_backoffice_architecture.md) · [02 API Backend](../../backoffice/sdd/02_backoffice_backend_api_spec.md) · [03 Base de Datos](../../backoffice/sdd/03_backoffice_database_schema.md) · [04 Frontend React](../../backoffice/sdd/04_backoffice_frontend_react_spec.md)

---

## 6. Índice Completo de ADRs (`ADR-0001` – `ADR-0033`)

- [ADR-0001: Monolito Modular Spring Modulith](../../adr/0001_spring_modulith_monolith.md) · [ADR-0002: Multitenancy Híbrido y Settings Dinámicos](../../adr/0002_hybrid_tenancy_and_dynamic_settings.md) · [ADR-0003: Offline-First y Sync Dexie](../../adr/0003_offline_first_dexie_sync.md)
- [ADR-0004: Clonación de Catálogo y Overrides](../../adr/0004_catalog_cloning_and_overrides.md) · [ADR-0005: Turnos de Caja y Gastos](../../adr/0005_cash_shift_expense_workflow.md) · [ADR-0006: Estándar de Fechas y Zonas Horarias](../../adr/0006_datetime_and_timezone_handling.md)
- [ADR-0007: Ciclo de Vida SDD-SDLC](../../adr/0007_sdd_software_development_lifecycle.md) · [ADR-0008: Sistema Unificado de Parámetros Multinivel](../../adr/0008_unified_multilevel_parameter_system.md) · [ADR-0009: Onboarding y Presets Operativos](../../adr/0009_tenant_onboarding_and_operational_presets.md)
- [ADR-0010: RBAC Granular y Privilegios](../../adr/0010_granular_rbac_roles_and_privileges.md) · [ADR-0011: Ciclo de Vida de Sesión y Silent Refresh](../../adr/0011_session_lifecycle_and_silent_refresh.md) · [ADR-0012: Responsividad Móvil y UX Táctil](../../adr/0012_mobile_responsiveness_and_touch_ux.md)
- [ADR-0013: Invariante de Turno POS y Autorizaciones](../../adr/0013_pos_shift_invariant_and_expense_authorizations.md) · [ADR-0014: TailwindAdmin v3, Tema Dinámico y UI Kit](../../adr/0014_tailwindadmin_v3_dynamic_theme_and_uikit.md) · [ADR-0015: Menús Diarios e Inventario Flexible](../../adr/0015_daily_menus_and_flexible_dish_inventory.md)
- [ADR-0016: Control de Caja Separado y Turnos Multi-Cajero](../../adr/0016_cash_control_separation_and_multi_cashier_shifts.md) · [ADR-0017: Gobernanza de Testing Automatizado](../../adr/0017_multilevel_testing_governance_and_automation_standard.md) · [ADR-0018: Modificadores, Extras y Notas POS](../../adr/0018_pos_modifiers_extras_and_kitchen_notes_suite.md)
- [ADR-0019: Cancelaciones, Autorizaciones y Merma](../../adr/0019_order_cancellation_workflow_authorizations_and_waste_treatment.md) · [ADR-0020: Modalidades de Servicio y Delivery](../../adr/0020_service_modalities_takeout_delivery_and_ticket_badges.md) · [ADR-0021: Catálogo Visual, Recorte de Imagen y Vistas POS](../../adr/0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md)
- [ADR-0022: Asistente SKU, Selector de Emojis y Ordenamiento](../../adr/0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md) · [ADR-0023: Auditoría Histórica de Turnos y Exportación CSV](../../adr/0023_shift_history_multicriteria_audit_and_csv_export.md) · [ADR-0024: Perfil Autoservicio y PIN de Supervisor](../../adr/0024_self_service_user_profile_pin_and_humanization.md)
- [ADR-0025: Consola Humanizada de Parámetros y Catálogo JSON](../../adr/0025_parameter_console_humanization_json_catalog_and_contextual_help.md) · [ADR-0026: Stack Cloud y Despliegue](../../adr/0026_cloud_infrastructure_and_deployment_stack.md) · [ADR-0027: Identidad de Plataforma y Clean Go-Live](../../adr/0027_platform_identity_and_clean_golive_bootstrap.md)
- [ADR-0028: Storage Multimedia Agnóstico y Platform Settings](../../adr/0028_agnostic_media_storage_and_platform_settings.md) · [ADR-0029: Suscripciones, Cuotas y Telemetría Backoffice](../../adr/0029_backoffice_subscriptions_quotas_and_telemetry.md) · [ADR-0030: Aislamiento de Catálogo por Sucursal](../../adr/0030_catalog_branch_isolation_by_default.md)
- [ADR-0031: Jornadas de Servicio y Combos Dinámicos](../../adr/0031_branch_service_sessions_and_dynamic_combos.md) · [ADR-0032: Motor de Promociones POS](../../adr/0032_pos_promotions_engine.md) · [ADR-0033: Runtime Env en SPAs y Media Cross-Origin](../../adr/0033_spa_runtime_env_and_cross_origin_media.md)

## Referencias de Gobernanza

- [Protocolo Operativo del Proyecto (AGENTS.md)](../../../AGENTS.md)
- [Skill: Estándar SDD y Diagramación](../../../.agents/skills/sdd-architecture-diagramming/SKILL.md)
- [Índice General de Documentación](../../README.md)
- Especificaciones SDD ERP: [01 Arquitectura](../../sdd/01_architecture_overview.md) · [02 Backend](../../sdd/02_backend_spec.md) · [03 Frontend](../../sdd/03_frontend_spec.md) · [04 Base de Datos](../../sdd/04_database_schema.md) · [05 Design System](../../sdd/05_design_system.md)
