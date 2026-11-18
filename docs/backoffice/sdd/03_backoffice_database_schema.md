---
title: "03. Esquema de Base de Datos de Plataforma (V17–V22)"
category: "sdd"
module: "backoffice"
tags:
  - sdd
  - backoffice
  - postgresql
  - flyway
  - database
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 03. Esquema de Base de Datos de Plataforma (`V17`–`V22`)

> ⬅️ [Volver al Índice Backoffice](../README.md) | [ERD Mermaid](../architecture/domain_and_erd_subscriptions.md)

Las tablas de plataforma conviven en PostgreSQL 16 con aislamiento lógico estricto respecto a los datos transaccionales de restaurantes (`ADR-0027`, `ADR-0028`, `ADR-0029`).

---

## 1. Identidad y Configuración Base (`V17` y `V18`)
- **`V17__platform_admins_and_sessions.sql`:**
  - `platform_admins`: `id`, `username` (índice único `LOWER(TRIM(username))`), `email`, `full_name`, `password_hash`, `role` (`SUPERADMIN` | `PLATFORM_OPERATOR`, agregado en `V19`), `is_active`, `must_change_password`, `last_login_at`, `created_at`, `updated_at`, `version`.
  - `platform_refresh_tokens`: `id`, `admin_id -> platform_admins`, `token_hash` (SHA-256 único), `device_info`, `ip_address`, `expires_at`, `revoked_at`, **`replaced_by_id`** (`UUID REFERENCES platform_refresh_tokens(id) ON DELETE SET NULL`), `created_at`.
- **`V18__platform_settings_storage.sql`:**
  - `platform_settings`: `setting_key` (`VARCHAR(100) PK`), `setting_value` (`TEXT`), `value_type` (`STRING` | `BOOLEAN` | `NUMBER` | `JSON`), `description`, `updated_at` (`TIMESTAMPTZ`), **`updated_by`** (`UUID REFERENCES platform_admins(id) ON DELETE SET NULL`), **`version`** (`BIGINT NOT NULL DEFAULT 0`).

---

## 2. Motor Comercial, Suscripciones, Telemetría y Auditoría (`V19`)

### 2.1 `subscription_plans`
- `id` `UUID PK`, `code` `VARCHAR(50) UNIQUE NOT NULL`, `name` `VARCHAR(120) NOT NULL`, `description` `TEXT`
- `plan_category` `VARCHAR(30) NOT NULL DEFAULT 'PUBLIC_ONBOARDING' CHECK IN ('PUBLIC_ONBOARDING', 'TRIAL', 'CUSTOM_PRIVATE')` *(añadido en `V21`)*
- `billing_cycle` `VARCHAR(20) NOT NULL CHECK IN ('WEEKLY', 'MONTHLY', 'ANNUAL', 'CUSTOM')`
- `duration_days` `INTEGER NOT NULL CHECK (>= 0)` *(`0` = vigencia ilimitada, actualizado en `V21`)*
- `price` `NUMERIC(12, 2) NOT NULL CHECK (>= 0)`, `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`
- Cuotas por defecto (`NULL` = ilimitado): `max_branches`, `max_users`, `max_dishes`, `max_inventory_items`, `max_monthly_orders` (`INTEGER CHECK (>= 0)`)
- `is_active` `BOOLEAN NOT NULL DEFAULT TRUE`, `created_at`, `updated_at` (`TIMESTAMPTZ`), `version` `BIGINT NOT NULL DEFAULT 0`
- *(Semilla idempotente: `TRIAL_14D`, `WEEKLY_FLEX`, `MONTHLY_STANDARD`, `MONTHLY_PRO` editables desde Backoffice).*

### 2.2 `tenant_subscriptions`
Relación `1:1` con `tenants(id)`:
- `id` `UUID PK`, `tenant_id` `UUID UNIQUE NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`, `plan_id` `UUID REFERENCES subscription_plans(id) ON DELETE SET NULL`
- `status` (`TRIAL`, `ACTIVE`, `GRACE_PERIOD`, `SUSPENDED`, `CANCELLED`)
- `suspension_mode` (`NONE`, `POS_AND_SHIFT_ONLY`, `TOTAL`), `auto_suspend_mode_on_expire` (`POS_AND_SHIFT_ONLY`, `TOTAL`), `suspension_reason` `TEXT`
- `current_period_start` `TIMESTAMPTZ`, `current_period_end` `TIMESTAMPTZ`
- **Gracia condicional:** `grace_until` `TIMESTAMPTZ`, `grace_max_orders` `INTEGER CHECK (> 0)`, `grace_orders_used` `INTEGER NOT NULL DEFAULT 0`, `grace_notes` `TEXT`
- **Overrides por tenant (`NULL` hereda valor del plan):** `override_max_branches`, `override_max_users`, `override_max_dishes`, `override_max_inventory_items`, `override_max_monthly_orders`
- `updated_by_admin_id` `UUID REFERENCES platform_admins(id) ON DELETE SET NULL`, `created_at`, `updated_at` (`TIMESTAMPTZ`), `version` `BIGINT NOT NULL DEFAULT 0`

