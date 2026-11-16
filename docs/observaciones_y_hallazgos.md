---
title: "Bitácora Dinámica de Observaciones, Hallazgos y Mejoras (Inbox de Triage)"
category: "feedback"
status: "living-document"
last_updated: "2026-11-13"
tags:
  - observations
  - bugs
  - feedback
  - triage
  - backlog-inbox
---

# 📥 Bitácora Dinámica de Observaciones, Hallazgos y Mejoras

> **Propósito:** Inbox temporal de hallazgos durante pruebas manuales.  
> **Estado Actual:** Inbox limpio (`0` pendientes sin triage). Los ítems `OBS-001` a `OBS-023` ya fueron volcados a [`docs/tasks.md`](./tasks.md) (`TASK-614`–`TASK-636`).

---

## 📊 Resumen Rápido (Volcado a `docs/tasks.md`)

| ID | Fecha | Módulo / Pantalla | Tipo | Severidad / Prioridad | Título Breve | Tarea Destino |
|:---|:---:|:---|:---:|:---:|:---|:---:|
| **OBS-013** | 2026-11-16 | Gestión Usuarios / Auth | `BUG` | `CRÍTICA` | `mustChangePassword` no obliga a cambiar clave tras reset desde Backoffice | `TASK-628` |
| **OBS-014** | 2026-11-16 | ERP Catálogo (`/catalog`) | `MEJORA` | `ALTA` | Falta edición y eliminación de categorías | `TASK-631` |
| **OBS-015** | 2026-11-16 | Catálogo / POS / Infra | `BUG` | `ALTA` | Imágenes guardadas en disco no renderizan en `http://app.localhost` (y documentar config por ambiente si aplica) | `TASK-629` |
| **OBS-016** | 2026-11-16 | ERP Catálogo (`/catalog`) | `MEJORA` | `ALTA` | Falta edición de platos existentes | `TASK-631` |
| **OBS-017** | 2026-11-16 | ERP Catálogo (`/catalog`) | `BUG/DISEÑO` | `ALTA` | Bug visual al desmarcar *"Precios de mi Sucursal"* + estudio/simplificación de *"Ajustar Sucursal"* y *"Clonar"* | `TASK-632` |
| **OBS-018** | 2026-11-16 | ERP POS / Catálogo | `UI/UX` | `ALTA` | Pulir comprobante POS (*"Registrado en servidor"*) y depurar jerga técnica (*"Receta BOM"*, etc.) | `TASK-630` |
| **OBS-019** | 2026-11-16 | ERP Catálogo | `FUNC/DISEÑO` | `ALTA` | Registro de Extras/Complementos y asignación por plato | `TASK-634` |
| **OBS-020** | 2026-11-16 | ERP Catálogo | `FUNC/DISEÑO` | `ALTA` | Creador y gestor de Combos | `TASK-634` |
| **OBS-021** | 2026-11-16 | ERP Catálogo (`/catalog/menus`) | `ESTUDIO` | `ALTA` | Estudio y generalización del concepto de Menú del Día por tipo de negocio | `TASK-634` |
| **OBS-022** | 2026-11-16 | Arquitectura / BD | `ARQ/ESTUDIO` | `ALTA` | Evaluación Relacional vs Híbrido `JSONB` para estructura dinámica de catálogo | `TASK-633` |
| **OBS-023** | 2026-11-16 | Catálogo / Inventario / Kardex | `ESTUDIO/UX` | `MEDIA (MVP) / BAJA (Post-MVP)` | Saneamiento mínimo de alta/edición de insumos (MVP) y estudio estándar de inventario/lotes/pre-elaborados (Post-MVP) | `TASK-635` / `TASK-636` |

---

