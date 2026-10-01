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
