---
title: "Modelo Entidad-Relación de Base de Datos (PostgreSQL 16+)"
category: "database"
tags: ["erd", "postgresql", "flyway", "v1-v24", "multi-tenant", "kardex", "pos", "rbac", "daily-menus"]
last_updated: "2026-11-17"
status: "verified"
---

# Modelo Entidad-Relación de Base de Datos (PostgreSQL 16+)

Esquema relacional completo del sistema **Restaurante ERP SaaS**, verificado directamente contra las migraciones Flyway `V1`–`V24`. La base de datos comprende **56 tablas totales**: **47 tablas ERP/Sistema** detalladas en este documento y **9 tablas de Plataforma/Suscripciones Backoffice** (`V17`–`V22`), documentadas en [ERD Backoffice y Suscripciones](../../../backoffice/architecture/domain_and_erd_subscriptions.md).

## Resumen de Evolución Flyway (`V1`–`V24`)

- **Esquema base e infraestructura:** [`V1__init_schema.sql`](../../../../backend/src/main/resources/db/migration/V1__init_schema.sql), [`V2__spring_modulith_events.sql`](../../../../backend/src/main/resources/db/migration/V2__spring_modulith_events.sql) (`event_publication`, `event_publication_archive`).
- **Tenancy, Onboarding, RBAC, Sesión y Parámetros:** [`V3__tenant_slug_and_rbac.sql`](../../../../backend/src/main/resources/db/migration/V3__tenant_slug_and_rbac.sql), [`V5__system_parameter_values.sql`](../../../../backend/src/main/resources/db/migration/V5__system_parameter_values.sql), [`V6__onboarding_and_tax_info.sql`](../../../../backend/src/main/resources/db/migration/V6__onboarding_and_tax_info.sql), [`V7__rbac_dynamic_roles_and_privileges.sql`](../../../../backend/src/main/resources/db/migration/V7__rbac_dynamic_roles_and_privileges.sql), [`V8__session_refresh_tokens.sql`](../../../../backend/src/main/resources/db/migration/V8__session_refresh_tokens.sql), [`V16__user_phone_number.sql`](../../../../backend/src/main/resources/db/migration/V16__user_phone_number.sql), [`V20__tenant_first_login_tracking.sql`](../../../../backend/src/main/resources/db/migration/V20__tenant_first_login_tracking.sql).
- **Catálogo, Inventario Flexible, Menús del Día y Jornadas:** [`V4__inventory_combos_promotions_modifiers.sql`](../../../../backend/src/main/resources/db/migration/V4__inventory_combos_promotions_modifiers.sql), [`V9__daily_menus_and_flexible_inventory.sql`](../../../../backend/src/main/resources/db/migration/V9__daily_menus_and_flexible_inventory.sql), [`V11__modifiers_kardex_consumption.sql`](../../../../backend/src/main/resources/db/migration/V11__modifiers_kardex_consumption.sql), [`V14__dish_image_url.sql`](../../../../backend/src/main/resources/db/migration/V14__dish_image_url.sql), [`V15__category_code_prefix.sql`](../../../../backend/src/main/resources/db/migration/V15__category_code_prefix.sql), [`V23__service_sessions_combo_menus_and_order_selections.sql`](../../../../backend/src/main/resources/db/migration/V23__service_sessions_combo_menus_and_order_selections.sql), [`V24__widen_daily_menus_day_of_week.sql`](../../../../backend/src/main/resources/db/migration/V24__widen_daily_menus_day_of_week.sql).
- **POS, Modalidades, Cancelaciones y Turnos:** [`V10__order_type_customer_name_and_shift_consolidation.sql`](../../../../backend/src/main/resources/db/migration/V10__order_type_customer_name_and_shift_consolidation.sql), [`V12__order_cancellations_and_shift_authorizations.sql`](../../../../backend/src/main/resources/db/migration/V12__order_cancellations_and_shift_authorizations.sql), [`V13__service_modalities_delivery_fee_and_pickup.sql`](../../../../backend/src/main/resources/db/migration/V13__service_modalities_delivery_fee_and_pickup.sql).
- **Backoffice SaaS (9 tablas en documento hermano):** [`V17`](../../../../backend/src/main/resources/db/migration/V17__platform_admins_and_sessions.sql), [`V18`](../../../../backend/src/main/resources/db/migration/V18__platform_settings_storage.sql), [`V19`](../../../../backend/src/main/resources/db/migration/V19__backoffice_subscriptions_telemetry_and_audit.sql), [`V21`](../../../../backend/src/main/resources/db/migration/V21__subscription_plans_category_and_unlimited.sql), [`V22`](../../../../backend/src/main/resources/db/migration/V22__platform_admin_optional_email_and_password_policy.sql).

