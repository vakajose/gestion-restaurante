---
title: "Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Histórico Sprint 6)"
category: "feedback"
status: "archived"
last_updated: "2026-11-17"
tags:
  - observations
  - bugs
  - feedback
  - triage
  - backlog-inbox
  - sprint-6
---

# 📥 Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Sprint 6)

> [!NOTE]
> **Documento Archivado de Ciclo UAT Sprint 6 (`0` pendientes sin triage):**
> - Las observaciones **`OBS-001` a `OBS-012`** fueron volcadas a tareas y completadas en [`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md) (`TASK-614`–`TASK-618`, `TASK-620`–`TASK-624`, `TASK-626`, `TASK-627`).
> - Las observaciones **`OBS-013` a `OBS-023`** fueron volcadas, atendidas y cerradas en [`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md) (`TASK-628`–`TASK-635`), quedando únicamente el estudio Post-MVP de inventario avanzado (`OBS-023` Post-MVP) abierto en [`docs/tasks.md`](./tasks.md) como **`TASK-636`**.
> - Para el siguiente ciclo de pruebas manuales, abrir un nuevo inbox limpio o reactivar esta bitácora cambiando `status: "active"`.

---

## 📊 Trazabilidad Completa (`OBS-001`–`OBS-023`)

### Ronda 1 — UAT Backoffice y ERP Pre-MVP (`OBS-001`–`OBS-012`, Cerradas)

| ID | Fecha | Módulo / Pantalla | Título Breve | Tarea Destino | Estado |
|:---|:---:|:---|:---|:---:|:---:|
| **OBS-001** | 2026-11-13 | ERP Config (`/settings/parameters`) | Alerta `Unknown Error` (HTTP 304) al navegar entre Empresa y Sucursales | `TASK-615` | `CERRADA` |
| **OBS-002** | 2026-11-13 | ERP Onboarding (`/onboarding`) | Confirmación de contraseña en registro inicial | `TASK-621` | `CERRADA` |
| **OBS-003** | 2026-11-13 | ERP Sucursales (`/company`) | Refresco del selector tras crear/editar/alternar sucursal | `TASK-614` | `CERRADA` |
| **OBS-004** | 2026-11-13 | Backoffice React | Soporte de Modo Oscuro persistente | `TASK-622` | `CERRADA` |
| **OBS-005** | 2026-11-13 | Frontends (React / Angular) | Reutilización de primitivas UI y regla SDLC | `TASK-620` | `CERRADA` |
| **OBS-006** | 2026-11-13 | Backoffice (`/plans`) | UX de planes ilimitados gratis y categorización para Onboarding | `TASK-623` | `CERRADA` |
| **OBS-007** | 2026-11-13 | Backoffice (`/platform/observability`) | Buscador de loggers, filtro por detalle, visor JSON y paginación | `TASK-616` | `CERRADA` |
| **OBS-008** | 2026-11-13 | Backoffice (`/platform/admins`) | Email opcional y política configurable de contraseñas | `TASK-624` | `CERRADA` |
| **OBS-009** | 2026-11-13 | Backoffice (`/tenants`) | Dirección de sucursal y email del admin opcionales al aprovisionar | `TASK-617` | `CERRADA` |
| **OBS-010** | 2026-11-13 | Backoffice (`/tenants`) | Estado diferenciador *Sin primer ingreso* en inquilinos recién creados | `TASK-618` | `CERRADA` |
| **OBS-011** | 2026-11-13 | ERP Finanzas (`/finance`) | Error 500 PostgreSQL (`could not determine data type`) con filtros nulos | `TASK-626` | `CERRADA` |
| **OBS-012** | 2026-11-13 | ERP POS (`/pos`) | Advertencia/confirmación al abrir turno sin platos registrados | `TASK-627` | `CERRADA` |

### Ronda 2 — UAT MVP v1 (`OBS-013`–`OBS-023`, Cerradas / Volcadas)

