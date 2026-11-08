# Reglas de Gobernanza Arquitectónica y Modularidad (Lecciones Aprendidas)

Este documento establece las directrices arquitectónicas de observancia obligatoria para el desarrollo del backend y frontend del proyecto `gestion-restaurante`, consolidadas tras la auditoría arquitectónica de la Iteración 1.

---

## 1. Regla de Oro: Prohibición de Inyecciones Directas de Repositorios
- **PROHIBIDO:** Inyectar directamente interfaces `JpaRepository` pertenecientes a otros módulos de negocio o al módulo `core` (tales como `BranchRepository`, `AppUserRepository`, `TenantRepository`, `OrderRepository`, etc.) dentro de servicios, controladores o componentes de cualquier módulo de negocio (`modules.*`).
- **OBLIGATORIO:** Toda interacción entre módulos de negocio, o entre un módulo de negocio y `core`, debe realizarse exclusivamente a través de:
  1. **Interfaces Públicas `.api`:** Utilizar contratos públicos expuestos en el subpaquete `.api` del módulo o de `core` (ej. `BranchPublicApi`, `UserPublicApi`, `CatalogPublicApi`). Los repositorios JPA deben permanecer con visibilidad de paquete (`package-private`) o estrictamente confinados dentro del subpaquete `.internal` o del módulo que los custodia.
  2. **Eventos Asíncronos Desacoplados:** Utilizar `ApplicationEventPublisher` y `@ApplicationModuleListener` para operaciones secundarias, efectos colaterales o integraciones desacopladas (ej. deducción de Kardex ante `OrderPaidEvent`).
- **Validación Automática:** Toda suite de pruebas debe verificar la modularidad con:
  ```java
  @Test
  void verifyModularity() {
      ApplicationModules.of(RestaurantApplication.class).verify();
  }
  ```

---

## 2. Aislamiento de Capa de Acceso a Datos en Frontend (Dexie.js)
- **PROHIBIDO:** Que los servicios de negocio de Angular (`PosService`, `FinanceService`, etc.) o componentes de interfaz de usuario importen directamente la instancia global `appDb` (`RestaurantPosDb`) o ejecuten consultas directas sobre las tablas de Dexie.js.
- **OBLIGATORIO:** Todo acceso, lectura y persistencia en IndexedDB debe encapsularse en una capa de Data Access Layer (DAL) / Repositorios Locales (`frontend/src/app/core/repositories/`, tales como `LocalOrderRepository`, `LocalCatalogRepository`, `LocalShiftRepository`).
- **Beneficio:** Permite simular (mockear) fácilmente la base de datos en pruebas unitarias, aísla la lógica de negocio de la implementación de almacenamiento y protege la UI contra cambios de esquema en IndexedDB.

---

## 3. Estandarización de Excepciones y Formato ProblemDetail (RFC 7807)
- **PROHIBIDO:** Lanzar excepciones genéricas de runtime (`IllegalStateException`, `NoSuchElementException`, `RuntimeException`) para flujos controlados de negocio en el backend.
- **PROHIBIDO:** Responder ante fallos con formatos JSON heterogéneos o códigos de error arbitrarios sin estandarización.
- **OBLIGATORIO:**
  - Definir excepciones de dominio tipadas en `core.common.exception` (`EntityNotFoundException`, `BusinessRuleException`, `ConflictException`, etc.).
  - Centralizar el manejo de excepciones en un `@RestControllerAdvice` que devuelva respuestas conformes a RFC 7807 (`ProblemDetail`), incluyendo atributos de contexto (`errorCode`, `timestamp`, `tenantId`, validaciones de campo).
  - Manejar las respuestas en el frontend a través de un interceptor HTTP que deserialice uniformemente `ProblemDetail` y presente mensajes accionables al usuario.

---

## 4. Principio de Responsabilidad Única (SRP) y Concurrencia
- **PROHIBIDO:** Sobrecargar servicios transaccionales de aplicación con responsabilidades periféricas (como generación y formateo de correlativos diarios, cálculo de husos horarios, etc.).
- **OBLIGATORIO:** Extraer componentes especializados (ej. `DailyTicketNumberGenerator`) con mecanismos de concurrencia seguros (bloqueos atómicos o secuencias particionadas) para evitar condiciones de carrera y colisiones de tickets en entornos de alto tráfico.

---

## 5. Polimorfismo e Inyección Dinámica del Strategy Pattern
- **PROHIBIDO:** Inyectar individualmente todas las clases concretas de una familia de estrategias en los constructores de servicios y seleccionar la ejecución mediante bifurcaciones manuales (`if-else` / `switch`) basadas en enums.
- **OBLIGATORIO:** Aplicar el Principio Abierto/Cerrado (OCP). Las estrategias deben identificarse a sí mismas (ej. método `getMode()`) e inyectarse dinámicamente mediante Spring en una colección o mapa polimórfico (`Map<KeyEnum, StrategyInterface>`). La adición de una nueva estrategia debe realizarse agregando una nueva clase sin modificar el servicio consumidor.

---

## 6. Modelo de Dominio Rico vs Modelo Anémico
- **PROHIBIDO:** Mantener entidades de negocio transaccionales clave (`Order`, `CashShift`, etc.) como meras estructuras de datos anémicas con getters y setters públicos indiscriminados, trasladando toda la lógica de negocio y validación de invariantes a los servicios.
- **OBLIGATORIO:** Encapsular invariantes de negocio, recálculos y transiciones de estado en métodos con semántica del dominio dentro de las propias entidades (ej. `closeShift()`, `addItem()`, `markAsPaid()`). Los servicios de aplicación deben actuar únicamente como orquestadores de persistencia y coordinación externa.