### [OBS-015] Imágenes de platos guardadas en disco no se visualizan en `http://app.localhost` (`/catalog` y `/pos`)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`), POS (`/pos`) e Infraestructura (`docker/caddy/Caddyfile.testing-local`)
- **Tipo:** `BUG`
- **Severidad:** `ALTA`
- **Estado:** `VOLCADA A TASK-629`
- **Diagnóstico Verificado:**
  - El archivo **sí se guarda correctamente** en el volumen mapeado del servidor (`uploads/dishes/dish_b0d9f7dc-08bb-4b3f-8334-6b29467ced4d_6e5fd45a.webp`, 28,682 bytes).
  - El backend retorna `imageUrl` relativa (`/uploads/dishes/...`), por lo que el navegador en `http://app.localhost` solicita `http://app.localhost/uploads/dishes/...` hacia `erp-frontend:8080` (Nginx) en lugar de `backend:8080` (`http://api.localhost/uploads/dishes/...`), dado que `Caddyfile.testing-local` solo tiene `handle /uploads/*` en `http://localhost` y no en `http://app.localhost` (ni el frontend resuelve rutas relativas contra `API_BASE_URL`).
- **Comportamiento Esperado:**
  - Las imágenes subidas deben renderizarse correctamente en cualquier ambiente (`dev`, `testing-local`, `staging`, `prod`). Si requiere configuración post-levantamiento no automatizable, debe quedar documentada explícitamente para los superadmins.

### [OBS-016] No existe edición de platos en Catálogo (`/catalog`)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`)
- **Estado:** `VOLCADA A TASK-631`
- **Detalle:** Solo existe creación, clonado y cambio de foto; falta editar los datos de un plato existente.

### [OBS-017] Ruptura visual al desmarcar *"Precios de mi Sucursal"* y flujo confuso de *"Ajustar Sucursal"* / *"Clonar"*
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo (`/catalog`)
- **Estado:** `VOLCADA A TASK-632`
- **Detalle:** Al desmarcar `Precios de mi Sucursal`, la tabla se desarma (columnas vacías y fila corrupta/fantasma). Los botones *"Ajustar Sucursal"* y *"Clonar"* confunden al usuario final; se requiere estudiar parámetros/lógica de negocio y definir un comportamiento por defecto más simple.

### [OBS-018] Etiquetas y textos técnicos expuestos al usuario final (POS y Catálogo)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP POS (`/pos`) y Catálogo (`/catalog`)
- **Estado:** `VOLCADA A TASK-630`
- **Detalle:** El comprobante del POS muestra *"Registrado en servidor"* y en Catálogo aparecen términos técnicos como *"Receta BOM"*. Debe depurarse toda jerga de desarrollo hacia lenguaje claro de negocio.

### [OBS-019] / [OBS-020] / [OBS-021] / [OBS-022] Catálogo MVP: Extras/Complementos, Combos, Menús del Día y Modelo Flexible (`JSONB` vs Relacional)
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo / Arquitectura Backend & BD
- **Estado:** `VOLCADA A TASK-633 y TASK-634`
- **Detalle:**
  - Falta categorización y gestión de extras/complementos (vendibles solos o exclusivos como acompañamiento) y su asignación en la configuración de cada plato (`OBS-019`).
  - Falta creador y gestor de combos (`OBS-020`).
  - Estudiar y generalizar el concepto de Menú del Día según tipo de negocio (`OBS-021`).
  - Analizar pros y contras de usar estructura flexible (`JSONB` en PostgreSQL) frente a tablas relacionales rígidas para soportar combinaciones dinámicas sin motor NoSQL adicional (`OBS-022`).

### [OBS-023] Flujo de Insumos, Inventario, Pre-elaborados y Costeo de Lote
- **Fecha:** 2026-11-16
- **Módulo / Ruta:** ERP Catálogo e Inventario
- **Estado:** `VOLCADA A TASK-635 (mínimo MVP) y TASK-636 (Post-MVP)`
- **Detalle:**
  - Unificar alta/edición de insumos sin duplicidades ni obligar a saltar entre vistas incoherentes.
  - Condicionar o aclarar *"Stock inicial"* vs *"Ajuste de stock"*; solicitar valor total del stock/lote calculando el unitario aproximado.
  - En MVP v1 priorizar únicamente dejar funcional/coherente lo mínimo de alta/modificación de insumos sin funciones que estorben; dejar Kardex completo e insumos transformados/pre-elaborados para fase Post-MVP.
---

## 📋 Plantilla para Nuevas Observaciones

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
