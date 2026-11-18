# Reglas de Gobernanza Arquitectónica y Modularidad

Este documento establece las directrices técnicas obligatorias para `backend/`, `frontend/` y `backoffice-frontend/` en el proyecto `gestion-restaurante`.

---

## 1. Regla de Oro: Prohibición de Inyecciones Directas de Repositorios
- **PROHIBIDO:** Inyectar interfaces `JpaRepository` de otros módulos de negocio o de `core` (`BranchRepository`, `AppUserRepository`, `TenantRepository`, `OrderRepository`, etc.) dentro de servicios o controladores de `modules.*`.
- **OBLIGATORIO:** Toda interacción intermodular o hacia `core` debe realizarse exclusivamente mediante:
  1. **Interfaces Públicas `.api`:** Contratos públicos en el subpaquete `.api` del módulo o de `core` (ej. `BranchPublicApi`, `UserPublicApi`, `CatalogPublicApi`, `TenantEntitlementService`, `MediaStorageService`). Los repositorios de módulos deben permanecer package-private dentro de `.internal`.
  2. **Eventos Asíncronos Desacoplados:** `ApplicationEventPublisher` + `@ApplicationModuleListener` para reacciones desacopladas (`OrderPaidEvent`, `OrderCancelledEvent`, `TenantEntitlementChangedEvent`).
- **Validación Automática:** Toda suite backend valida los límites modulares con:
  ```java
  @Test
  void verifyModularity() {
      ApplicationModules.of(RestaurantApplication.class).verify();
  }
  ```

---

## 2. Aislamiento de Capa de Acceso a Datos en Frontend (Dexie.js)
- **PROHIBIDO:** Que servicios de negocio (`PosService`, `FinanceService`, etc.) o componentes Angular importen directamente `appDb` (`RestaurantPosDb`) o consulten tablas de Dexie.js sin abstracción.
- **OBLIGATORIO:** Encapsular IndexedDB tras el Data Access Layer (DAL) en `frontend/src/app/core/repositories/` (`LocalOrderRepository`, `LocalCatalogRepository`, `LocalShiftRepository` vía token `APP_DATABASE`).
- **Beneficio:** Facilita pruebas unitarias en Vitest, desacopla el almacenamiento local de la UI y centraliza migraciones de esquema IndexedDB.

---

## 3. Estandarización de Excepciones y Formato ProblemDetail (RFC 7807)
- **PROHIBIDO:** Lanzar excepciones genéricas (`IllegalStateException`, `NoSuchElementException`, `RuntimeException`) para flujos de negocio controlados o responder errores HTTP sin contrato uniforme.
- **OBLIGATORIO:**
  - Usar excepciones tipadas en `core.common.exception` (`DomainException`, `EntityNotFoundException`, `BusinessRuleException`, `ResourceConflictException`, `ValidationException`, `UnauthorizedOperationException`, `InsufficientStockException`, `ItemSoldOutException`, `ShiftRequiredException`, `PayloadTooLargeException`) y `core.entitlements.api.EntitlementBlockedException`.
  - Centralizar el mapeo en `GlobalExceptionHandler` (`@RestControllerAdvice`) devolviendo `ProblemDetail` RFC 7807 con `errorCode`, `timestamp` y `tenantId`.
  - Deserializar uniformemente `ProblemDetail` en frontend mediante `errorInterceptor` y `NotificationService`.

---

## 4. Principio de Responsabilidad Única (SRP) y Concurrencia
- **PROHIBIDO:** Sobrecargar servicios transaccionales de aplicación con generación manual de correlativos o cálculos de zonas horarias embebidos.
- **OBLIGATORIO:** Aislar responsabilidades concurrentes (bloqueos optimistas `@Version`, actualizaciones condicionadas atómicas como cupos en `shift_menu_items` y conversión explícita de `branches.timezone` según [ADR-0006](../../docs/adr/0006_datetime_and_timezone_handling.md)).

---

## 5. Polimorfismo e Inyección Dinámica del Strategy Pattern
- **PROHIBIDO:** Inyectar implementaciones concretas de estrategias una por una en constructores y seleccionarlas con `if-else` / `switch` extensos cuando el patrón admite registro dinámico.
- **OBLIGATORIO:** Aplicar el Principio Abierto/Cerrado (OCP) inyectando listas o mapas polimórficos de estrategias autoidentificadas (`InventoryDeductionStrategy` en `InventoryStrategyFactory`, `DeductionStrategy` en `KardexService`, `StorageStrategy` en `MediaStorageServiceImpl`).

