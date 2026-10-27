# Guía de Ejecución y Gobernanza de Pruebas E2E (End-to-End)

Este documento detalla la suite automatizada de pruebas End-to-End (E2E) del **Sistema de Gestión de Restaurante**, estructurada con **Playwright** y complementada con un ejecutor visual interactivo sobre **Chrome DevTools Protocol (CDP)**.

---

## 1. Propósito y Filosofía
Las pruebas E2E verifican la integridad funcional del sistema completo (Base de Datos PostgreSQL + Backend Spring Boot + Frontend Angular 22 Zoneless con Dexie.js) simulando interacciones reales de usuarios finales.

Garantizan que:
1. Ninguna nueva funcionalidad rompa flujos críticos existentes (regresión cero).
2. Los límites modulares y políticas de aislamiento multi-tenant y por sucursal se cumplan.
3. El ciclo financiero de apertura, ventas POS, promociones, control de gastos menores, arqueo físico y cierre de turno opere sin discrepancias.

---

## 2. Catálogo de Pruebas Persistidas

Las especificaciones se encuentran centralizadas en `frontend/e2e/`:

| Archivo | Flujo Evaluado | Funcionalidades Clave Verificadas |
|---|---|---|
| `shift-lifecycle.spec.ts` | **Ciclo Operativo Completo (Jornada)** | • Apertura de turno con fondo inicial (Cajero).<br>• Venta POS con promociones Happy Hour (`TASK-209`), combos (`TASK-208`) y modificadores con costo extra (`TASK-210`).<br>• Registro de solicitud de egreso de gaveta (`PENDING_APPROVAL`).<br>• Verificación de restricción de aprobación para cajero (RBAC).<br>• Cambio de sesión y login como Administrador (`ADMIN_TENANT`).<br>• Aprobación de gastos de caja menor.<br>• Conciliación física de gaveta (Arqueo con cuadre perfecto).<br>• Cierre de turno y preparación del sistema para el día siguiente. |
| `pos.spec.ts` | **Punto de Venta y Offline First** | • Catálogo visual y buscador.<br>• Selección de combos y grupos de modificadores.<br>• Cálculo de comanda reactiva y descuentos automáticos.<br>• Cobro en efectivo con cálculo de cambio y emisión de ticket.<br>• Resiliencia offline con Dexie.js (IndexedDB) y sincronización automática. |
| `finance.spec.ts` | **Finanzas y Caja Menor** | • Tarjetas KPI ejecutivas de turno.<br>• Desglose de arqueo en tiempo real.<br>• Registro y listado de egresos.<br>• Modal de arqueo y verificación de diferencia dinámica. |
| `inventory.spec.ts` | **Inventario y Kardex** | • Listado de existencias valorizadas.<br>• Trazabilidad de movimientos en Kardex.<br>• Ajuste físico de stock con persistencia atómica. |
| `catalog.spec.ts` | **Catálogo y Recetas BOM** | • Gestión de platos, insumos e ingredientes.<br>• Categorías de menú.<br>• Navegación por pestañas TailwindAdmin. |
| `auth.spec.ts` | **Autenticación y Tema** | • Validación de credenciales y botones demo.<br>• Redirección por roles (`ADMIN_TENANT` → `/company`, `CASHIER` → `/pos`).<br>• Conmutación de Modo Claro / Modo Oscuro (`ThemeService`). |

---

## 3. Comandos de Ejecución a Demanda

### A. Ejecución Rápida con el Script Maestro (Recomendado)
Desde la raíz del proyecto, el script `./scripts/run-e2e.sh` verifica automáticamente que el backend y el frontend estén activos y lanza las pruebas:

```bash
# Ejecutar todas las 25 pruebas E2E en modo headless
./scripts/run-e2e.sh

# Ejecutar únicamente el ciclo completo de turno y arqueo
./scripts/run-e2e.sh shift-lifecycle.spec.ts

# Ejecutar únicamente las pruebas de Punto de Venta
./scripts/run-e2e.sh pos.spec.ts
```

### B. Ejecución con Playwright CLI
Desde el directorio `frontend/`:

```bash
cd frontend

# Ejecutar todas las pruebas
npm run test:e2e

# Ejecutar visualizando el navegador en pantalla
npm run test:e2e:headed

# Modo interactivo Playwright UI (permite pausar paso a paso y ver DOM)
npm run test:e2e:ui

# Ver el último reporte generado en HTML
npm run test:e2e:report
```

### C. Ejecución Visual en Vivo sobre Chrome DevTools (Puerto 9222)
Cuando Chrome está levantado con depuración remota (`--remote-debugging-port=9222`), se pueden correr los scripts CDP para observar la automatización en directo sobre la ventana del navegador del usuario:

```bash
# Ejecutar el flujo completo de sesión de turno y arqueo en Chrome abierto
node scripts/e2e/run_shift_session_flow.js

# Ejecutar la suite de regresión visual de 10 casos
node scripts/e2e/run_full_regression_cdp.js
```
*Las capturas de pantalla de cada hito se guardan automáticamente en `./e2e-screenshots/`.*

---

## 4. Requisitos Previos para la Ejecución
1. **Base de Datos:** PostgreSQL en ejecución (`localhost:5432`, base de datos `restaurant_db`).
2. **Backend:** Servidor Spring Boot activo (`./backend/mvnw spring-boot:run` en puerto `8080`).
3. **Frontend:** Servidor Angular activo (`npm run start` en puerto `4200`).
