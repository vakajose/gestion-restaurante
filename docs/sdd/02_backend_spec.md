---
title: "02. Especificación Técnica de Backend"
category: "sdd"
tags:
  - sdd
  - backend
  - java25
  - spring-boot
  - spring-modulith
last_updated: "2026-11-17"
status: "verified"
---

# 02. Especificación Técnica de Backend

> [!NOTE]
> Para los diagramas C4 y secuencias del backend consulta [c4_level3_backend_modulith.md](../project/architecture/01-c4-system/c4_level3_backend_modulith.md) y [04-behavior-and-flows/](../project/architecture/04-behavior-and-flows/sequence_pos_order_deduction.md). Para el plano Backoffice SaaS consulta [02_backoffice_backend_api_spec.md](../backoffice/sdd/02_backoffice_backend_api_spec.md).

## 1. Stack Base, Perfiles Twelve-Factor y Errores

- **Runtime:** Java 25 LTS (Amazon Corretto), Spring Boot `4.1.1`, Spring Modulith `2.1.1`, Virtual Threads (`spring.threads.virtual.enabled=true`), Jackson UTC (`write-dates-as-timestamps: false`), Flyway (`V1`–`V24`), PostgreSQL 16.
- **Perfiles reales (`backend/src/main/resources/application*.yml`):**
  - `dev` (default): PostgreSQL `restaurant_dev` (o `SPRING_DATASOURCE_URL`), ejecuta `DataInitializer` y seeders demo (`@Profile({"dev", "test"})`).
  - `testing-local`: Contenerizado sin seeds demo (`restaurant_testing`).
  - `staging` y `prod`: Variables de entorno obligatorias (`SPRING_DATASOURCE_URL`, `JWT_SECRET`, `PLATFORM_JWT_SECRET`, `CORS_ALLOWED_ORIGINS`), `flyway.clean-disabled=true` y supresión de stacktraces.
  - `test` (`src/test/resources/application-test.yml`): H2 en modo PostgreSQL (`ddl-auto: create-drop`, `flyway.enabled: false`).
  - *(Toda autenticación opera nativamente mediante JWT HMAC-SHA256 y Argon2id sin proveedores OIDC externos; la audiencia `platform-backoffice` aísla el Backoffice).*
- **Errores RFC 7807:** `GlobalExceptionHandler` (`@RestControllerAdvice`) mapea `DomainException` (`EntityNotFoundException`, `BusinessRuleException`, `ResourceConflictException`, `ValidationException`, `UnauthorizedOperationException`, `InsufficientStockException`, `ItemSoldOutException`, `ShiftRequiredException`, `PayloadTooLargeException`, `EntitlementBlockedException`) a `ProblemDetail` con `errorCode`, `timestamp` y `tenantId`.

---

## 2. Estructura Modular (`com.restaurant.app`) y Contratos `.api` Verificados

La verificación `ApplicationModules.of(RestaurantApplication.class).verify()` (`RestaurantModularityTests`) prohíbe dependencias circulares e inyección directa de repositorios ajenos.

### 2.1 Núcleo `core.*`
| Paquete | Contratos Públicos / Servicios Clave | Responsabilidad |
| :--- | :--- | :--- |
| **`core.common`** | `BaseTenantEntity`, `BaseBranchEntity`, `GlobalExceptionHandler`, `core.common.exception.*` | Infraestructura transversal, excepciones tipadas y servicio estático de `/uploads/**` (`WebConfig`) |
| **`core.security.api`** | `UserPublicApi`, `RolePublicApi`, `SessionPublicApi`, `PasswordChangePolicyPort` | CRUD usuarios/perfiles/PIN (`ProfileController`, `UserController`), roles dinámicos y matriz de permisos (`RoleController`), rotación de refresh tokens (`AuthController`), `countUsersByTenant` y `findFirstTenantLoginAt` |
| **`core.config.api`** | `PlatformSettingsService`, `TenantConfigService`, `BranchConfigService` | Configuración global de plataforma (`platform_settings`) y overrides heredados; `FlywayConfig` |
| **`core.entitlements.api`** | `TenantEntitlementService`, `TenantEntitlementPort`, `TenantEntitlementChangedEvent` | Caché O(1) de suscripciones/cuotas (`assertLoginAllowed`, `assertPosAndShiftOperationsAllowed`, `assertQuotaAvailable`, `getEntitlementView`, `evict`). Implementado por `TenantEntitlementPortImpl` en `modules.backoffice.internal` |
| **`core.storage.api`** | `MediaStorageService` (`storeDishImage`, `deleteByUrl`, `getActiveProvider`, `getMaxFileSizeBytes`, `testActiveConnection`) | Almacenamiento agnóstico con `LocalStorageStrategy` (`./uploads/dishes/`, `/uploads/dishes/`) y `S3CompatibleStorageStrategy` (OCI/S3/R2/MinIO), validación MIME/magic-bytes y límite por defecto `1 MB` ([ADR-0028](../adr/0028_agnostic_media_storage_and_platform_settings.md)) |

