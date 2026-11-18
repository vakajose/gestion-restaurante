# Reglas de Gobernanza de Testing Automatizado y Calidad Continua

Normativa técnica obligatoria que rige la estrategia de pruebas, niveles de verificación y certificación continua bajo [ADR-0017](../../docs/adr/0017_multilevel_testing_governance_and_automation_standard.md).

---

## 1. Trofeo de Pruebas y Criterio de Nivelación

Toda nueva funcionalidad o modificación debe someterse a pruebas clasificadas en la **Matriz de los 4 Niveles**. Está prohibido sobrecargar la capa E2E con validaciones de formulario o lógica unitaria que corresponden a niveles inferiores.

```text
          / \
         / E2E \        Nivel 4: Flujos Críticos Cross-Módulo & Dinero (Playwright POM)
        /-------\
       / Integr. \      Nivel 3: Contratos .api, Transacciones, Flyway, Modulith
      /-----------\
     / Componentes \    Nivel 2: Angular Signals / React RTL, UI Kit, A11y
    /---------------\
   /    Dominio      \  Nivel 1: Matemáticas, Arqueo, Precios, Kardex, JWT/Storage
  /-------------------\
```

---

## 2. Definición Estricta de los 4 Niveles de Prueba

### Nivel 1: Pruebas Unitarias de Dominio y Reglas Puras
- **Alcance:** Fórmulas matemáticas, precios, arqueo teórico, valuación Kardex, hashing y validación JWT, utilidades (`csv-exporter.ts`, `formatters.ts`, `runtimeEnv.ts`).
- **Herramientas:** JUnit 5 (`backend/`), Vitest (`frontend/` y `backoffice-frontend/`).
- **Regla:** Sin BD real, sin red, sin mocks pesados ($< 10\text{ ms}$ por prueba).

### Nivel 2: Pruebas de Componentes, Signals y UI Kit
- **Alcance:**
  - **ERP Angular 22 (`frontend/`):** Reactividad (`signal`, `computed`, `effect`), formularios `ReactiveFormsModule` / `FormsModule`, validación en `<app-form-field>`, los 13 componentes UI Kit + `BottomSheetComponent`, accesibilidad WCAG 2.2 AA.
  - **Backoffice React 19 (`backoffice-frontend/`):** Páginas, rutas protegidas, `ThemeProvider`, primitivas compartidas (`UiPrimitives.test.tsx`) con Vitest + React Testing Library (`jsdom`).
- **Regla:** Mocks ligeros de servicios HTTP (`provideHttpClientTesting()` o stubs Vitest).

### Nivel 3: Pruebas de Integración Modular y Contratos API (`backend/`)
- **Alcance:**
  1. `ApplicationModules.of(RestaurantApplication.class).verify()` (`RestaurantModularityTests`).
  2. Contratos `.api` entre módulos y puertos SPI (`core.entitlements`, `core.storage`).
  3. Transacciones `@Transactional`, `@Version`, migraciones Flyway (`V1`–`V24`, `FlywayMigrationTest`).
  4. Seguridad `@PreAuthorize`, aislamiento multi-tenant y respuestas `ProblemDetail` RFC 7807.
- **Herramientas:** Spring Boot Test (`@SpringBootTest`, `@AutoConfigureMockMvc`, perfil `test` H2 modo PostgreSQL).

### Nivel 4: Pruebas End-to-End (E2E) de Jornada Completa (`frontend/e2e/`)
- **Alcance Exclusivo:** Flujos críticos económicos y operativos multi-módulo (turno $\rightarrow$ venta POS $\rightarrow$ gasto $\rightarrow$ autorización $\rightarrow$ arqueo/cierre, resiliencia offline Dexie, catálogo/menús diarios/cupos, regresión Sprint 5/6).
- **Herramientas:** Playwright Test Runner oficial (`@playwright/test`). *(Los scripts legados de CDP `:9222` en `scripts/e2e/` están retirados).*

---

## 3. Normas Inviolables para E2E (Playwright POM)

1. **Determinismo (Zero Flakiness):** Prohibido usar `page.waitForTimeout()`, `sleep()` o `setTimeout()`. Usar aserciones Web-First (`await expect(locator).toBeVisible()`, `toBeEnabled()`, `toHaveText()`).
2. **Page Object Model (POM) Obligatorio:** Centralizar selectores y acciones en `frontend/e2e/pages/` (`AuthPage`, `PosPage`, `CashControlPage`, `FinancePage`). Prohibido dispersar selectores crudos en `.spec.ts`.
3. **Selectores Semánticos `data-testid`:** Usar atributos `data-testid="..."` en kebab-case para elementos interactivos críticos.
4. **Aislamiento y Limpieza:** Cada `.spec.ts` debe poder ejecutarse de forma independiente sin depender de corridas previas ni guardar capturas en el repositorio.

---

## 4. Política de Catalogación Continua

- Toda tarea técnica (`TASK-xxx`) debe entregar sus pruebas en los niveles aplicables y catalogarlas en [docs/testing/inventario_de_pruebas.md](../../docs/testing/inventario_de_pruebas.md).
- Las regresiones UAT validadas manualmente en `release/sprint-X` se documentan en `docs/testing/casos_uat_y_regresion.md` para su automatización progresiva.

---

## 5. Checklist de Certificación Previa a Merge (`develop` / `release/*` / `main`)

Alineado exactamente con [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml):
1. **Backend:** `cd backend && ./mvnw -B clean test spotbugs:check -Psecurity` $\rightarrow$ 100% tests pasando y 0 hallazgos SpotBugs/FindSecBugs.
2. **ERP Frontend:** `cd frontend && npm run lint && npm test -- --watch=false && npm run build` $\rightarrow$ 0 errores ESLint/TS y 100% Vitest verde.
3. **Backoffice React:** `cd backoffice-frontend && npm run lint && npm test -- --run && npm run build` $\rightarrow$ 0 errores ESLint/TS y 100% Vitest/RTL verde.
4. **DevSecOps:** `./scripts/security/run-security-scan.sh --quick` $\rightarrow$ 0 secretos ni fallos de hardening Docker.
5. **E2E Playwright:** `./scripts/run-e2e.sh` (o `cd frontend && npm run test:e2e`) $\rightarrow$ 100% de la suite POM en verde contra entorno activo.