### 2.3 `subscription_charges`
- `id` `UUID PK`, `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`, `plan_id` `UUID REFERENCES subscription_plans(id) ON DELETE SET NULL`, `plan_name_snapshot` `VARCHAR(120) NOT NULL`
- `period_start` `TIMESTAMPTZ NOT NULL`, `period_end` `TIMESTAMPTZ NOT NULL`
- `amount_due` `NUMERIC(12, 2) NOT NULL CHECK (>= 0)`, `paid_amount` `NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (>= 0)`, `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`
- `charge_status` (`PENDING`, `PARTIAL`, `PAID`, `WAIVED`), `due_date` `DATE` (`ADR-0006`), `notes` `TEXT`
- `created_by_admin_id` `UUID REFERENCES platform_admins(id) ON DELETE SET NULL`, `created_at`, `updated_at` (`TIMESTAMPTZ`), `version` `BIGINT NOT NULL DEFAULT 0`

### 2.4 `subscription_payments`
- `id` `UUID PK`, `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`, `charge_id` `UUID REFERENCES subscription_charges(id) ON DELETE SET NULL`
- `amount` `NUMERIC(12, 2) NOT NULL CHECK (> 0)`, `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`, `payment_method` (`TRANSFER`, `QR`, `CASH`, `CARD`, `ADJUSTMENT`)
- `reference_code` `VARCHAR(120)`, `receipt_url` `TEXT`, `paid_at` `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `recorded_by_admin_id` `UUID REFERENCES platform_admins(id) ON DELETE SET NULL`, `notes` `TEXT`, `created_at` `TIMESTAMPTZ`

### 2.5 `tenant_daily_telemetry` (Read-Model CQRS)
- `id` `UUID PK`, `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`, `branch_id` `UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE`, `stat_date` `DATE NOT NULL` *(zona horaria local de sucursal)*
- `paid_orders_count` `INTEGER NOT NULL DEFAULT 0`, `gross_sales_amount` `NUMERIC(14, 2) NOT NULL DEFAULT 0`, `cancelled_orders_count` `INTEGER NOT NULL DEFAULT 0`, `last_event_at` `TIMESTAMPTZ`
- `CONSTRAINT uq_tenant_daily_telemetry UNIQUE (tenant_id, branch_id, stat_date)`

### 2.6 `platform_audit_logs`
- `id` `UUID PK`, `actor_admin_id` `UUID REFERENCES platform_admins(id) ON DELETE SET NULL`, `actor_username` `VARCHAR(100) NOT NULL`
- `action` `VARCHAR(80) NOT NULL`, `target_type` `VARCHAR(50)`, `target_id` `VARCHAR(100)`
- **`details_json` `TEXT`** *(payload JSON serializado como texto en columna `TEXT`)*, `ip_address` `VARCHAR(64)`, `created_at` `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`

---

## 3. Evolución UAT Sprint 6 (`V20`, `V21`, `V22`)
- **`V20__tenant_first_login_tracking.sql`:** Agrega `app_users.last_login_at TIMESTAMPTZ` con índice parcial para distinguir inquilinos aprovisionados desde Backoffice sin primer ingreso en el ERP.
- **`V21__subscription_plans_category_and_unlimited.sql`:** Flexibiliza `subscription_plans.duration_days >= 0` (`0` = vigencia ilimitada), añade `plan_category` (`PUBLIC_ONBOARDING`, `TRIAL`, `CUSTOM_PRIVATE`) y clasifica `TRIAL_14D` como `TRIAL`.
- **`V22__platform_admin_optional_email_and_password_policy.sql`:** Hace `platform_admins.email` nullable con índice único parcial `uq_platform_admin_email` (`WHERE email IS NOT NULL AND TRIM(email) <> ''`) e inserta `security.platform_password_min_length` (`12`) y `security.platform_password_require_complexity` (`false`) en `platform_settings`.