### 2.2 Módulos de Negocio `modules.*`
| Módulo | API Pública `.api` / Eventos | Controladores REST |
| :--- | :--- | :--- |
| **`modules.company`** | `CompanyPublicApi`, `BranchPublicApi`, `OnboardingPublicApi`, `TenantOnboardedEvent` | `CompanyController` (`/api/v1/company`, `/entitlements`), `BranchController` (`/api/v1/branches`), `OnboardingController` (`/api/v1/auth/onboarding`, `/check-slug`, `/onboarding/check-availability` y **`POST /api/v1/backoffice/tenants`**) |
| **`modules.configuration`** | `ConfigurationPublicApi`, `SystemParameter` (20 parámetros), `ParameterUpdatedEvent` | `ConfigurationController` (`/api/v1/parameters` con `ETag` SHA-256, `PATCH /{key}`, `DELETE /{key}`) |
| **`modules.catalog`** | `CatalogPublicApi`, `DailyMenuPublicApi` (`BranchServiceSessionDto`, `ShiftMenuDto`, `ComboDto`, `ModifierGroupDto`) | `CatalogController` (`/api/v1/catalog/{categories,ingredients,dishes,combos,modifier-groups}`), `DailyMenuController` (`/api/v1/catalog/{menus,service-sessions,shifts}`) |
| **`modules.inventory`** | `InventoryPublicApi` (`getAvailableStock`, `getBranchStockSummary`, `registerInitialStock`, `adjustStock`, `getKardexMovements`, `getTenantInventoryMetrics`) | `InventoryController` (`/api/v1/inventory/{stock,kardex,stock/adjust,stock/initial}`) |
| **`modules.pos`** | `PosPublicApi` (`placeOrder`, `cancelOrder`, `getShiftSalesSummary`, `countActiveOrdersInRange`), `OrderPaidEvent`, `OrderCancelledEvent` | `OrderController` (`/api/v1/pos/orders`, `/sync`, `/{orderId}/cancel`), `PromotionController` (`/api/v1/pos/promotions`, `/active`) |
| **`modules.finance`** | `FinancePublicApi` (`openShift`, `findActiveShift`, `findActiveShiftForUser`, `listActiveShifts`, `recordSale`, `recordRefund`, `validateSupervisorPin`, `createAuthorizationRequest`, `approveAuthorization`) | `FinanceController` (`/api/v1/finance/{shifts,expenses}`, `/shifts/history`, `/shifts/{id}/audit-detail`), `AuthorizationController` (`/api/v1/finance/authorizations`, `/validate-pin`, `/{id}/approve`, `/{id}/reject`) |
| **`modules.backoffice`** | Implementa `TenantEntitlementPort` + `BackofficeTelemetryListener` | `/api/v1/backoffice/**` (ver [02_backoffice_backend_api_spec.md](../backoffice/sdd/02_backoffice_backend_api_spec.md)) |
| **`modules.analytics`** | *(Stub `package-info.java`)* | Sin endpoints activos (planificado en `PB-024`) |

---

## 3. Catálogo Real de los 20 Parámetros (`modules.configuration`)

Definidos tipadamente en `SystemParameter.java` y enriquecidos con textos, opciones y ayuda contextual (`help.impact`, `help.example`) desde `backend/src/main/resources/config/parameters-catalog.json` ([ADR-0008](../adr/0008_unified_multilevel_parameter_system.md), [ADR-0025](../adr/0025_parameter_console_humanization_json_catalog_and_contextual_help.md)):

