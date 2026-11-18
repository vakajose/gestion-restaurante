---
title: "ADR-0027: Identidad Supra-Tenant de Plataforma (platform_admins) y Bootstrap Limpio Go-Live"
category: "adr"
tags:
  - adr
  - backoffice
  - security
  - bootstrap
  - multi-tenant
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0027: Identidad Supra-Tenant de Plataforma (`platform_admins`) y Bootstrap Limpio Go-Live

- **Sprint / Tarea:** Sprint 6 — `TASK-602` (`PB-057`, `PB-002`)

---

## 1. Contexto

En el esquema inicial (`V1`–`V16`), toda identidad de usuario vivía en `app_users` con `tenant_id UUID NOT NULL REFERENCES tenants(id)` y sus sesiones en `refresh_tokens` también atadas a `tenant_id NOT NULL`. Esto impedía arrancar una base de datos limpia sin restaurantes demo (`testing-local`, `staging`, `prod`) con un administrador de plataforma (`ROLE_SUPERADMIN`).

Adicionalmente, el rol de administrador de plataforma (`Backoffice`) y los usuarios operativos de restaurantes (`ERP`) pertenecen a dominios de seguridad y negocio distintos. Mezclarlos en las mismas tablas, DTOs de `TenantContext` o filtros JWT introduciría nulos artificiales y riesgo de fuga de privilegios.

---

## 2. Decisión

### 2.1 Separación Física de Identidades, Sesiones y Configuración
1. **`platform_admins` (`V17`):** Tabla física independiente de `app_users` para los administradores de plataforma (estimados en 2–3 cuentas iniciales). Incluye `must_change_password`, `is_active` y `last_login_at`.
2. **`platform_refresh_tokens` (`V17`):** Tabla dedicada de sesiones opacas (SHA-256) con rotación atómica y detección de reúso, manteniendo intactas `app_users` y `refresh_tokens` exclusivamente para tenants.
3. **Separación de Parametría (decisión para `TASK-603`/`604`):** `system_parameter_values` permanece exclusiva para restaurantes (`TENANT`/`BRANCH`, conservando su FK `updated_by -> app_users(id)`). Los parámetros globales de plataforma residirán en su propia tabla `platform_settings` con FK hacia `platform_admins(id)`.

### 2.2 Factorización Criptográfica y Cadenas de Seguridad Independientes
- **Patrón de Composición (`JwtCodec` + `JwtSigningSpec`):** Se extrae la firma/verificación HMAC-SHA256 (`exp`, `iss`, `aud`) a `JwtCodec` en `core.security`.
  - `JwtService` (ERP) compone `JwtCodec` con `aud = "erp-tenant"` y `${JWT_SECRET}`, retornando `TenantContext`.
  - `PlatformJwtService` (Backoffice, en `modules.backoffice`) compone `JwtCodec` con `aud = "platform-backoffice"` y `${PLATFORM_JWT_SECRET:${JWT_SECRET}}`, retornando `PlatformPrincipal` sin `tenantId`.
- **Doble `SecurityFilterChain`:**
  - `@Order(1)` en `modules.backoffice` (`securityMatcher("/api/v1/backoffice/**", "/actuator/**")`) con `PlatformJwtAuthenticationFilter` asignando únicamente `ROLE_SUPERADMIN` y sin tocar `TenantContextHolder`.
  - `@Order(2)` en `core.security` para las rutas ERP (`/api/v1/**`).

### 2.3 Bootstrap Fail-Fast y Recuperación de Emergencia (*Break-Glass*)
- **Bootstrap Idempotente (`PlatformBootstrapInitializer`):** Si `platform_admins` está vacía, crea el administrador inicial desde `PLATFORM_ADMIN_USERNAME`, `PLATFORM_ADMIN_EMAIL` y `PLATFORM_ADMIN_PASSWORD` (mínimo **12 caracteres**) con `must_change_password = true`.
- **Fail-Fast en Nube/Testing:** En `testing-local`, `staging` y `prod`, si `platform_admins` está vacía y faltan las variables o la contraseña tiene $< 12$ caracteres, el arranque se aborta de inmediato.
- **Recuperación *Break-Glass*:** Los reinicios normales jamás sobrescriben contraseñas ya cambiadas. En caso de bloqueo total de acceso, un operador con acceso SSH al host ejecuta `scripts/ops/reset-superadmin.sh` (`--app.platform.admin.break-glass-reset=true`), el cual restablece la clave desde variable temporal, activa la cuenta, fuerza `must_change_password = true` y revoca todos sus refresh tokens activos.

---

## 3. Consecuencias

- **Aislamiento total:** Un JWT de restaurante (`erp-tenant`) es criptográficamente rechazado por el Backoffice y viceversa, aunque compartieran infraestructura.
- **Go-Live limpio:** Los entornos `testing-local`, `staging` y `prod` arrancan sin restaurantes ficticios ni usuarios demo.
- **Seguridad reforzada:** Contraseñas de plataforma mínimas de 12 caracteres con rotación obligatoria tras bootstrap o *break-glass*.

## Documentos Relacionados
- [ADR-0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica](./0002_hybrid_tenancy_and_dynamic_settings.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0009: Onboarding Guiado de Empresa y Presets Operativos](./0009_tenant_onboarding_and_operational_presets.md)
- [ADR-0010: Control de Acceso RBAC Dinámico](./0010_granular_rbac_roles_and_privileges.md)
- [ADR-0011: Ciclo de Vida de Sesiones y Refresh Tokens](./0011_session_lifecycle_and_silent_refresh.md)
- [ADR-0026: Estrategia de Infraestructura Cloud y Despliegue](./0026_cloud_infrastructure_and_deployment_stack.md)
- [ADR-0028: Almacenamiento Agnóstico de Media y `platform_settings`](./0028_agnostic_media_storage_and_platform_settings.md)
- [ADR-0029: Motor SaaS de Backoffice, Cuotas y Telemetría](./0029_backoffice_subscriptions_quotas_and_telemetry.md)
- [Casos de Uso de Backoffice SaaS](../backoffice/architecture/use_cases_backoffice.md)
- [Manual de Bootstrap y Operación Inicial](../manuals/plataforma/bootstrap_y_operacion_inicial.md)
- [Manual Operativo de Backoffice SaaS](../manuals/plataforma/operacion_backoffice_saas.md)