| ID | Fecha | Módulo / Pantalla | Tipo | Severidad / Prioridad | Título Breve | Tarea Destino | Estado |
|:---|:---:|:---|:---:|:---:|:---|:---:|:---:|
| **OBS-013** | 2026-11-16 | Gestión Usuarios / Auth | `BUG` | `CRÍTICA` | `mustChangePassword` no obliga a cambiar clave tras reset desde Backoffice | `TASK-628` | `CERRADA` |
| **OBS-014** | 2026-11-16 | ERP Catálogo (`/catalog`) | `MEJORA` | `ALTA` | Falta edición y eliminación de categorías | `TASK-631` | `CERRADA` |
| **OBS-015** | 2026-11-16 | Catálogo / POS / Infra | `BUG` | `ALTA` | Imágenes guardadas en disco no renderizan en `http://app.localhost` | `TASK-629` | `CERRADA` |
| **OBS-016** | 2026-11-16 | ERP Catálogo (`/catalog`) | `MEJORA` | `ALTA` | Falta edición de platos existentes | `TASK-631` | `CERRADA` |
| **OBS-017** | 2026-11-16 | ERP Catálogo (`/catalog`) | `BUG/DISEÑO` | `ALTA` | Bug visual al desmarcar *"Precios de mi Sucursal"* + estudio flujo sucursal/clonar | `TASK-632` | `CERRADA` |
| **OBS-018** | 2026-11-16 | ERP POS / Catálogo | `UI/UX` | `ALTA` | Pulir comprobante POS (*"Registrado en servidor"*) y depurar jerga técnica | `TASK-630` | `CERRADA` |
| **OBS-019** | 2026-11-16 | ERP Catálogo | `FUNC/DISEÑO` | `ALTA` | Registro de Extras/Complementos y asignación por plato | `TASK-634` | `CERRADA` |
| **OBS-020** | 2026-11-16 | ERP Catálogo | `FUNC/DISEÑO` | `ALTA` | Creador y gestor de Combos | `TASK-634` | `CERRADA` |
| **OBS-021** | 2026-11-16 | ERP Catálogo (`/catalog/menus`) | `ESTUDIO` | `ALTA` | Estudio y generalización del concepto de Menú del Día por tipo de negocio | `TASK-634` | `CERRADA` |
| **OBS-022** | 2026-11-16 | Arquitectura / BD | `ARQ/ESTUDIO` | `ALTA` | Evaluación Relacional vs Híbrido `JSONB` para estructura dinámica de catálogo | `TASK-633` | `CERRADA` |
| **OBS-023** | 2026-11-16 | Catálogo / Inventario / Kardex | `ESTUDIO/UX` | `MEDIA (MVP) / BAJA (Post-MVP)` | Saneamiento mínimo de alta/edición de insumos (MVP) y estudio estándar de inventario/lotes/pre-elaborados (Post-MVP) | `TASK-635` (Cerrada) / `TASK-636` (Post-MVP) | `MVP CERRADO / POST-MVP EN TASKS` |

---

## 🗂️ Detalle de Hallazgos Cerrados (Ronda 2 MVP)

