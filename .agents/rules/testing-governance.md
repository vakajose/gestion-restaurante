# Reglas de Gobernanza de Testing Automatizado y Calidad Continua

Este documento establece la normativa técnica e inviolable que rige la estrategia de pruebas, niveles de verificación y aseguramiento de calidad en el ciclo de vida del software (SDD-SDLC) para el proyecto `gestion-restaurante`.

---

## 1. Principio Fundamental: Trofeo de Pruebas y Criterio de Nivelación

Toda nueva funcionalidad o modificación a un módulo existente debe someterse a pruebas automatizadas clasificadas estrictamente según la **Matriz de los 4 Niveles de Verificación**. Está terminantemente prohibido sobrecargar la capa E2E con validaciones que corresponden a capas inferiores, así como también dar por concluida una funcionalidad transaccional sin pruebas de integración o de dominio.

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

---

## 2. Definición Estricta de los 4 Niveles de Prueba

### Nivel 1: Pruebas Unitarias de Dominio y Reglas Puras (Backend / Frontend)
- **Alcance:** Fórmulas matemáticas, redondeos, precios con impuestos/descuentos, reglas de arqueo teórico de caja, valuación de Kardex (FIFO / Promedio Ponderado), generador de tickets correlativos, validaciones de tokens JWT y hashing criptográfico.
- **Herramientas:** JUnit 5 (Backend), Vitest (Frontend).
- **Regla:** 0 dependencias de bases de datos, 0 llamadas de red, 0 mocks pesados. Tiempo de ejecución $< 10\text{ms}$ por prueba.

### Nivel 2: Pruebas de Componentes, Signals y UI Kit (Frontend)
- **Alcance:** Reactividad de Angular Signals (`signal`, `computed`, `effect`), formularios `Signal Forms`, validaciones visuales en `<app-form-field>`, estados de botones y badges, accesibilidad (WCAG 2.2 AA), aperturas/cierres de modales y toasts.
- **Herramientas:** Angular TestBed con Vitest.
- **Regla:** Mocks ligeros de servicios (`provideHttpClientTesting()`). Prohibido verificar lógica de negocio del servidor aquí. Tiempo $< 100\text{ms}$.

### Nivel 3: Pruebas de Integración Modular y Contratos API (Backend)
- **Alcance:**
  1. Verificación de arquitectura Spring Modulith: `ApplicationModules.of(RestaurantApplication.class).verify()`.
  2. Integración de contratos públicos `.api` entre módulos (ej. `PosService` consumiendo `FinancePublicApi` o `CatalogPublicApi`).
  3. Integridad transaccional `@Transactional`, bloqueos optimistas `@Version` y concurrencia.
  4. Migraciones Flyway y restricciones de base de datos PostgreSQL (`CHECK`, `FOREIGN KEY`, `UNIQUE`).
  5. Seguridad y autorización `@PreAuthorize("hasAuthority(...)")`.
  6. Respuestas de error estandarizadas RFC 7807 (`ProblemDetail`).
- **Herramientas:** Spring Boot Test (`@SpringBootTest`, `@AutoConfigureMockMvc`).

### Nivel 4: Pruebas End-to-End (E2E) de Jornada Completa (Playwright)
- **Alcance Exclusivo:** Flujos críticos de alta sensibilidad económica y operativa que involucran interacción de usuario, navegación, red real y persistencia en base de datos:
  1. Ciclo de Jornada Operativa: Bloqueo sin turno $\rightarrow$ Apertura de turno $\rightarrow$ Venta en POS $\rightarrow$ Gasto menor con aprobación $\rightarrow$ Arqueo físico $\rightarrow$ Cierre de turno.
  2. Sincronización Offline-First: Desconexión de red $\rightarrow$ Venta en Dexie.js (IndexedDB) $\rightarrow$ Reconexión $\rightarrow$ Sync idempotente con `client_transaction_id`.
  3. Agotamiento de Cupos y Control de Stock en Tiempo Real.
  4. Onboarding inicial de Tenant y configuración de empresa.
- **Herramientas:** Playwright Test Runner oficial (`@playwright/test`).
- **Prohibición Estricta:** Prohibido probar micro-interacciones estéticas o validaciones de inputs triviales en E2E.

---

## 3. Normas Inviolables para la Automatización E2E (Playwright)

1. **Determinismo Absoluto (Zero Flakiness):**
   - **PROHIBIDO:** Usar esperas arbitrarias basadas en tiempo (`page.waitForTimeout()`, `sleep()`, `setTimeout()`).
   - **OBLIGATORIO:** Usar aserciones Web-First que reintentan automáticamente contra el DOM (`await expect(locator).toBeVisible()`, `toBeEnabled()`, `toHaveText()`).
2. **Page Object Model (POM) Obligatorio:**
   - **PROHIBIDO:** Escribir selectores CSS/DOM arbitrarios directamente en los archivos `.spec.ts`.
   - **OBLIGATORIO:** Centralizar los selectores y acciones en clases Page Object bajo `frontend/e2e/pages/` (ej. `PosPage`, `CashControlPage`, `FinancePage`). Si cambia la UI, solo se actualiza una línea en el POM.
3. **Selectores Basados en `data-testid` Semánticos:**
   - Todo elemento interactivo clave del UI Kit debe contar con `data-testid="..."` kebab-case explícito (ej. `[data-testid="pos-context-menu-btn"]`, `[data-testid="open-shift-cta-btn"]`).
4. **Reutilización de Estados de Autenticación (Fixtures):**
   - Para evitar loguearse manualmente en cada escenario, utilizar `storageState` o fixtures de Playwright con tokens pre-generados para los roles `CASHIER` y `ADMIN_TENANT`.
5. **Aislamiento e Idempotencia:**
   - Toda prueba E2E debe ser capaz de ejecutarse de forma independiente sin asumir que otra prueba corrió antes.

---

## 4. Política de Incorporación Progresiva (Boy Scout Testing & Retrospectivas)

- **Funcionalidades Nuevas:** Toda tarea de construcción (`TASK-xxx`) debe incluir como entregable sus pruebas en los niveles que correspondan según la matriz de decisión y actualizar [`docs/testing/inventario_de_pruebas.md`](file:///home/jvaca/proyectos/gestion-restaurante/docs/testing/inventario_de_pruebas.md).
- **Funcionalidades Pasadas (Deuda de QA):** No se bloquea el desarrollo para reescribir todo el histórico de golpe. Durante las retrospectivas de cada Sprint y al momento de refactorizar un módulo ("Boy Scout Rule"), se auditarán y nivelarán los casos de prueba históricos pendientes.

---

## 5. Checklist de Certificación Previa a Merge (`main` / `develop`)

Antes de realizar cualquier commit de cierre o merge a `develop`/`main`:
1. `backend`: `./mvnw test` $\rightarrow$ 100% de tests pasando (`BUILD SUCCESS`), modularidad intacta.
2. `frontend`: `npm run lint` $\rightarrow$ 0 errores, 0 warnings.
3. `frontend`: `npm test -- --watch=false` $\rightarrow$ 100% de suites de componentes pasando.
4. `e2e`: `./scripts/run-e2e.sh` o `npm run test:e2e` $\rightarrow$ 100% de la suite de regresión pasando.