| Clave (`SystemParameter`) | Tipo | Ámbito | Default | Uso / Estado Real |
| :--- | :---: | :---: | :---: | :--- |
| `inventory.deduction_mode` | `ENUM` | `TENANT` | `AUTOMATIC_PERMISSIVE` | Activo en `KardexService` (`AUTOMATIC_PERMISSIVE`, `AUTOMATIC_STRICT`, `MANUAL_ISOLATED`) |
| `inventory.allow_negative_stock` | `BOOLEAN` | `TENANT` | `false` | Activo en deducción de recetas/stock (`dishes.allow_negative_stock_override` lo sobrescribe por plato) |
| `pos.display_dish_images` | `BOOLEAN` | `BRANCH` | `true` | Activo en `PosComponent` (vista `'grid'` vs `'compact'`) |
| `pos.default_order_mode` | `ENUM` | `BRANCH` | `DINE_IN` | Activo en `PosComponent` y presets onboarding (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`) |
| `pos.allow_scheduled_pickup` | `BOOLEAN` | `BRANCH` | `true` | *Capacidad futura:* declarado en catálogo; `PosComponent` muestra `SCHEDULED_PICKUP` sin filtro condicional aún (`TASK-640`) |
| `pos.delivery_fee_enabled` | `BOOLEAN` | `BRANCH` | `false` | *Capacidad futura:* declarado en catálogo; `PosComponent` permite flete sin condicionarlo aún al parámetro (`TASK-640`) |
| `pos.delivery_fee_default` | `DOUBLE` | `BRANCH` | `0.00` | *Capacidad futura:* declarado en catálogo; pendiente de precarga en checkout `DELIVERY` (`TASK-640`) |
| `pos.cancellation_prepared_waste_policy` | `ENUM` | `BRANCH` | `ALLOW_AUTO_BUFFER_REUSE` | Activo en `OrderCancelledEvent` / `KardexService` (`ALLOW_AUTO_BUFFER_REUSE`, `STRICT_WASTE`, `ALLOW_BUFFER_REUSE`) |
| `finance.max_petty_cash_expense` | `DOUBLE` | `BRANCH` | `100.00` | Activo en `ExpenseService` (techo para gastos de gaveta sin autorización superior) |
| `finance.require_supervisor_pin_cancel` | `BOOLEAN` | `TENANT` | `true` | Activo en cancelación de comandas (`OrderServiceImpl`) |
| `finance.shift_opening_policy` | `ENUM` | `BRANCH` | `ALL_AUTHORIZED` | Activo en apertura de turno (`ALL_AUTHORIZED` \| `SUPERVISOR_ONLY`) |
| `finance.shift_concurrency_mode` | `ENUM` | `BRANCH` | `SINGLE_PER_BRANCH` | Activo (`SINGLE_PER_BRANCH` \| `INDIVIDUAL_PER_CASHIER`, [ADR-0016](../adr/0016_cash_control_separation_and_multi_cashier_shifts.md)) |
| `finance.expense_allow_self_approval` | `BOOLEAN` | `BRANCH` | `false` | Activo en `ExpenseService` (control dual de gastos) |
| `security.require_user_email` | `BOOLEAN` | `TENANT` | `true` | Activo en gestión de usuarios ERP |
| `security.enforce_password_change_on_reset` | `BOOLEAN` | `TENANT` | `true` | Activo en login/guard ERP para usuarios con `must_change_password` |
| `security.session_inactivity_timeout_minutes` | `INTEGER` | `TENANT` | `15` | *Capacidad futura:* seeded en onboarding y editable en consola; `IdleTimerService` usa default `30` min hasta cableado (`TASK-640`) |
| `company.currency` | `STRING` | `BRANCH` | `BOB` | Seeded en onboarding y configuración por sucursal |
| `company.timezone` | `STRING` | `BRANCH` | `America/La_Paz` | Identificador IANA por sucursal (`ADR-0006`) |
| `catalog.scope_mode` | `ENUM` | `TENANT` | `BRANCH_ISOLATED` | Activo en `CatalogServiceImpl` (`BRANCH_ISOLATED` \| `GLOBAL_SHARED`, [ADR-0030](../adr/0030_catalog_branch_isolation_by_default.md)) |
| `finance.auto_open_full_catalog_session` | `BOOLEAN` | `BRANCH` | `true` | Activo en `DailyMenuPublicApi.attachCashShiftToServiceSession` |

---

## 4. Capacidades Transaccionales Clave Implementadas

### 4.1 Jornadas de Servicio, Menús Diarios, Combos y Modificadores (`V4`, `V9`, `V11`, `V23`, `V24` — [ADR-0031](../adr/0031_branch_service_sessions_and_dynamic_combos.md))
- **Jornadas (`BranchServiceSession` en `branch_service_sessions`):** Desacopladas de `cash_shifts` para que administración configure el servicio del día (`FULL_CATALOG` o `DAILY_MENU`) compartido entre múltiples cajeros, con estados `CONFIGURED` $\rightarrow$ `ACTIVE` $\rightarrow$ `CLOSED`.
- **Menús Diarios y Cupos (`daily_menus`, `daily_menu_items`, `daily_menu_combos`, `shift_menu_items`):** Soportan `day_of_week` múltiple (`V24`), `allow_individual_sale`, `is_unlimited` (`V23`) y decremento atómico optimista (`@Version` + SQL condicional; lanza `ItemSoldOutException` HTTP 409).
- **Combos y Selecciones (`combos`, `combo_items`, `order_item_selections`):** Soportan renglones `FIXED`, `SPECIFIC_OPTIONS` y dinámicos `MENU_COURSE` por tiempo de comida, registrando las elecciones reales del cliente en `OrderItemSelection` (`order_item_selections`) para deducción exacta de inventario.
- **Modificadores y Estrategias de Inventario (`InventoryStrategyFactory` + `DeductionStrategy`):** `KardexService` deduce en `@ApplicationModuleListener` sobre `OrderPaidEvent` aplicando la estrategia del plato (`RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT`, `COMMERCIAL_NO_INVENTORY`), las reglas de saldo del tenant (`MANUAL_ISOLATED`, `AUTOMATIC_PERMISSIVE`, `AUTOMATIC_STRICT`) y el consumo de insumos de `order_item_modifiers`. *(Nota: la estrategia por plato se define en `POST/PUT /api/v1/catalog/dishes`, sin endpoint separado `PATCH /inventory-strategy`).*

### 4.2 POS, Turnos, Arqueos, Cancelaciones e Imágenes (`V10`, `V12`–`V16`)
- **Invariante de Turno y Cajero:** `OrderServiceImpl` exige turno `OPEN` vía `FinancePublicApi.findActiveShift` (HTTP 409 `ShiftRequiredException`), persiste el usuario creador en **`orders.created_by`** (proyectado como `cashierId`/`cashierName` en `OrderDto`) e incorpora `orderType`, `customerName`, `customerPhone`, `deliveryAddress`, `deliveryFee` y `deliveryNotes`.
- **Autorizaciones y Cancelaciones:** `AuthorizationController` (`shift_authorizations`) valida PIN de supervisor (`app_users.pin_hash`) o gestiona cola remota para gastos y anulaciones (`OrderCancelledEvent` + `prepared_dish_buffer` / `WASTE_ADJUSTMENT` / `total_cash_refunds`).
- **Histórico de Turnos:** `GET /api/v1/finance/shifts/history` (JPA Specification segura ante filtros nulos, `TASK-626`) con conversión horaria de sucursal (`ADR-0006`) y `GET /api/v1/finance/shifts/{id}/audit-detail`.
- **Imágenes y SKU:** `POST /api/v1/catalog/dishes/{id}/image` delega en `MediaStorageService` (`1 MB` máximo, `413 Payload Too Large` si excede) y `GET /api/v1/catalog/dishes/next-code` sugiere `${prefix}-${maxSeq + 1}`.
- **Onboarding (`OnboardingServiceImpl` / `OnboardingController`):** Soporta plantillas `FAST_FOOD`, `TRADITIONAL`, `CAFE`, `BAR`, `CAFE_BAR` y `EMPTY`; además `OnboardingController` expone `POST /api/v1/backoffice/tenants` delegando en `executeOnboarding(request, false)` para no marcar primer ingreso hasta el login real del restaurante (`V20`).
