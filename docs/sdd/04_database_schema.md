---
title: "04. Esquema de Base de Datos y Catálogo Maestro (PostgreSQL)"
category: "sdd"
tags:
  - sdd
  - database
  - postgresql
  - flyway
  - erd
  - multi-tenant
last_updated: "2026-11-17"
status: "verified"
---

# 04. Esquema de Base de Datos y Catálogo Maestro (PostgreSQL)

> [!NOTE]
> El DDL ejecutable canónico reside en `backend/src/main/resources/db/migration/V1__*.sql` a `V24__*.sql`. Para los diagramas visuales consulta:
> - **ERD Físico ERP & Sistema:** [database_erd.md](../project/architecture/02-domain-and-data/database_erd.md)
> - **ERD y Diccionario Backoffice (`V17`–`V22`):** [03_backoffice_database_schema.md](../backoffice/sdd/03_backoffice_database_schema.md) y [domain_and_erd_subscriptions.md](../backoffice/architecture/domain_and_erd_subscriptions.md)

## 1. Convenciones Globales

- **Motor:** PostgreSQL 16+ (H2 en modo PostgreSQL para pruebas unitarias/integración bajo perfil `test`).
- **Claves Primarias:** `UUID` con `gen_random_uuid()`.
- **Precisión Numérica:** Moneda en `NUMERIC(12, 2)` (`NUMERIC(10, 2)` en `delivery_fee`, `NUMERIC(14, 2)` en telemetría agregada); cantidades de insumos y costos unitarios en `NUMERIC(12, 4)`.
- **Fechas y Horas (`ADR-0006`):** Fechas calendario puras en `DATE` (`expenses.expense_date`, `branch_service_sessions.service_date`, `tenant_daily_telemetry.stat_date`, `subscription_charges.due_date`) y timestamps transaccionales en `TIMESTAMPTZ` UTC.
- **Concurrencia y Tenancy:** `@Version` (`version BIGINT DEFAULT 0`) en entidades críticas; denormalización de `tenant_id` y `branch_id` en tablas operativas con índices compuestos.

---

## 2. Catálogo Maestro de las 56 Tablas (`V1`–`V24`)