### [OBS-013] / [OBS-014] Cambio obligatorio de contraseña y edición/eliminación de categorías
- **Fecha:** 2026-11-16
- **Estado:** `CERRADA — Completada en TASK-628 y TASK-631` ([`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md))
- **Resolución:** Guardias ERP actualizados para redirigir obligatoriamente a `/profile` cuando `mustChangePassword = true` (gobernado por `security.enforce_password_change_on_reset`) y endpoints `PUT/DELETE` seguros para categorías en `/catalog`.

### [OBS-015] Imágenes de platos guardadas en disco no se visualizaban en `http://app.localhost` (`/catalog` y `/pos`)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`), POS (`/pos`) e Infraestructura (`docker/caddy/Caddyfile.testing-local`)
- **Tipo:** `BUG` | **Severidad:** `ALTA`
- **Estado:** `CERRADA — Completada en TASK-629` ([`sprint_06_backlog.md`](./project/sprints/sprint_06_backlog.md))
- **Resolución:** Resolución de URLs relativas `/uploads/...` contra `API_BASE_URL` en `runtime-env.ts`, bloque `handle /uploads/*` agregado en `Caddyfile.testing-local` y `Caddyfile.cloud`, y documentación en [`bootstrap_y_operacion_inicial.md`](./manuals/plataforma/bootstrap_y_operacion_inicial.md).

### [OBS-016] Edición de platos existentes en Catálogo (`/catalog`)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`)
- **Estado:** `CERRADA — Completada en TASK-631`
- **Resolución:** Habilitado `PUT /api/v1/catalog/dishes/{id}` y modal de edición completa de platos en `CatalogComponent`.

### [OBS-017] Ruptura visual al desmarcar *"Precios de mi Sucursal"* y flujo de *"Ajustar Sucursal"* / *"Clonar"*
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`)
- **Estado:** `CERRADA — Completada en TASK-632`
- **Resolución:** Reparada la renderización de tabla en `/catalog`, introducido el parámetro `catalog.scope_mode` (`BRANCH_ISOLATED` por defecto) y clarificado el clonado hacia sucursal destino.

### [OBS-018] Etiquetas y textos técnicos expuestos al usuario final (POS y Catálogo)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP POS (`/pos`), Catálogo (`/catalog`) y Caja (`/cash-control`)
- **Estado:** `CERRADA — Completada en TASK-630`
- **Resolución:** Depurada la jerga de desarrollo (*"Registrado en servidor"*, *"Receta BOM"*) hacia lenguaje de negocio claro.

### [OBS-019] / [OBS-020] / [OBS-021] / [OBS-022] Catálogo MVP: Extras/Complementos, Combos, Menús del Día y Modelo Flexible
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo / Arquitectura Backend & BD
- **Estado:** `CERRADA — Completada en TASK-633 y TASK-634`
- **Resolución:** Definido modelo relacional extendido (`V23`, `V24`), jornadas compartidas de sucursal (`branch_service_sessions`), CRUD de combos (`FIXED` / `MENU_COURSE`) y grupos de complementos, cupos ilimitados (`is_unlimited`), restricción `allow_individual_sale` y selecciones en comanda (`order_item_selections`). Evoluciones conceptuales futuras (`CourseType` -> categorías `0..*`, complementos en combos) registradas en [`TASK-637`](./tasks.md) y [`TASK-638`](./tasks.md).

### [OBS-023] Flujo de Insumos, Inventario, Pre-elaborados y Costeo de Lote
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo e Inventario
- **Estado:** `MVP CERRADO EN TASK-635 / POST-MVP VOLCADA A TASK-636`
- **Resolución:** En `TASK-635` se implementó edición/eliminación segura de insumos, diferenciación clara de Stock Inicial vs Ajuste de Stock y cálculo de costo unitario desde el Valor Total del Lote. El estudio integral de insumos pre-elaborados y Kardex avanzado permanece en [`TASK-636`](./tasks.md).

---

## 📋 Plantilla para Nuevas Observaciones (Próximo Ciclo UAT)

```markdown
### [OBS-XXX] Título conciso del hallazgo o cambio solicitado
- **Fecha:** YYYY-MM-DD
- **Módulo / Ruta:** (ej. POS `/pos`, Finanzas `/finance`, Catálogo `/catalog/menus`)
- **Tipo:** `BUG` | `UI/UX` | `MEJORA` | `CAMBIO`
- **Severidad:** `CRÍTICA` | `ALTA` | `MEDIA` | `BAJA`
- **Estado:** `PENDIENTE`
- **Comportamiento Actual:**
  - ...
- **Comportamiento Esperado:**
  - ...
```