---

## 1. Tenancy, Configuración Dinámica, Seguridad RBAC y Sesiones (12 Tablas)

```mermaid
erDiagram
    tenants ||--o{ tenant_settings : "configura"
    tenants ||--o{ branches : "opera"
    tenants ||--o{ roles : "define"
    tenants ||--o{ app_users : "emplea"
    tenants ||--o{ system_parameter_values : "parametriza"

    branches ||--o{ branch_settings : "personaliza"
    branches ||--o{ app_users : "sucursal_base"
    branches ||--o{ user_branches : "asigna"
    branches ||--o{ system_parameter_values : "sobreescribe"

    roles ||--o{ role_permissions : "agrupa"
    roles ||--o{ app_users : "asigna_a"
    permissions ||--o{ role_permissions : "catalogo"

    app_users ||--|| user_preferences : "preferencias"
    app_users ||--o{ user_branches : "acceso_multi_sucursal"
    app_users ||--o{ refresh_tokens : "emite"

    tenants {
        uuid id PK
        varchar legal_name
        varchar slug UK "V3"
        varchar nit_or_tax_id "Nullable V6, UK parcial"
        text logo_url "V6"
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }

    tenant_settings {
        uuid id PK
        uuid tenant_id FK
        varchar setting_key
        text setting_value
        varchar value_type "STRING|BOOLEAN|NUMBER|JSON"
        varchar description
        timestamptz updated_at
    }

    branches {
        uuid id PK
        uuid tenant_id FK
        varchar name
        text address
        varchar phone
        varchar timezone "Default America/La_Paz"
        numeric latitude "V6"
        numeric longitude "V6"
        varchar currency "V6 Default BOB"
        boolean is_active
        timestamptz created_at
        bigint version
    }

    branch_settings {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        varchar setting_key
        text setting_value
        varchar value_type
        varchar description
        timestamptz updated_at
    }

    roles {
        uuid id PK
        uuid tenant_id FK "Nullable para sistema"
        varchar code "UK con tenant_id"
        varchar name
        varchar description
        boolean is_system
        timestamptz created_at
    }

    permissions {
        varchar code PK "V7 ej. pos:access"
        varchar name
        varchar description
        varchar module
    }

    role_permissions {
        uuid id PK
        uuid tenant_id FK
        uuid role_id FK
        varchar permission
        timestamptz created_at
    }

    app_users {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        varchar username "UK con tenant_id"
        varchar email "Nullable"
        varchar full_name "V6"
        varchar phone_number "V16"
        varchar password_hash
        varchar pin_hash "V12 BCrypt supervisor"
        varchar role "V7 VARCHAR(50)"
        uuid role_id FK "V7"
        timestamptz role_valid_until "V7"
        boolean must_change_password "V7"
        boolean email_verified
        boolean is_active
        timestamptz last_login_at "V20"
        timestamptz created_at
        bigint version
    }

    user_preferences {
        uuid user_id PK,FK
        varchar theme "LIGHT|DARK"
        varchar language
        boolean onboarding_tour_completed "V6"
    }

    user_branches {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        uuid branch_id FK
        timestamptz created_at
    }

    refresh_tokens {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        varchar token_hash UK
        varchar device_info
        varchar ip_address
        timestamptz expires_at
        timestamptz revoked_at
        uuid replaced_by_id FK
        timestamptz created_at
    }

    system_parameter_values {
        uuid id PK
        varchar parameter_key
        varchar scope "GLOBAL|TENANT|BRANCH"
        uuid tenant_id FK
        uuid branch_id FK
        text parameter_value
        timestamptz updated_at
        uuid updated_by FK
        varchar audit_reason
    }
```

---

## 2. Catálogo, Recetas, Modificadores, Combos, Menús y Jornadas (15 Tablas)

