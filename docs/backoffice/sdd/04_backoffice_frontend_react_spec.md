---
title: "04. Especificación Frontend Independiente — Backoffice React 19 (backoffice-frontend/)"
category: "sdd"
module: "backoffice-frontend"
tags:
  - sdd
  - backoffice
  - react
  - vite
  - tailwind
  - obsidian
last_updated: "2026-11-17"
status: "verified"
---

# 04. Especificación Frontend Independiente — Backoffice React 19 (`backoffice-frontend/`)

> ⬅️ [Volver al Índice Backoffice](../README.md) | [C4 Contenedores](../architecture/c4_backoffice_system.md)

Aplicación web totalmente desacoplada de la SPA Angular ERP (`frontend/`).

---

## 1. Ubicación y Stack Tecnológico Real
- **Directorio en Monorepo:** `backoffice-frontend/`
- **Stack:** **React 19 LTS** (`^19.2.0`), **TypeScript** estricto (`~5.8.3`), **Vite** (`^6.3.5`), **React Router** (`^7.5.0`), **Tailwind CSS 3.4** (`darkMode: 'class'` con variables CSS semánticas `--bo-canvas`, `--bo-surface`, `--bo-border` gestionadas por `ThemeProvider`), **Lucide React** y **Vitest + React Testing Library** (`11` archivos / `22` tests).
- **Inyección Runtime Twelve-Factor:** `public/env.js` generado al arranque del contenedor Nginx (`window.__ENV__ = { API_BASE_URL: "..." }`) leído por `src/config/runtimeEnv.ts` para promover una única imagen Docker entre `testing-local`, `staging` y `prod`.
- **Comandos de Calidad (`.github/workflows/ci.yml`):** `cd backoffice-frontend && npm run lint && npm test -- --run && npm run build`.

---

## 2. Rutas Funcionales (`backoffice-frontend/src/App.tsx`)

| Ruta | Página (`src/pages/`) | Capacidades Principales |
|---|---|---|
| `/login` | `LoginPage.tsx` | Login aislado (`platform_admins`) + redirección forzada si `mustChangePassword = true` |
| `/profile` | `ProfilePage.tsx` | Cambio obligatorio u opcional de contraseña propia |
| `/dashboard` | `DashboardPage.tsx` | KPIs globales (tenants activos/gracia/suspendidos, ventas consolidadas, mora pendiente, alertas *over-quota*) |
| `/tenants` | `TenantsPage.tsx` | Cartera con filtros (`status`, `hasDebt`, `overQuota`), estado de primer ingreso (`Sin primer ingreso` vs fecha) y modal de alta rápida |
| `/tenants/:id` | `TenantDetailPage.tsx` | Ficha 360° compuesta por las 5 pestañas bajo `src/components/tenant/` |
| `/plans` | `PlansPage.tsx` | CRUD de `subscription_plans` (`WEEKLY`, `MONTHLY`, `ANNUAL`, `CUSTOM`), categorías (`PUBLIC_ONBOARDING`, `TRIAL`, `CUSTOM_PRIVATE`), preset ilimitado gratis (`durationDays = 0`) y límites |
| `/platform/settings` | `PlatformSettingsPage.tsx` | Edición de `platform_settings` (`LOCAL` vs `S3`, tamaño máx., prefijo URL, política de contraseñas) + **"Probar Conexión"** |
| `/platform/observability` | `ObservabilityPage.tsx` | Salud BD/Hikari/disco, métricas JVM, loggers en caliente con buscador, entorno efectivo y auditoría `platform_audit_logs` paginada con visor JSON |
| `/platform/admins` | `AdminsPage.tsx` | *(Solo `SUPERADMIN` vía `ProtectedRoute requireSuperAdmin`)* ABM de `platform_admins` con email opcional y validación contra política vigente |

---

## 3. Catálogo Completo de Componentes (`backoffice-frontend/src/components/`)

### 3.1 Layout, Seguridad y Primitivos UI (`src/components/`)
| Componente | Archivo | Responsabilidad |
|---|---|---|
| **`AppLayout`** | `AppLayout.tsx` | Shell lateral/topbar con navegación por rol, selector de tema claro/oscuro (`ThemeProvider`) y cierre de sesión |
| **`ProtectedRoute`** | `ProtectedRoute.tsx` | Guarda de autenticación, redirección por `mustChangePassword` y verificación opcional `requireSuperAdmin` |
| **`AlertBanner`** | `AlertBanner.tsx` | Banner contextual reutilizable (`tone`, `title`, `message`) |
| **`Badge`** | `Badge.tsx` | Insignia tipada (`neutral` \| `primary` \| `success` \| `warning` \| `danger` \| `info`) con soporte `dark:` |
| **`StatusBadge`** | `StatusBadge.tsx` | Mapeo semántico de estados de suscripción, cargos y bloqueo (`ACTIVE`, `GRACE_PERIOD`, `SUSPENDED`, etc.) |
| **`Card`** | `Card.tsx` | Superficie contenedora con `padding` configurable y tokens `--bo-surface`/`--bo-border` |
| **`EmptyState` / `TableEmptyRow`** | `EmptyState.tsx` | Estados vacíos consistentes para paneles y filas `<tr>` de tablas |
| **`KpiCard`** | `KpiCard.tsx` | Tarjeta ejecutiva de métrica con ícono Lucide, subtítulo y tono semántico |
| **`Modal`** | `Modal.tsx` | Diálogo modal accesible reutilizable para flujos de creación, confirmación y visor JSON |
| **`QuotaBar`** | `QuotaBar.tsx` | Barra de progreso de consumo vs límite efectivo (resaltando estados *over-quota*) |
| **`DailySalesChart`** | `DailySalesChart.tsx` | Visualización ligera nativa de ventas diarias agregadas de `tenant_daily_telemetry` |

### 3.2 Pestañas de Detalle de Inquilino (`src/components/tenant/`)
| Componente | Archivo | Responsabilidad |
|---|---|---|
| **`TenantOverviewTab`** | `tenant/TenantOverviewTab.tsx` | Resumen legal, primer ingreso, sucursales y barras de uso vs cuota (`QuotaBar`) |
| **`TenantUsersTab`** | `tenant/TenantUsersTab.tsx` | Búsqueda de colaboradores `app_users`, reseteo de contraseña temporal y activación/desactivación |
| **`TenantSubscriptionTab`** | `tenant/TenantSubscriptionTab.tsx` | Asignación de plan (pagado o pendiente), overrides de cuotas, gracia condicional y suspensión (`POS_AND_SHIFT_ONLY` \| `TOTAL`) |
| **`TenantLedgerTab`** | `tenant/TenantLedgerTab.tsx` | Cargos pendientes/históricos, registro de pagos FIFO y condonación de cargos |
| **`TenantTelemetryTab`** | `tenant/TenantTelemetryTab.tsx` | Reporte de ventas por rango `DATE` y sucursal usando `DailySalesChart` e indicadores de inventario |