---

## 6. Modelo de Dominio Rico vs Modelo Anémico
- **PROHIBIDO:** Mantener entidades transaccionales clave (`Order`, `CashShift`, `TenantSubscription`, `SubscriptionCharge`) como bolsas anémicas de getters/setters públicos dispersando invariantes en servicios.
- **OBLIGATORIO:** Encapsular transiciones de estado, cálculos y validaciones de invariantes en métodos semánticos dentro de las propias entidades (ej. `TenantSubscription.suspend()`, `SubscriptionCharge.applyPayment()`).

---

## 7. Gobernanza SDD y Ciclo de Vida (ADR-0007)
- **PROHIBIDO:** Implementar módulos, cambios de esquema o contratos `.api` sin Spec-First (Fases 1 y 2) o marcar `[x]` tareas en `docs/tasks.md` sin sincronizar especificaciones y diagramas Mermaid.
- **OBLIGATORIO:** Cumplir las 4 fases de [ADR-0007](../../docs/adr/0007_sdd_software_development_lifecycle.md) y [sdd-architecture-diagramming](../skills/sdd-architecture-diagramming/SKILL.md) (Vía A Spec-First por defecto; Fast-Track solo para *hotfixes* con Fase 4 Sync-Always obligatoria en el mismo commit).

---

## 8. Gobernanza de Testing Automatizado (ADR-0017)
- **PROHIBIDO:** Usar `waitForTimeout()` / `sleep()` en E2E o cerrar `TASK-xxx` sin catalogar sus casos en [inventario_de_pruebas.md](../../docs/testing/inventario_de_pruebas.md).
- **OBLIGATORIO:** Aplicar la pirámide/trofeo de 4 niveles (Dominio, Componentes Angular/React, Integración Spring Modulith, E2E Playwright POM en `frontend/e2e/`) definida en [testing-governance.md](testing-governance.md) y [ADR-0017](../../docs/adr/0017_multilevel_testing_governance_and_automation_standard.md).

---

## 9. Modularización Frontend: Dumb vs Smart Components
- **PROHIBIDO:** Construir *God Components* que mezclen marcado complejo, lógica HTTP/Dexie y decenas de banderas condicionales, o inyectar servicios de red/DAL dentro de componentes presentacionales puros.
- **OBLIGATORIO:**
  1. **Presentacionales puros (*Dumb*):** En `frontend/src/app/shared/components/ui/` o `components/` locales; reciben estado por `input()` y emiten eventos por `output()`.
  2. **Contenedores (*Smart*):** En `frontend/src/app/features/<modulo>/` o `backoffice-frontend/src/pages/`; custodian llamadas HTTP/DAL, permisos RBAC y coordinación transaccional.

---

## 10. Dependencias Externas vs Implementación Propia Optimizada
- **PROHIBIDO:** Instalar librerías externas o reinventar utilidades estándar unilateralmente sin evaluación previa en Fases 1–2.
- **OBLIGATORIO:** Evaluar opciones maduras compatibles (Java 25 / Angular 22 Zoneless / React 19), diseñar alternativa nativa acotada (como `<app-image-cropper-modal>`, `<app-emoji-picker>`, `csv-exporter.ts`) y decidir conjuntamente con el usuario según impacto en bundle y mantenibilidad.

---

## 11. Reutilización Visual y Prevención de Duplicación (`backoffice-frontend` & `frontend`)
- **PROHIBIDO:** Duplicar tarjetas, modales, estados vacíos, insignias o KPIs en $\ge 3$ vistas sin extraer un primitivo reutilizable, o sobrecargar primitivos compartidos con decenas de flags booleanos en lugar de composición (`children` / `<ng-content>`).
- **OBLIGATORIO:**
  1. En `backoffice-frontend/`: reutilizar `src/components/` (`AppLayout`, `ProtectedRoute`, `AlertBanner`, `Badge`, `StatusBadge`, `Card`, `EmptyState`, `TableEmptyRow`, `KpiCard`, `Modal`, `QuotaBar`, `DailySalesChart`) con soporte completo de modo oscuro (`dark:` y variables `--bo-*`).
  2. En `frontend/`: reutilizar los 13 componentes UI Kit de `src/app/shared/components/ui/` (TailwindAdmin v3.0) más `BottomSheetComponent`.
  3. Acompañar los primitivos compartidos con pruebas unitarias de comportamiento en Vitest.