```mermaid
erDiagram
    categories ||--o{ dishes : "clasifica"
    categories ||--o{ combos : "clasifica"

    dishes ||--o{ dish_recipes : "compuesto_por"
    ingredients ||--o{ dish_recipes : "insumo"
    dishes ||--o{ branch_dishes : "override_sucursal"
    dishes ||--o{ dish_modifier_groups : "permite"
    modifier_groups ||--o{ dish_modifier_groups : "asocia"
    modifier_groups ||--o{ modifiers : "contiene"
    ingredients ||--o{ modifiers : "descuenta_opcional"

    combos ||--o{ combo_items : "estructura"
    combo_items ||--o{ combo_item_options : "opciones"
    dishes ||--o{ combo_items : "default_dish"
    dishes ||--o{ combo_item_options : "opcion_plato"

    daily_menus ||--o{ daily_menu_items : "incluye_platos"
    daily_menus ||--o{ daily_menu_combos : "habilita_combos"
    dishes ||--o{ daily_menu_items : "referencia"
    combos ||--o{ daily_menu_combos : "referencia"
    daily_menus ||--o{ branch_service_sessions : "asignado_a_jornada"

    categories {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar code_prefix "V15 UK con tenant_id"
        varchar icon
        int sort_order
        timestamptz created_at
    }

    ingredients {
        uuid id PK
        uuid tenant_id FK
        varchar name
        varchar unit_of_measure "KG|GRAM|LITER|ML|UNIT"
        numeric min_stock_alert
        timestamptz created_at
    }

    dishes {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK "NULL=Maestro"
        uuid cloned_from_id FK
        uuid category_id FK
        varchar code
        varchar name
        text description
        numeric sale_price
        varchar image_url "V14"
        varchar inventory_strategy "V9 RECIPE_BOM_STRICT|DISH_AS_INVENTORY_UNIT|COMMERCIAL_NO_INVENTORY"
        boolean allow_negative_stock_override "V9"
        boolean is_active
        timestamptz created_at
        bigint version
    }

    dish_recipes {
        uuid id PK
        uuid dish_id FK
        uuid ingredient_id FK
        numeric quantity
    }

    branch_dishes {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid dish_id FK
        boolean is_available
        numeric price_override
        timestamptz created_at
        bigint version
    }

    modifier_groups {
        uuid id PK
        uuid tenant_id FK
        varchar name
        int min_selectable
        int max_selectable
        boolean is_required
        timestamptz created_at
    }

    dish_modifier_groups {
        uuid id PK
        uuid dish_id FK
        uuid group_id FK "V4 nombre real group_id"
        int sort_order
    }

    modifiers {
        uuid id PK
        uuid tenant_id FK
        uuid group_id FK
        varchar name
        numeric additional_price
        uuid ingredient_id FK
        numeric quantity_consumed
        boolean is_active
        int sort_order
    }

    combos {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid category_id FK
        varchar code
        varchar name
        text description
        numeric base_price
        boolean is_active
        timestamptz created_at
        bigint version
    }

    combo_items {
        uuid id PK
        uuid combo_id FK
        varchar name
        varchar selection_mode "FIXED|CHOICE"
        varchar selection_source "V23 FIXED|SPECIFIC_OPTIONS|MENU_COURSE"
        varchar course_type "V23"
        boolean is_required
        uuid default_dish_id FK
        int quantity
        int sort_order
    }

    combo_item_options {
        uuid id PK
        uuid combo_item_id FK
        uuid dish_id FK
        numeric price_adjustment
        boolean is_default
    }

    daily_menus {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        varchar name
        text description
        varchar day_of_week "V24 VARCHAR(120)"
        varchar price_mode
        numeric package_price
        boolean is_template
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }

    daily_menu_items {
        uuid id PK
        uuid menu_id FK
        uuid dish_id FK
        varchar course_type
        int suggested_quota
        numeric override_price
        boolean is_mandatory_in_package
        boolean allow_individual_sale "V23"
        boolean is_unlimited "V23"
        int sort_order
    }

    daily_menu_combos {
        uuid id PK
        uuid menu_id FK
        uuid combo_id FK
        int sort_order
    }

    branch_service_sessions {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        date service_date "ADR-0006 DATE"
        varchar menu_mode "FULL_CATALOG|DAILY_MENU"
        uuid daily_menu_id FK
        varchar status "CONFIGURED|ACTIVE|CLOSED"
        uuid configured_by FK
        text notes
        timestamptz created_at
        timestamptz activated_at
        timestamptz closed_at
        bigint version
    }
```

---

## 3. POS, Promociones, Turnos de Caja, Gastos y Menú Activo (12 Tablas)

