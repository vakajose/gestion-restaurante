---
title: "Guía de Ejecución y Gobernanza de Pruebas E2E (Playwright)"
category: "testing"
tags:
  - testing
  - e2e
  - playwright
  - pom
last_updated: "2026-11-17"
status: "active"
---

# 🎭 Guía de Ejecución y Gobernanza de Pruebas E2E (End-to-End)

> ⬅️ **Navegación:** [Bóveda de Pruebas](./README.md) | [Inventario Maestro](./inventario_de_pruebas.md) | [Casos UAT y Regresión](./casos_uat_y_regresion.md)

Este documento detalla el estándar unificado de pruebas End-to-End (Nivel 4 — **ADR-0017**) usando **Playwright Test Runner Oficial** con patrón **Page Object Model (POM)**.

---

## 1. Propósito y Alcance del Nivel 4 (E2E)

Las pruebas E2E verifican la integridad funcional completa (PostgreSQL 16 + Spring Boot Modulith + Angular 22 / Dexie.js + Backoffice React) simulando interacción real de usuario y red:
1. **Ciclo de Jornada y Dinero:** Bloqueo sin turno $\rightarrow$ Apertura $\rightarrow$ Ventas / Menú Diario $\rightarrow$ Cancelaciones con PIN $\rightarrow$ Gastos $\rightarrow$ Arqueo y Cierre.
2. **Invariantes Financieras y Cuotas:** Prohibición de venta sin turno (`SHIFT_REQUIRED_ERROR`), tope de cupos (`ITEM_SOLD_OUT`) y cuotas SaaS (`OVER_QUOTA_RESOLUTION_REQUIRED`).
3. **Resiliencia Offline-First:** Ventas en Dexie.js sin red y sincronización idempotente (`client_transaction_id`).
4. **Onboarding, Multi-Tenant y RBAC:** Registro de nuevo tenant, aislamiento entre empresas y roles ERP / Backoffice.

---

## 2. Arquitectura Playwright (Page Object Model)

Inventario exacto en `frontend/e2e/` (**7 archivos `.spec.ts` / 34 tests + 4 Page Objects**):

```text
frontend/e2e/
├── pages/
│   ├── auth.page.ts           # Login y selección de sucursal
│   ├── pos.page.ts            # Catálogo, modificadores, carrito, cobro y banner de turno
│   ├── cash-control.page.ts   # KPIs de gaveta, comandas en vivo, apertura y arqueo
│   └── finance.page.ts        # Historial de turnos, auditoría, gastos y CSV
├── auth.spec.ts               # 4 tests: login, tema, redirección por rol
├── catalog.spec.ts            # 3 tests: catálogo, platos, insumos y categorías
├── finance.spec.ts            # 4 tests: KPIs, historial, gastos y control de caja
├── inventory.spec.ts          # 4 tests: existencias, Kardex y ajuste físico
├── pos.spec.ts                # 5 tests: carrito, modificadores, efectivo y offline Dexie
├── shift-lifecycle.spec.ts    # 6 tests: ciclo completo de caja + consolidación
└── sprint5-journey.spec.ts    # 8 tests: parámetros, SKU/emojis, PIN, delivery, anulación y CSV
```

*(Nota: los scripts bajo `scripts/e2e/*.js` corresponden a ejecutores CDP legacy de Sprint 1–3 conservados únicamente como referencia histórica; toda automatización E2E vigente reside en `frontend/e2e/*.spec.ts` usando POM).*

---

## 3. Normas Inviolables de Implementación

1. **Cero Esperas Rígidas (Zero Sleeps):** Prohibido `page.waitForTimeout()`. Usar siempre Web-First assertions (`await expect(locator).toBeVisible()`).
2. **Selectores `data-testid` Semánticos:** Todo elemento interactivo clave se referencia por `[data-testid="..."]` kebab-case encapsulado dentro de `frontend/e2e/pages/*.page.ts`.
3. **Aislamiento e Independencia de Entorno:** Las suites destinadas a `testing-local` o `staging` **no deben depender** de los botones demo de login (`demo-admin-btn`), los cuales solo se renderizan cuando Angular corre en modo `dev` (`TASK-612`); deben autenticar vía formulario estándar o fixture de sesión.

---

## 4. Comandos de Ejecución

### Script Maestro (desde la raíz)
```bash
# Ejecutar las 7 suites E2E (34 tests) en modo headless contra dev (:4200 / :8080)
./scripts/run-e2e.sh

# Ejecutar una suite específica
./scripts/run-e2e.sh shift-lifecycle.spec.ts
./scripts/run-e2e.sh sprint5-journey.spec.ts
```

### Playwright CLI (desde `frontend/`)
```bash
cd frontend
npm run test:e2e          # Headless
npm run test:e2e:headed   # Navegador visible
npm run test:e2e:ui       # Playwright UI interactivo
npm run test:e2e:report   # Reporte HTML con trazas
```

---

## 5. Requisitos Previos por Entorno y Brecha Pendiente en `testing-local`

- **Entorno `dev` nativo (Operativo para `./scripts/run-e2e.sh`):** PostgreSQL (`docker/docker-compose.yml`), Backend en `:8080` con perfil `dev` (migraciones **`V1`–`V24`** + seeders demo) y ERP Angular en `:4200`.
- **Entorno `testing-local` dockerizado (`docker/docker-compose.testing-local.yml`):** Stack levantado tras Caddy (`http://app.localhost`, `http://admin.localhost`, `http://api.localhost`, `http://logs.localhost`), con migraciones **`V1`–`V24`** y **sin seeders demo precargados**.

> [!WARNING]
> **Brecha Pendiente para Ejecutar Playwright contra `testing-local`:**
> En el estado actual de `release/sprint-6`, [`frontend/playwright.config.ts`](../../frontend/playwright.config.ts) fija `baseURL: 'http://localhost:4200'` y `webServer.url: 'http://localhost:4200'`, mientras que [`auth.page.ts`](../../frontend/e2e/pages/auth.page.ts), [`auth.spec.ts`](../../frontend/e2e/auth.spec.ts), [`catalog.spec.ts`](../../frontend/e2e/catalog.spec.ts) e [`inventory.spec.ts`](../../frontend/e2e/inventory.spec.ts) aún utilizan los atajos `[data-testid="demo-admin-btn"]` / `[data-testid="demo-cashier-btn"]` (exclusivos de `dev` tras `TASK-612`). Para ejecutar la batería automatizada directamente contra `http://app.localhost` (`testing-local` / `staging`), resta refactorizar esas 4 piezas hacia autenticación estándar por formulario (`tenantIdentifier`, `username`, `password`) y parametrizar `PLAYWRIGHT_BASE_URL` en `playwright.config.ts`.
