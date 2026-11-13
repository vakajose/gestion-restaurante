---
title: "03. Esquema de Base de Datos de Plataforma (V17, V18, V19)"
category: "sdd"
module: "backoffice"
tags:
  - sdd
  - backoffice
  - postgresql
  - flyway
  - database
  - obsidian
last_updated: "2026-11-12"
status: "approved"
---

# 03. Esquema de Base de Datos de Plataforma (`V17`, `V18`, `V19`)

> ⬅️ [Volver al Índice Backoffice](../README.md) | [ERD Mermaid](../architecture/domain_and_erd_subscriptions.md)

Las tablas de plataforma conviven en PostgreSQL 16 con aislamiento lógico estricto respecto a los datos transaccionales de restaurantes (`ADR-0027`, `ADR-0028`, `ADR-0029`).

---

## 1. Identidad y Configuración Base (Implementado en `TASK-602` y `TASK-603`)
- **`V17__platform_admins_and_sessions.sql`:**
  - `platform_admins`: `id`, `username`, `email`, `full_name`, `password_hash`, `role` (`SUPERADMIN` | `PLATFORM_OPERATOR`), `is_active`, `must_change_password`, `last_login_at`, `created_at`, `updated_at`, `version`.
  - `platform_refresh_tokens`: `id`, `admin_id -> platform_admins`, `token_hash` (SHA-256), `device_info`, `ip_address`, `expires_at`, `revoked_at`, `replaced_by_token_hash`.
- **`V18__platform_settings_storage.sql`:**
  - `platform_settings`: `setting_key` (PK), `setting_value`, `value_type`, `description`, `updated_by_admin_id -> platform_admins`, `updated_at`.

---

## 2. Motor Comercial, Suscripciones, Telemetría y Auditoría (`V19__backoffice_subscriptions_telemetry_and_audit.sql`)

### 2.1 `subscription_plans`
Catálogo maestro de planes SaaS disponibles:
- `id` `UUID PK`
- `code` `VARCHAR(50) UNIQUE NOT NULL` (ej. `WEEKLY_STARTER`, `MONTHLY_PRO`, `MONTHLY_ENTERPRISE`, `TRIAL_14D`)
- `name` `VARCHAR(120) NOT NULL`, `description` `TEXT`
- `billing_cycle` `VARCHAR(20) NOT NULL CHECK IN ('WEEKLY', 'MONTHLY', 'ANNUAL', 'CUSTOM')`
- `duration_days` `INTEGER NOT NULL CHECK (> 0)`
- `price` `NUMERIC(12, 2) NOT NULL CHECK (>= 0)`
- `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`
- Cuotas por defecto (`NULL` = ilimitado): `max_branches`, `max_users`, `max_dishes`, `max_inventory_items`, `max_monthly_orders` (`INTEGER CHECK (>= 0)`)
- `is_active` `BOOLEAN NOT NULL DEFAULT TRUE`
- `created_at`, `updated_at` (`TIMESTAMPTZ`), `version` `BIGINT`

*(Semilla inicial idempotente: planes base `TRIAL_14D`, `WEEKLY_FLEX`, `MONTHLY_STANDARD`, `MONTHLY_PRO` editables desde Backoffice).*

### 2.2 `tenant_subscriptions`
Relación `1:1` con `tenants(id)` manteniendo el estado efectivo de acceso y cuotas:
- `id` `UUID PK`, `tenant_id` `UUID UNIQUE NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`
- `plan_id` `UUID REFERENCES subscription_plans(id)`
- `status` `VARCHAR(30) NOT NULL` (`TRIAL`, `ACTIVE`, `GRACE_PERIOD`, `SUSPENDED`, `CANCELLED`)
- `suspension_mode` `VARCHAR(30) NOT NULL DEFAULT 'NONE'` (`NONE`, `POS_AND_SHIFT_ONLY`, `TOTAL`)
- `auto_suspend_mode_on_expire` `VARCHAR(30) NOT NULL DEFAULT 'POS_AND_SHIFT_ONLY'` (`POS_AND_SHIFT_ONLY`, `TOTAL`)
- `suspension_reason` `TEXT`
- `current_period_start` `TIMESTAMPTZ`, `current_period_end` `TIMESTAMPTZ`
- **Gracia condicional:** `grace_until` `TIMESTAMPTZ`, `grace_max_orders` `INTEGER CHECK (> 0)`, `grace_orders_used` `INTEGER NOT NULL DEFAULT 0`, `grace_notes` `TEXT`
- **Overrides configurables por tenant (`NULL` hereda valor del plan):** `override_max_branches`, `override_max_users`, `override_max_dishes`, `override_max_inventory_items`, `override_max_monthly_orders` (`INTEGER CHECK (>= 0)`)
- `updated_by_admin_id` `UUID REFERENCES platform_admins(id)`
- `created_at`, `updated_at` (`TIMESTAMPTZ`), `version` `BIGINT`

