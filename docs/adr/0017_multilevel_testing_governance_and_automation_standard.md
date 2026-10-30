# ADR-0017: Marco Integral de Gobernanza y Pirámide Multinivel de Testing Automatizado

- **Estado:** Aceptado
- **Fecha:** 2026-10-30
- **Contexto:** TASK-412 / Estandarización de Calidad y Suite E2E de Jornada Completa (PB-046)

---

## 1. Contexto y Problemática

El Sistema Integral de Gestión para Restaurante (POS + ERP) combina múltiples capas tecnológicas críticas:
- Multi-tenancy híbrido con aislamiento estricto por `tenant_id` y `branch_id`.
- Operaciones financieras en efectivo y cobros digitales (invariante de turno obligatorio, arqueo físico de gaveta y workflow de gastos menores).
- Desacoplamiento modular en backend con Spring Modulith y base de datos relacional PostgreSQL con migraciones Flyway.
- Frontend reactivo con Angular 22 Zoneless, Signal Forms, UI Kit propio y capacidades Offline-First con Dexie.js (IndexedDB).

Durante los Sprints 1 a 3, el esfuerzo de pruebas creció de forma reactiva, generando tres problemas estructurales:
1. **Fragmentación en E2E:** Coexistencia de dos ejecutores incompatibles: una suite oficial de Playwright (`frontend/e2e/*.spec.ts`) y scripts paralelos de Node.js basados en Chrome DevTools Protocol (`scripts/e2e/*.js`).
2. **Uso de Esperas Rígidas:** Aparición de llamadas `waitForTimeout(...)` / `sleep` en pruebas de UI, generando lentitud e intermitencia (*flakiness*).
3. **Falta de Criterio de Nivelación:** Incertidumbre sobre a qué nivel del ciclo de pruebas (unitario, componente, integración o E2E) debe pertenecer cada nueva funcionalidad o modificación.

Se requiere un marco formal, permanente e inviolable que estandarice los niveles de verificación, elimine la duplicidad y asegure la calidad continua sin asfixiar la velocidad de desarrollo.

---

## 2. Decisión Arquitectónica

Se adopta formalmente el **Marco de Testing Multinivel Basado en el Trofeo de Pruebas (Testing Trophy)** con 4 niveles complementarios y la unificación absoluta de las pruebas End-to-End bajo el **Playwright Test Runner Oficial**:

```
          / \
         / E2E \        Nivel 4: Flujos Críticos de Negocio Cross-Módulo & Dinero
        /-------\
       / Integr. \      Nivel 3: Contratos .api, Transacciones, Flyway, Modulith
      /-----------\
     / Componentes \    Nivel 2: Signals, UI Kit, Validaciones de Formulario, A11y
    /---------------\
   /    Dominio      \  Nivel 1: Matemáticas, Fórmulas de Arqueo, Precios, Kardex
  /-------------------\
```

### 2.1. Definición de Responsabilidades por Nivel

| Nivel | Enfoque Principal | Tecnologías | Criterio de Éxito |
| :--- | :--- | :--- | :--- |
| **Nivel 1: Dominio Puro** | Lógica matemática pura, fórmulas financieras, invariantes contables de gaveta, valuación Kardex y tokens criptográficos. | JUnit 5 (Java), Vitest (TS puro). | Ejecución en memoria sin I/O ($< 10\text{ms}$). |
| **Nivel 2: Componentes UI** | Reactividad de Signals, Signal Forms, accesibilidad WCAG 2.2 AA, estados de UI Kit (`app-card`, `app-modal`, `app-badge`). | Angular TestBed + Vitest. | Pruebas de renderizado y eventos con servicios mockeados ($< 100\text{ms}$). |
| **Nivel 3: Integración Modular** | Límites Spring Modulith (`.verify()`), contratos `.api`, transacciones `@Transactional`, `@Version`, migraciones Flyway y RFC 7807. | Spring Boot Test (`MockMvc`, PostgreSQL/H2). | 100% de coherencia transaccional y validación de modularidad. |
| **Nivel 4: E2E Journeys** | Flujos completos de dinero, ciclo de jornada, resiliencia offline Dexie y concurrencia multi-cajero. | Playwright Oficial (`@playwright/test`). | Determinismo absoluto (cero sleeps, Web-First assertions, Page Object Model). |

### 2.2. Estandarización de la Suite E2E (Playwright)
1. **Retiro de Scripts CDP Aislados:** Se consolida toda la automatización en `frontend/e2e/`. Los scripts Node/CDP quedan obsoletos a favor de comandos nativos de Playwright (`--ui`, `--headed`, headless).
2. **Patrón Page Object Model (POM):** Todos los selectores y acciones de vistas críticas (`/pos`, `/cash-control`, `/finance`, `/catalog/menus`) deben encapsularse en clases bajo `frontend/e2e/pages/`.
3. **Cero Esperas Arbitrarias:** Prohibido el uso de `page.waitForTimeout()`. Toda aserción debe utilizar auto-retrying locators de Playwright (`await expect(locator).toBeVisible()`).
4. **Fixtures de Autenticación Rápida:** Reutilización de `storageState` para pruebas que requieren roles autenticados (`CASHIER`, `ADMIN_TENANT`), eliminando la sobrecarga de login repetitivo en cada spec.

### 2.3. Principio de Incorporación Progresiva (Boy Scout Testing)
- **Funcionalidades Nuevas:** Toda nueva tarea debe registrar sus casos de prueba en los niveles adecuados antes de darse por completada.
- **Funcionalidades Históricas:** No se detiene el desarrollo para nivelar el histórico de una sola vez. Se incorporan gradualmente durante las sesiones de Retrospectiva y cuando una tarea toque un módulo legacy.

---

## 3. Consecuencias

### Positivas
- **Determinismo y Estabilidad:** Eliminación completa de falsos positivos y pruebas intermitentes causadas por timeouts rígidos.
- **Mantenibilidad:** El patrón POM aísla los cambios de diseño y selectores del UI Kit a un único archivo por vista.
- **Claridad para Agentes y Desarrolladores:** Reglas explícitas sobre en qué nivel probar cada cambio, evitando sobrecargar la capa E2E con pruebas que corresponden a componentes o dominio.
- **Velocidad de Feedback:** Las pruebas unitarias y de componentes se ejecutan en segundos antes de comprometer cambios en Git.

### Negativas / Compromisos
- **Curva de Disciplina:** Exige mantener actualizadas las clases del POM cada vez que se refactoricen componentes clave.
- **Dependencia de Entorno para E2E:** Las pruebas de Nivel 4 requieren backend y frontend activos con base de datos levantada.

---

## 4. Referencias
- [Reglas de Gobernanza de Testing Automatizado](file:///.agents/rules/testing-governance.md)
- [Inventario Maestro de Pruebas](file:///docs/testing/inventario_de_pruebas.md)
- [ADR-0007: Ciclo de Vida SDD-SDLC](file:///docs/adr/0007_sdd_software_development_lifecycle.md)
