---
title: "04. Especificación Frontend Independiente — Backoffice React LTS (backoffice-frontend/)"
category: "sdd"
module: "backoffice-frontend"
tags:
  - sdd
  - backoffice
  - react
  - vite
  - tailwind
  - obsidian
last_updated: "2026-11-13"
status: "implemented"
---

# 04. Especificación Frontend Independiente — Backoffice React LTS (`backoffice-frontend/`)

> ⬅️ [Volver al Índice Backoffice](../README.md) | [C4 Contenedores](../architecture/c4_backoffice_system.md)

*(Documento de especificación base para **`TASK-605`**, completamente independiente de la SPA Angular ERP en `frontend/`).*

---

## 1. Ubicación y Stack Tecnológico
- **Directorio en Monorepo:** `backoffice-frontend/` (hermano al mismo nivel de `backend/` y `frontend/`).
- **Stack:** **React 19 LTS**, **TypeScript** estricto, **Vite**, **React Router**, **Tailwind CSS** (reutilizando paleta semántica del sistema con estética administrativa limpia sin dependencias *bloatware*), **Lucide React** para iconografía sobria y **Vitest + React Testing Library** para tests unitarios/componentes.
- **Inyección Runtime Twelve-Factor:** `public/env.js` generado al arranque del contenedor Nginx (`window.__ENV__ = { API_BASE_URL: "..." }`) consumido en producción sin reconstruir la imagen Docker (`TASK-606`).

---

## 2. Módulos y Rutas Funcionales de la SPA

| Ruta | Módulo | Capacidades Principales |
|---|---|---|
| `/login` | Autenticación Plataforma | Login aislado (`platform_admins`) + redirección forzada si `mustChangePassword = true` |
| `/profile` | Mi Perfil | Cambio obligatorio u opcional de contraseña propia |
| `/dashboard` | Ejecutivo & Telemetría | KPIs globales (tenants activos/gracia/suspendidos, ventas consolidadas, mora pendiente, alertas *over-quota*) |
| `/tenants` | Cartera de Inquilinos | Tabla con estado de suscripción, deuda, filtros, botón "Nuevo Inquilino" |
| `/tenants/:id` | Ficha 360° de Tenant | Uso vs cuotas (`branches`, `users`, `dishes`, `inventory`, `orders`), gráfico/tabla ventas por día/sucursal, asignación de planes (pagado o pendiente), registro de pagos, gracia condicional, overrides de cuota y suspensión (`POS_AND_SHIFT_ONLY` \| `TOTAL`) |
| `/plans` | Planes de Suscripción | CRUD de `subscription_plans` (`WEEKLY`, `MONTHLY`, `ANNUAL`, límites base y estado) |
| `/platform/settings` | Storage & Plataforma | Edición dinámica de `platform_settings` (`LOCAL` vs `S3`, MB máx., prefijo URL) + botón **"Probar Conexión"** (proveedor activo) |
| `/platform/observability` | Observabilidad & Logs | Salud de BD/Hikari/disco, métricas JVM, selector de loggers por paquete en caliente (`INFO`/`DEBUG`/`TRACE`) con alerta preventiva en `DEBUG`, visor de configuración efectiva y bitácora `platform_audit_logs` |
| `/platform/admins` | Equipo Backoffice | ABM de cuentas `platform_admins` |
