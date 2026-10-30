# Guía de Ejecución y Gobernanza de Pruebas E2E (End-to-End)

Este documento detalla el estándar unificado de pruebas End-to-End (E2E) para el **Sistema Integral de Gestión de Restaurante (POS + ERP)**, estructurado con **Playwright Test Runner Oficial** y gobernado por el **ADR-0017**.

---

## 1. Propósito y Filosofía del Nivel 4 (E2E)

Las pruebas E2E verifican la integridad funcional de extremo a extremo (PostgreSQL + Spring Boot Modulith + Angular 22 Zoneless con Dexie.js) simulando la interacción real de usuarios y el comportamiento de la red.

Siguiendo el **ADR-0017**, las pruebas E2E no se utilizan para validar aspectos visuales triviales ni validaciones de inputs básicas (que pertenecen a los Niveles 1 y 2), sino exclusivamente para:
1. **Flujos Críticos de Dinero y Jornada Completa:** Apertura de turno con saldo inicial $\rightarrow$ Ventas de catálogo y menú del día $\rightarrow$ Workflow de gastos menores $\rightarrow$ Cuadre de gaveta y arqueo de caja.
2. **Invariantes Financieras Inviolables:** Prohibición absoluta de venta sin turno activo (`SHIFT_REQUIRED_ERROR`).
3. **Control de Cupos y Stock en Tiempo Real:** Bloqueo reactivo y badge `AGOTADO` cuando el cupo o inventario llega a cero.
4. **Resiliencia Offline-First:** Cobros en Dexie.js (IndexedDB) desconectado y sincronización idempotente con `client_transaction_id` al reconectar.
5. **Aislamiento Multi-Tenant y Seguridad RBAC:** Verificación de acceso según roles (`CASHIER` vs `ADMIN_TENANT`).

---

## 2. Arquitectura de Automatización: Page Object Model (POM)

Para garantizar cero redundancias y máxima mantenibilidad, todas las pruebas E2E consumen selectores y acciones encapsuladas en clases Page Object ubicadas en `frontend/e2e/pages/`:

```
frontend/e2e/
├── pages/
│   ├── pos.page.ts           # Interacción con catálogo, carrito, cobro y banner de turno
│   ├── cash-control.page.ts  # KPIs de gaveta, comandas en vivo, apertura y arqueo
│   ├── finance.page.ts       # Historial de turnos, auditoría de discrepancias y gastos
│   ├── daily-menus.page.ts   # Configuración de plantillas de menús del día
│   └── auth.page.ts          # Login rápido por roles y selección de sucursal
├── shift-lifecycle.spec.ts   # Suite maestra de ciclo de jornada completa
├── pos.spec.ts               # Pruebas específicas de terminal de venta
├── finance.spec.ts           # Auditoría gerencial y reportes
├── inventory.spec.ts         # Existencias físicas y Kardex
├── catalog.spec.ts           # Catálogo general y recetas
└── auth.spec.ts              # Redirecciones y conmutación de temas
```

---

## 3. Normas Inviolables de Implementación

1. **Cero Esperas Rígidas (Zero Sleeps):**
   - Prohibido `page.waitForTimeout()`. Toda espera debe realizarse mediante auto-waiting locators y Web-First assertions:
     ```typescript
     // CORRECTO
     await expect(this.openShiftBanner).toBeVisible({ timeout: 10000 });
     await expect(this.quotaBadge).toHaveText('Cupo: 2');

     // PROHIBIDO
     await page.waitForTimeout(1000);
     ```
2. **Selectores por `data-testid` Semánticos:**
   - Todo locator interactivo se referencia mediante `[data-testid="..."]` kebab-case.
3. **Idempotencia y Determinismo:**
   - Toda prueba debe poder ejecutarse de forma independiente sin depender del estado residual de una prueba anterior.

---

## 4. Comandos de Ejecución a Demanda

### A. Ejecución Rápida con Script Maestro (Recomendado)
Desde la raíz del proyecto, el script `./scripts/run-e2e.sh` verifica que los servicios estén activos y lanza Playwright:

```bash
# Ejecutar todas las pruebas E2E en modo headless
./scripts/run-e2e.sh

# Ejecutar únicamente la suite de ciclo de jornada de caja
./scripts/run-e2e.sh shift-lifecycle.spec.ts

# Ejecutar únicamente las pruebas del POS
./scripts/run-e2e.sh pos.spec.ts
```

### B. Ejecución Nativa con Playwright CLI
Desde el directorio `frontend/`:

```bash
cd frontend

# Ejecutar todas las pruebas en modo headless (para CI o terminal)
npm run test:e2e

# Ejecutar visualizando el navegador en pantalla (Headed Mode)
npm run test:e2e:headed

# Modo interactivo Playwright UI (permite pausar, inspeccionar DOM y red paso a paso)
npm run test:e2e:ui

# Visualizar el reporte HTML detallado con trazas de la última ejecución
npm run test:e2e:report
```

---

## 5. Requisitos Previos para la Ejecución
1. **Base de Datos:** PostgreSQL en ejecución (`localhost:5432`, base de datos `restaurant_db` con migraciones Flyway V1 a V9 aplicadas).
2. **Backend:** Servidor Spring Boot activo (`./backend/mvnw spring-boot:run` en puerto `8080`).
3. **Frontend:** Servidor Angular activo (`npm run start` en puerto `4200`).