---

## 7. Gobernanza de Specification-Driven Development (SDD) y Ciclo de Vida (ADR-0007)
- **PROHIBIDO:** Iniciar la implementación de funcionalidades complejas, nuevos módulos o alteraciones de modelos/contratos sin haber analizado y diseñado previamente sus diagramas y especificaciones técnicas (Spec-First, Fases 1 y 2 de ADR-0007).
- **PROHIBIDO:** Marcar como completada cualquier tarea que modifique comportamiento, estructura de datos, contratos de API (`.api`) o estados de negocio sin actualizar sus correspondientes especificaciones y diagramas Mermaid en `docs/project/architecture/`.
- **OBLIGATORIO:**
  - Seguir el ciclo de vida SDD-SDLC de 4 fases definido en [ADR-0007](../../docs/adr/0007_sdd_software_development_lifecycle.md) y [.agents/skills/sdd-architecture-diagramming/SKILL.md](../skills/sdd-architecture-diagramming/SKILL.md):
    1. *Elicitación y Análisis (Inception):* Requerimiento (`PB-xxx`) $\rightarrow$ Caso de Uso (`UC-xxx`).
    2. *Diseño y Especificación (Elaboration / Spec-First):* Diagramas Mermaid, ERD, DTOs, `.api`, ADRs y tareas atómicas.
    3. *Construcción y Verificación (Construction):* Branch `feat/...`, TDD, `./mvnw clean test`, `npm test`.
    4. *Sincronización y Cierre (Transition / Sync-Always):* Verificación de convergencia, actualización documental y cierre atómico.
  - **Excepción Fast-Track:** Reservada exclusivamente para hotfixes críticos en producción o correcciones cosméticas menores, debiendo ejecutar siempre la Fase 4 (Sync-Always) en el mismo commit.
  - Los diagramas Mermaid son artefactos vivos de especificación y deben reflejar fielmente la realidad del sistema.

---

## 8. Gobernanza de Testing Automatizado y Pirámide Multinivel (ADR-0017)
- **PROHIBIDO:** Sobrecargar la suite E2E con pruebas cosméticas o de validación de campos que corresponden a componentes o lógica de dominio.
- **PROHIBIDO:** Usar `waitForTimeout()`, `sleep()` o retardos arbitrarios en pruebas E2E.
- **PROHIBIDO:** Cerrar una tarea de funcionalidad (`TASK-xxx`) sin registrar o actualizar sus casos de prueba en `docs/testing/inventario_de_pruebas.md`.
- **OBLIGATORIO:**
  - Seguir el estándar de 4 niveles del Trofeo de Pruebas:
    1. *Nivel 1 (Dominio Puro):* Fórmulas matemáticas, cálculo de precios, invariantes contables de gaveta, kardex y hashing en memoria ($< 10\text{ms}$).
    2. *Nivel 2 (Componentes UI):* Angular Signals, Signal Forms, UI Kit, accesibilidad WCAG 2.2 AA ($< 100\text{ms}$).
    3. *Nivel 3 (Integración Modular):* Spring Modulith `.verify()`, contratos `.api`, concurrencia `@Version`, transacciones `@Transactional`, migraciones Flyway y RFC 7807.
    4. *Nivel 4 (E2E Journeys):* Playwright oficial con Page Object Model (POM) exclusivamente para flujos críticos de dinero, ciclo de jornada completa, resiliencia offline Dexie y concurrencia.
  - Toda suite E2E debe ser determinista, utilizando Web-First assertions (`expect(locator).toBeVisible()`).
  - Ver [.agents/rules/testing-governance.md](testing-governance.md) y [ADR-0017](../../docs/adr/0017_multilevel_testing_governance_and_automation_standard.md).

---

## 9. Modularización Frontend: Componentes Presentacionales Puros (Dumb) vs Contenedores (Smart) y Prevención de God Components
- **PROHIBIDO:** Crear "God Components" (componentes monolíticos sobrecargados) que intenten resolver simultáneamente la presentación visual y la lógica transaccional de negocio mediante condicionales excesivos (`@if (isCashier && !isSupervisor && isModal)`).
- **PROHIBIDO:** Que los componentes reutilizables o presentacionales inyecten directamente servicios de persistencia HTTP o manipulen el estado global/DAL de Dexie.
- **OBLIGATORIO:**
  1. **Separación Estricta de Responsabilidades:**
     - **Componentes Presentacionales Puros (*Dumb Components*):** Ubicados en `shared/components/ui/` o subcarpetas `components/` de cada feature. Solo reciben datos mediante Signals `input()` / `input.required()` y notifican interacciones al exterior mediante `output()`. Son funciones puras de su interfaz: predecibles, altamente reutilizables y comprobables unitariamente en Vitest sin necesidad de mocks de red.
     - **Componentes Contenedores (*Smart Containers*):** Ubicados en `features/<modulo>/` (ej. `CashControlComponent`, `FinanceComponent`). Custodian la lógica de negocio, consumen servicios HTTP/DAL, resuelven permisos RBAC y coordinan las mutaciones transaccionales.
  2. **Reutilización Proactiva (Principio DRY):** Toda tabla, visor, tarjeta de datos o cola de trabajo con similitud funcional entre roles (ej. cola de autorizaciones para cajero vs supervisor) debe abstraerse en un componente presentacional configurable por inputs (`viewMode`, `filters`), evitando duplicar marcado HTML y estilos visuales en múltiples vistas.