### A. Tenancy, Seguridad, RBAC y Parametría (12 tablas)
| Tabla | Migración Origen | Propósito Principal |
| :--- | :---: | :--- |
| `tenants` | `V1` (`V3`, `V6`) | Empresas registradas (`slug` único, `nit_or_tax_id` nullable con índice único parcial, `logo_url`, `is_active`) |
| `branches` | `V1` (`V6`) | Sucursales físicas, coordenadas opcionales, `currency` (`BOB`) y `timezone` IANA (`America/La_Paz`) |
| `app_users` | `V1` (`V7`, `V12`, `V16`, `V20`) | Cuentas de usuario (`username` único por tenant, `email` nullable, `role_id`, `role_valid_until`, `must_change_password`, `pin_hash`, `phone_number`, `last_login_at`) |
| `user_preferences` | `V1` | Preferencias por usuario (`theme`, `language`) |
| `roles` | `V3` | Roles dinámicos y de sistema por tenant (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`, custom) |
| `role_permissions` | `V3` | Relación de permisos asignados a cada rol |
| `permissions` | `V7` | Catálogo maestro de permisos atómicos del sistema |
| `user_branches` | `V7` | Asignación `N:M` de colaboradores rotativos a sucursales autorizadas |
| `refresh_tokens` | `V8` | Sesiones ERP con hash SHA-256, rotación (`replaced_by_id`) y revocación remota |
| `system_parameter_values` | `V5` | Sparse storage de overrides (`GLOBAL`/`TENANT`/`BRANCH`), `updated_by` y `audit_reason` |
| `tenant_settings` | `V1` | Tabla legada clave-valor por tenant *(mantenida por compatibilidad; reemplazada operativamente por `system_parameter_values`)* |
| `branch_settings` | `V1` | Tabla legada clave-valor por sucursal *(reemplazada operativamente por `system_parameter_values`)* |

### B. Catálogo, Modificadores, Combos y Menús Diarios (18 tablas)
| Tabla | Migración Origen | Propósito Principal |
| :--- | :---: | :--- |
| `categories` | `V1` (`V15`) | Categorías del menú por tenant (`icon`, `sort_order`, `code_prefix` único por tenant) |
| `ingredients` | `V1` | Insumos base (`unit_of_measure`, `min_stock_alert`) |
| `dishes` | `V1` (`V9`, `V14`) | Platos maestros (`branch_id = NULL`) o exclusivos de sucursal, `cloned_from_id`, `inventory_strategy`, `allow_negative_stock_override`, `image_url` |
| `dish_recipes` | `V1` | Ficha técnica de insumos por plato (BOM) |
| `branch_dishes` | `V1` | Disponibilidad y `price_override` de platos maestros por sucursal |
| `combos` | `V4` | Paquetes/combos con precio promocional |
| `combo_items` | `V4` (`V23`) | Bloques de combo (`selection_source`: `FIXED` \| `SPECIFIC_OPTIONS` \| `MENU_COURSE`, `course_type`) |
| `combo_item_options` | `V4` | Platos elegibles y recargos en bloques `SPECIFIC_OPTIONS` |
| `modifier_groups` | `V4` | Grupos de extras/modificadores (`min_selectable`, `max_selectable`, `is_required`) |
| `dish_modifier_groups` | `V4` | Asignación de grupos de modificadores a platos |
| `modifiers` | `V4` (`V11`) | Opciones individuales con precio adicional e impacto opcional a ingrediente (`ingredient_id`, `quantity_consumed`) |
| `daily_menus` | `V9` (`V24`) | Plantillas de menú diario (`day_of_week VARCHAR(120)`, `price_mode`, `package_price`) |
| `daily_menu_items` | `V9` (`V23`) | Platos por tiempo (`course_type`), cupo sugerido, `allow_individual_sale`, `is_unlimited` |
| `daily_menu_combos` | `V23` | Combos habilitados dentro de una plantilla de menú del día |
| `branch_service_sessions` | `V23` | Jornada operativa diaria por sucursal (`FULL_CATALOG` \| `DAILY_MENU`, `CONFIGURED` \| `ACTIVE` \| `CLOSED`) |
| `shift_menu_assignments` | `V9` (`V23`) | Vínculo del menú instanciado al turno o a `branch_service_sessions` |
| `shift_menu_items` | `V9` (`V23`) | Cupos en vivo con bloqueo optimista `@Version`, `is_sold_out`, `allow_individual_sale`, `is_unlimited` |
| `shift_menu_quota_adjustments` | `V9` | Bitácora auditada de ampliaciones/reducciones de cupo en vivo |

### C. Inventario, Compras y Kardex (5 tablas)
| Tabla | Migración Origen | Propósito Principal |
| :--- | :---: | :--- |
| `branch_stock` | `V1` (`V9`) | Existencia física y costo promedio ponderado por sucursal (soporta `ingredient_id` o `dish_id` según estrategia) |
| `kardex_movements` | `V1` (`V9`, `V11`, `V12`) | Libro mayor inmutable de entradas, salidas (`SALE_OUT`, `SALE_CONSUMPTION`), mermas (`WASTE_ADJUSTMENT`), reversiones (`CANCELLATION_REVERSAL`) y ajustes |
| `prepared_dish_buffer` | `V12` | Buffer de platos ya preparados reutilizables tras cancelación (`ALLOW_AUTO_BUFFER_REUSE`) |
| `purchases` | `V1` | *Capacidad futura:* cabecera de compras a proveedores (sin UI/servicio activo aún; `PB-021` / `TASK-636`) |
| `purchase_items` | `V1` | *Capacidad futura:* detalle de compras a proveedores (sin UI/servicio activo aún) |

### D. POS, Promociones, Finanzas y Eventos (12 tablas)
| Tabla | Migración Origen | Propósito Principal |
| :--- | :---: | :--- |
| `orders` | `V1` (`V10`, `V12`, `V13`) | Comandas con `client_transaction_id`, `cash_shift_id`, **`created_by`** (cajero), `order_type`, datos de entrega/delivery, `delivery_fee` y metadatos de anulación |
| `order_items` | `V1` (`V23`) | Líneas de pedido (`dish_id` nullable para combos/paquetes, `combo_id`, `daily_menu_id`, `notes`, `discount_amount`) |
| `order_item_modifiers` | `V4` (`V11`) | Snapshot de modificadores cobrados y su consumo de insumo |
| `order_item_selections` | `V23` | Selecciones concretas elegidas dentro de combos dinámicos o menús armables |
| `promotions` | `V4` | Reglas de descuentos y horarios Happy Hour |
| `promotion_branches` | `V4` | Sucursales habilitadas por promoción |
| `cash_shifts` | `V1` (`V10`, `V12`, `V23`) | Turnos de caja (`service_session_id`, totales por medio de pago, `total_cash_refunds`, arqueo y consolidación) |
| `expenses` | `V1` | Egresos operativos diarios y gastos de caja menor (`expense_date`, `approval_status`, `evidence_url`) |
| `shift_authorizations` | `V12` | Solicitudes de autorización (`EXPENSE` \| `ORDER_CANCELLATION`) con decisión in-situ por PIN o remota |
| `audit_logs` | `V1` | Tabla reservada para auditoría transversal ERP (`core.audit`) |
| `event_publication` | `V2` | Outbox transaccional de eventos Spring Modulith |
| `event_publication_archive` | `V2` | Histórico de eventos completados de Spring Modulith |

### E. Backoffice SaaS y Plataforma (9 tablas — `V17`–`V19`)
| Tabla | Migración Origen | Propósito Principal |
| :--- | :---: | :--- |
| `platform_admins` | `V17` (`V19`, `V22`) | Operadores y superadministradores de plataforma (`role`, `email` nullable único parcial) |
| `platform_refresh_tokens` | `V17` | Sesiones de plataforma con SHA-256 y cadena `replaced_by_id` |
| `platform_settings` | `V18` (`V22`) | Parámetros globales de storage y política de contraseñas (`updated_by`, `version`) |
| `subscription_plans` | `V19` (`V21`) | Catálogo de planes SaaS (`plan_category`, `duration_days >= 0`, límites base) |
| `tenant_subscriptions` | `V19` | Suscripción vigente `1:1` por tenant, gracia condicional, overrides y modo de suspensión |
| `subscription_charges` | `V19` | Cargos exigibles por ciclo (`PENDING`, `PARTIAL`, `PAID`, `WAIVED`, `due_date`) |
| `subscription_payments` | `V19` | Pagos registrados y conciliación FIFO |
| `tenant_daily_telemetry` | `V19` | Read-model CQRS de ventas y cancelaciones diarias por sucursal |
| `platform_audit_logs` | `V19` | Bitácora inmutable de plataforma (`details_json TEXT`) |

*(Conteo total verificado contra `backend/src/main/resources/db/migration/V*.sql`: **12 + 18 + 5 + 12 + 9 = 56 tablas**).*

---

## 3. Historial de Migraciones Flyway (`V1`–`V24`)

| Rango | Migraciones y Alcance |
| :--- | :--- |
| **`V1`–`V4`** | Esquema fundacional ERP (`V1`), outbox Spring Modulith (`V2`), slug y RBAC base (`V3`), combos/promociones/modificadores (`V4`) |
| **`V5`–`V8`** | Parametría sparse (`V5`), onboarding/NIT opcional (`V6`), matriz de permisos y `user_branches` (`V7`), `refresh_tokens` (`V8`) |
| **`V9`–`V12`** | Menús diarios e inventario flexible (`V9`), modalidades iniciales/consolidación (`V10`), modificadores en Kardex (`V11`), cancelaciones, PIN y autorizaciones (`V12`) |
| **`V13`–`V16`** | Delivery fee y `SCHEDULED_PICKUP` (`V13`), `dishes.image_url` (`V14`), `categories.code_prefix` (`V15`), `app_users.phone_number` (`V16`) |
| **`V17`–`V22`** | Plataforma Backoffice: `platform_admins` y tokens (`V17`), `platform_settings` (`V18`), suscripciones/pagos/telemetría/auditoría (`V19`), `app_users.last_login_at` (`V20`), categorías/ilimitados en planes (`V21`), email opcional y política de claves (`V22`) |
| **`V23`–`V24`** | Jornadas `branch_service_sessions`, combos `MENU_COURSE`, `daily_menu_combos` y `order_item_selections` (`V23`); ampliación `daily_menus.day_of_week VARCHAR(120)` (`V24`) |