```mermaid
erDiagram
    branch_service_sessions ||--o{ cash_shifts : "agrupa_turnos_V23"
    branch_service_sessions ||--o| shift_menu_assignments : "define_menu_jornada"
    cash_shifts ||--o| shift_menu_assignments : "legacy_cash_shift_nullable"
    shift_menu_assignments ||--o{ shift_menu_items : "cupos_en_vivo"
    shift_menu_items ||--o{ shift_menu_quota_adjustments : "audita_ajustes"

    cash_shifts ||--o{ orders : "registra_ventas"
    cash_shifts ||--o{ expenses : "egresos_caja"
    cash_shifts ||--o{ shift_authorizations : "solicitudes"

    orders ||--o{ order_items : "contiene"
    order_items ||--o{ order_item_modifiers : "extras"
    order_items ||--o{ order_item_selections : "selecciones_combo_V23"
    promotions ||--o{ promotion_branches : "aplica_en_sucursal"

    orders {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid cash_shift_id FK
        varchar ticket_number
        varchar order_status "DRAFT..PAID|CANCELLED"
        varchar payment_method "CASH|CARD|QR|TRANSFER"
        numeric total_amount
        uuid client_transaction_id UK
        varchar order_type "V10+V13 DINE_IN|TAKEOUT|DELIVERY|SCHEDULED_PICKUP"
        varchar customer_name "V10"
        varchar customer_phone "V13"
        text delivery_address "V13"
        numeric delivery_fee "V13"
        text delivery_notes "V13"
        varchar cancellation_reason "V12"
        text cancellation_notes "V12"
        timestamptz cancelled_at "V12"
        uuid cancelled_by FK "V12"
        uuid cancellation_authorized_by FK "V12"
        varchar refund_method "V12 CASH|DIGITAL"
        varchar waste_policy_applied "V12"
        text notes
        uuid created_by FK
        timestamptz created_at
        timestamptz closed_at
        bigint version
    }

    order_items {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid order_id FK
        uuid dish_id FK "Nullable V23 para combos"
        uuid combo_id FK "V4"
        uuid daily_menu_id FK "V23"
        int quantity
        numeric unit_price
        numeric discount_amount "V4"
        numeric subtotal
        text notes
    }

    order_item_modifiers {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid order_item_id FK
        uuid modifier_id FK
        varchar modifier_name
        numeric unit_price
        uuid ingredient_id FK
        numeric quantity_consumed
    }

    order_item_selections {
        uuid id PK
        uuid order_item_id FK
        varchar slot_label
        uuid selected_dish_id FK
        int quantity
        numeric price_adjustment
    }

    promotions {
        uuid id PK
        uuid tenant_id FK
        varchar name
        text description
        varchar discount_type "PERCENTAGE|FIXED_AMOUNT"
        numeric discount_value
        varchar scope "GLOBAL|CATEGORY|DISH"
        uuid target_category_id FK
        uuid target_dish_id FK
        varchar application_mode "AUTOMATIC|MANUAL"
        varchar days_of_week
        time start_time
        time end_time
        date start_date "ADR-0006 DATE"
        date end_date "ADR-0006 DATE"
        numeric min_order_amount
        boolean is_active
        timestamptz created_at
        bigint version
    }

    promotion_branches {
        uuid id PK "V4"
        uuid promotion_id FK
        uuid branch_id FK
    }

    cash_shifts {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid service_session_id FK "V23"
        uuid opened_by FK
        uuid closed_by FK
        timestamptz opened_at
        timestamptz closed_at
        numeric initial_cash
        numeric total_cash_sales
        numeric total_card_sales
        numeric total_other_sales
        numeric total_cash_expenses
        numeric total_cash_refunds "V12"
        numeric expected_cash
        numeric actual_cash
        numeric difference
        varchar status "OPEN|CLOSED|CONSOLIDATED"
        uuid consolidated_by FK "V10"
        timestamptz consolidated_at "V10"
        text consolidation_notes "V10"
    }

    expenses {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid cash_shift_id FK
        varchar expense_type "DAILY_OPERATIONAL|MONTHLY_FIXED_PRORATED"
        text description
        numeric amount
        boolean paid_from_cash_drawer
        varchar approval_status "PENDING_APPROVAL|APPROVED|REJECTED"
        uuid approved_by FK
        text evidence_url
        text approval_notes
        date expense_date "ADR-0006 DATE"
        uuid created_by FK
        timestamptz created_at
    }

    shift_authorizations {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid cash_shift_id FK
        varchar request_type "EXPENSE|ORDER_CANCELLATION"
        uuid reference_id
        uuid requested_by FK
        numeric amount
        varchar status "PENDING|APPROVED|REJECTED"
        text justification
        uuid decided_by FK
        text decision_notes
        varchar decision_channel "IN_SITU_PIN|REMOTE_SESSION"
        timestamptz created_at
        timestamptz decided_at
    }

    shift_menu_assignments {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid service_session_id FK "V23 UK parcial"
        uuid cash_shift_id FK "Nullable V23"
        uuid menu_id FK
        uuid assigned_by FK
        timestamptz assigned_at
    }

    shift_menu_items {
        uuid id PK
        uuid shift_menu_assignment_id FK
        uuid dish_id FK
        varchar course_type
        int initial_quota
        int current_quota
        int sold_count
        numeric effective_price
        boolean is_sold_out
        boolean allow_individual_sale "V23"
        boolean is_unlimited "V23"
        bigint version
    }

    shift_menu_quota_adjustments {
        uuid id PK
        uuid shift_menu_item_id FK
        uuid adjusted_by FK
        int quantity_delta
        varchar reason
        timestamptz created_at
    }
```