### 2.3 `subscription_charges`
Períodos/asignaciones exigibles (soporta tanto planes pagados al momento como planes asignados en pendiente/mora):
- `id` `UUID PK`, `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`
- `plan_id` `UUID REFERENCES subscription_plans(id)`, `plan_name_snapshot` `VARCHAR(120) NOT NULL`
- `period_start` `TIMESTAMPTZ NOT NULL`, `period_end` `TIMESTAMPTZ NOT NULL`
- `amount_due` `NUMERIC(12, 2) NOT NULL CHECK (>= 0)`
- `paid_amount` `NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (>= 0)`
- `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`
- `charge_status` `VARCHAR(20) NOT NULL` (`PENDING`, `PARTIAL`, `PAID`, `WAIVED`)
- `due_date` `DATE` (`ADR-0006`)
- `notes` `TEXT`, `created_by_admin_id` `UUID REFERENCES platform_admins(id)`, `created_at` `TIMESTAMPTZ`, `updated_at` `TIMESTAMPTZ`

### 2.4 `subscription_payments` & Conciliación
- `id` `UUID PK`, `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`
- `charge_id` `UUID REFERENCES subscription_charges(id) ON DELETE SET NULL` *(cargo principal cubierto o conciliación FIFO registrando distribución)*
- `amount` `NUMERIC(12, 2) NOT NULL CHECK (> 0)`
- `currency` `VARCHAR(10) NOT NULL DEFAULT 'BOB'`
- `payment_method` `VARCHAR(30) NOT NULL` (`TRANSFER`, `QR`, `CASH`, `CARD`, `ADJUSTMENT`)
- `reference_code` `VARCHAR(120)`, `receipt_url` `TEXT`
- `paid_at` `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
- `recorded_by_admin_id` `UUID REFERENCES platform_admins(id)`
- `notes` `TEXT`, `created_at` `TIMESTAMPTZ`

### 2.5 `tenant_daily_telemetry` (Read-Model CQRS)
- `id` `UUID PK`
- `tenant_id` `UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE`
- `branch_id` `UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE`
- `stat_date` `DATE NOT NULL` *(zona horaria local de la sucursal)*
- `paid_orders_count` `INTEGER NOT NULL DEFAULT 0`
- `gross_sales_amount` `NUMERIC(14, 2) NOT NULL DEFAULT 0`
- `cancelled_orders_count` `INTEGER NOT NULL DEFAULT 0`
- `last_event_at` `TIMESTAMPTZ`
- `CONSTRAINT uq_tenant_daily_telemetry UNIQUE (tenant_id, branch_id, stat_date)`

### 2.6 `platform_audit_logs`
- `id` `UUID PK`
- `actor_admin_id` `UUID REFERENCES platform_admins(id)`
- `actor_username` `VARCHAR(100) NOT NULL`
- `action` `VARCHAR(80) NOT NULL` (ej. `PLAN_ASSIGNED`, `PAYMENT_RECORDED`, `GRACE_GRANTED`, `TENANT_SUSPENDED`, `LOGGER_UPDATED`, `SETTING_UPDATED`, `ADMIN_CREATED`)
- `target_type` `VARCHAR(50)`, `target_id` `VARCHAR(100)`
- `details_json` `JSONB`
- `ip_address` `VARCHAR(64)`
- `created_at` `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