---

## 4. Inventario Kardex, Buffer, Compras, Auditoría y Outbox Modulith (8 Tablas)

```mermaid
erDiagram
    branches ||--o{ branch_stock : "almacena"
    branches ||--o{ kardex_movements : "registra"
    branches ||--o{ prepared_dish_buffer : "vitrina_rescate"
    branches ||--o{ purchases : "abastece"
    purchases ||--o{ purchase_items : "detalla"
    orders ||--o{ prepared_dish_buffer : "origina_al_cancelar"

    branch_stock {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid ingredient_id FK "Nullable V9 CHECK XOR"
        uuid dish_id FK "V9 CHECK XOR"
        numeric current_quantity
        numeric average_unit_cost
        timestamptz last_updated
        bigint version
    }

    kardex_movements {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid ingredient_id FK "Nullable V9 CHECK XOR"
        uuid dish_id FK "V9 CHECK XOR"
        varchar movement_type "PURCHASE_IN|SALE_OUT|SALE_CONSUMPTION|WASTE_ADJUSTMENT|INITIAL_STOCK|CANCELLATION_REVERSAL..."
        numeric quantity
        numeric unit_cost
        numeric total_cost
        numeric balance_quantity
        uuid reference_id
        timestamptz movement_date
        uuid created_by FK
    }

    prepared_dish_buffer {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid order_id FK
        uuid dish_id FK
        int quantity
        timestamptz expires_at
        boolean is_consumed
        timestamptz created_at
    }

    purchases {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        varchar supplier_name
        varchar invoice_number
        numeric total_amount
        timestamptz purchase_date
        uuid created_by FK
        timestamptz created_at
        bigint version
    }

    purchase_items {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid purchase_id FK
        uuid ingredient_id FK
        numeric quantity
        numeric unit_cost
        numeric subtotal
    }

    audit_logs {
        uuid id PK
        uuid tenant_id FK
        uuid branch_id FK
        uuid user_id FK
        varchar action
        varchar entity_name
        uuid entity_id
        jsonb old_values
        jsonb new_values
        varchar ip_address
        timestamptz created_at
    }

    event_publication {
        uuid id PK
        varchar listener_id
        varchar event_type
        text serialized_event
        timestamptz publication_date
        timestamptz completion_date
        timestamptz last_resubmission_date
        int completion_attempts
        varchar status
    }

    event_publication_archive {
        uuid id PK
        varchar listener_id
        varchar event_type
        text serialized_event
        timestamptz publication_date
        timestamptz completion_date
        timestamptz last_resubmission_date
        int completion_attempts
        varchar status
    }
```

> [!NOTE]
> `purchases`, `purchase_items` y `audit_logs` existen en el esquema DDL desde `V1__init_schema.sql` como estructura base relacional, aunque la versión actual (Sprint 6) no implementa aún entidades JPA ni repositorios Java para dichos agregados. Adicionalmente, `branch_stock` y `kardex_movements` imponen las restricciones `chk_branch_stock_target` y `chk_kardex_target` (`ingredient_id` XOR `dish_id`) para soportar platos unitarios (`DISH_AS_INVENTORY_UNIT`).

## Enlaces Relacionados

- [ERD del Plano Backoffice (9 tablas V17–V22)](../../../backoffice/architecture/domain_and_erd_subscriptions.md)
- [Esquema Local Dexie.js (IndexedDB POS)](./dexie_local_schema.md)
- [Modelo de Dominio Java](./domain_model.md)
- [SDD Esquema de Base de Datos](../../../sdd/04_database_schema.md)
- [ADR-0006: Estándar de Fechas y Zonas Horarias](../../../adr/0006_datetime_and_timezone_handling.md)
- [ADR-0015: Menús Diarios e Inventario Flexible](../../../adr/0015_daily_menus_and_flexible_dish_inventory.md)
- [ADR-0031: Jornadas de Servicio y Combos Dinámicos](../../../adr/0031_branch_service_sessions_and_dynamic_combos.md)
