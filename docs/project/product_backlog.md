# Product Backlog y Registro de Nuevas Funcionalidades

Este documento centraliza el Product Backlog del sistema, ordenado estrictamente por **nivel de prioridad**. Aquí se registran las iniciativas, requerimientos, historias de usuario y decisiones arquitectónicas que surgen durante el desarrollo pero que **no se implementarán de forma inmediata**, sirviendo de repositorio para la planificación de futuros sprints.

---

## Matriz de Priorización

| Nivel | Clasificación | Criterio de Entrada |
| :--- | :--- | :--- |
| **P0** | **Crítica / Máxima** | Bloqueante estructural para el modelo de negocio, gobernanza SaaS, seguridad/RBAC o ciclo de vida inicial del inquilino (previo a rollout). |
| **P1** | **Alta** | Ampliación clave de la operación del cliente (multi-sucursal, catálogo avanzado, resiliencia arquitectónica y desacoplamiento). |
| **P2** | **Media** | Integraciones periféricas (hardware, tributaria, soporte multi-motor, refactors internos de dominio y polimorfismo). |
| **P3** | **Baja / Futuro** | Mejoras de experiencia operativa avanzada (KDS avanzado, plano interactivo de mesas). |

---

## 1. Prioridad P0 (Máxima Prioridad)

### PB-001: Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante)
- **Categoría:** Ciclo de Vida del Tenant / Onboarding
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto:** Actualmente el sistema asume la existencia estática o previa de tenants en base de datos. Se requiere un flujo guiado y automatizado para dar de alta a nuevos clientes comerciales.
- **Alcance Funcional y Técnico:**
  - Registro inicial del cliente / empresa (Razón Social, Nombre Comercial, Identificación Fiscal/NIT, logo, sector).
  - Creación y configuración de la primera sucursal principal:
    - Nombre, dirección física, teléfono.
    - Zona horaria de la sucursal (según [ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_timezone_standard.md), por defecto `America/La_Paz`).
    - Moneda base y símbolos monetarios.
  - Asignación y creación de la cuenta del Administrador Propietario del Restaurante.
  - Parametrización inicial operativa (política de inventario predeterminada, turnos de caja iniciales).
- **Criterios de Aceptación:**
  - El asistente de onboarding completa el registro creando el tenant, la sucursal principal y el usuario administrador en una única transacción atómica.
  - Las credenciales iniciales se hashean con Argon2id y se emite el primer token JWT de bienvenida.
  - La sucursal inicial queda configurada con valores por defecto funcionales para operar de inmediato.

---

### PB-002: Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner)
- **Categoría:** Gobernanza SaaS / Seguridad
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto:** Se requiere un rol administrativo exclusivo para los desarrolladores y dueños del sistema (proveedores del servicio), diferenciado de los administradores de los restaurantes inquilinos.
- **Alcance Funcional y Técnico:**
  - **Aislamiento Supra-Tenant:** Este rol opera fuera del ámbito de un `tenant_id` específico; tiene visibilidad y control sobre la totalidad de la plataforma.
  - **Gestión de Inquilinos (Tenants):** Activación, suspensión temporal, bloqueo por mora o baja de cuentas de clientes.
  - **Parámetros de Inicialización:** Configuración de semillas globales, catálogos base sugeridos y feature flags del sistema.
  - **Métricas y Monitoreo de Rendimiento:**
    - Tiempos de respuesta y rendimiento de autenticación/login.
    - Tasa de error general, latencia de base de datos y volumen de transacciones globales.
    - Detección de abusos, intentos fallidos de autenticación y auditoría de seguridad a nivel infraestructura.
- **Criterios de Aceptación:**
  - Los endpoints con scope supra-tenant rechazan cualquier token que no contenga el rol `SUPERADMIN`.
  - La suspensión de un tenant invalida las sesiones de todos los usuarios de dicho tenant en su siguiente petición.
  - Panel de telemetría capaz de reportar métricas agregadas por inquilino.

---

### PB-003: Backoffice Independiente (Frontend Desacoplado + BFF Dedicado)
- **Categoría:** Arquitectura / Plataforma
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog (Por Diseñar)`
- **Contexto y Justificación:**
  - Las herramientas del Árbitro del Sistema no deben coexistir dentro del frontend operativo de los clientes (restaurantes).
  - Incluir el backoffice en el frontend del cliente sobrecargaría el bundle innecesariamente, expondría código y rutas internas a usuarios finales (superficie de ataque) y acoplaría despliegues de soporte con la operación de los restaurantes.
- **Lineamientos Arquitectónicos:**
  - **Frontend Independiente:** Una aplicación web propia (Backoffice Web SPA), con diseño orientado a consola de administración, métricas y gestión de tenants.
  - **BFF Dedicado (Backend For Frontend):** Capa o API de backend exclusiva para el Backoffice, encargada de orquestar operaciones a nivel plataforma, parámetros de rendimiento y administración de tenants con controles de seguridad y rate limiting especializados.
- **Criterios de Aceptación:**
  - El frontend de cliente no contiene rutas, componentes ni modelos relativos al Backoffice de Superadmin.
  - El BFF del Backoffice exige autenticación mediante mTLS o tokens con audiencia (`aud`) exclusiva.

---

### PB-017: Gestión y Configuración de Empresa (Tenant) y Sucursales
- **Categoría:** Configuración Organizacional / Core Negocio
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog`
- **Contexto:** Para que un restaurante opere comercialmente de forma autónoma, el administrador debe poder gestionar la información legal, operativa y geográfica de su empresa y de todas sus sucursales (activación, horarios, zona horaria, moneda, políticas fiscales y datos de contacto). Actualmente la base de datos cuenta con tablas `tenants` y `branches`, pero no existen endpoints de administración ni interfaz visual para su gestión dinámica.
- **Alcance Funcional y Técnico:**
  - **Backend:**
    - CRUD completo de información de Empresa/Tenant en `core.config`: Razón Social, Nombre Comercial, Identificación Fiscal/NIT, logo, sector y régimen tributario.
    - CRUD completo de Sucursales asociadas al Tenant: Nombre, código interno único, dirección física, geolocalización, teléfono, zona horaria IANA ([ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_timezone_standard.md)), moneda base (BOB, USD, etc.) y estado (activa/inactiva).
    - Parametrización operativa por sucursal: política de inventario (`INVENTORY_DEDUCTION_MODE`), tolerancia de arqueo de caja y límites de caja chica.
    - Aseguramiento de aislamiento multi-tenant estricto mediante `TenantContextHolder` (prohibido que un tenant acceda o modifique sucursales de otro tenant).
  - **Frontend:**
    - Vistas administrativas en TailwindAdmin v2.0:
      - Pantalla "Configuración de Empresa": Formulario con datos fiscales, logo y parámetros globales.
      - Pantalla "Gestión de Sucursales": Listado de sucursales con tarjetas/tabla, badges de estado, formulario modal/página para crear y editar sucursal.
- **Criterios de Aceptación:**
  - Un administrador de tenant solo puede listar, consultar y modificar las sucursales pertenecientes a su propio `tenant_id`. Intentar acceder a otra sucursal retorna HTTP 404/403.
  - La creación y actualización de sucursal exige una zona horaria IANA válida y un código interno único por tenant.
  - La desactivación de una sucursal bloquea inmediatamente la apertura de nuevos turnos de caja y el registro de órdenes para esa sucursal.
  - La interfaz de usuario refleja los cambios inmediatamente y proporciona feedback claro de guardado y errores de validación.

---

### PB-018: Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios por Tenant
- **Categoría:** Seguridad / Autenticación y Autorización
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog`
- **Contexto:** Se requiere una administración granular de los colaboradores del restaurante. Cada usuario debe tener roles predeterminados (`SUPERADMIN`, `ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`), acceso delimitado a sucursales específicas, y una experiencia de navegación en Sidebar/Topbar restringida a sus permisos, además de poder gestionar su propio perfil.
- **Alcance Funcional y Técnico:**
  - **Backend:**
    - Entidades y tablas relacionales para asignación de usuarios a sucursales (`user_branches` vinculando `app_users` con `branches`).
    - Matriz RBAC definida con roles:
      - `SUPERADMIN`: Control global de plataforma (fuera de tenant).
      - `ADMIN_TENANT`: Dueño/Administrador de empresa; acceso total a sucursales, usuarios, catálogo, finanzas y reportes de su tenant.
      - `BRANCH_MANAGER`: Gerente de sucursal(es) asignada(s); gestión de personal de su local, turnos, inventario local y gastos.
      - `CASHIER`: Cajero; acceso estricto al POS, apertura/cierre de turnos de caja asignados y órdenes locales.
      - `KITCHEN`: Personal de cocina; acceso a KDS / comanderas y visualización de pedidos en preparación.
    - Endpoints CRUD de usuarios (`/api/v1/users`) protegidos por `@PreAuthorize` permitiendo solo a administradores crear, editar, suspender usuarios y asociarles sucursales autorizadas.
    - Endpoints de perfil personal (`/api/v1/auth/me` GET/PUT) para actualizar datos básicos (nombre, teléfono, correo) y cambio seguro de contraseña (validando contraseña actual con Argon2id).
  - **Frontend:**
    - Pantalla "Gestión de Usuarios": Tabla de colaboradores por tenant, badges de rol legibles, estado activo/bloqueado, selector multiselección de sucursales autorizadas.
    - Filtrado dinámico de Sidebar y Topbar: Los enlaces de navegación (POS, Catálogo, Inventario, Finanzas, Configuración) se renderizan condicionalmente evaluando los roles del usuario autenticado.
    - Pantalla "Mi Perfil": Tarjeta de perfil con avatar, visualización amigable de rol (ej. "Administrador de Empresa", "Cajero", "Gerente de Local"), sucursales autorizadas y formulario para actualización de datos y contraseña.
    - Guards de ruta en Angular que redirigen o bloquean accesos directos por URL no autorizados.
- **Criterios de Aceptación:**
  - Un usuario con rol `CASHIER` solo tiene visibilidad del módulo POS y Turnos de Caja en el Sidebar; las rutas administrativas (`/admin/*`, `/catalog/manage`) rechazan el acceso.
  - Un usuario solo puede operar en las sucursales que tiene asignadas.
  - La vista "Mi Perfil" permite modificar nombre y contraseña pero prohíbe auto-escalar roles.
  - Al crear un usuario se encripta la contraseña con Argon2id y se valida unicidad de username/email dentro del tenant.

---

### PB-019: Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa
- **Categoría:** Autenticación / UX Multi-Tenant
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog`
- **Contexto:** En un entorno multi-inquilino y multi-sucursal, el login debe identificar la empresa a la que pertenece el usuario (mediante slug en subdominio o campo `tenantIdentifier` en formulario). Además, si un usuario (como `ADMIN_TENANT` o `BRANCH_MANAGER`) administra múltiples locales, debe contar con un selector en el Topbar para cambiar el contexto operativo de la sucursal activa sin tener que reloguearse, mientras que si solo opera en 1 sucursal, el flujo debe ingresar directamente.
- **Alcance Funcional y Técnico:**
  - **Backend:**
    - Login adaptativo que recibe `tenantIdentifier` (o slug de URL) + `username`/`email` + `password`.
    - Generación de token JWT enriquecido con claims: `tenantId`, `userId`, `roles`, y lista de sucursales autorizadas (`branchIds`).
    - Soporte de cabecera `X-Branch-Id` en peticiones HTTP para definir el contexto de sucursal activa en endpoints transaccionales, validando en el backend que el usuario realmente pertenezca a dicha sucursal.
  - **Frontend:**
    - Formulario de Login adaptativo que solicita Identificador de Empresa/Tenant (o lo infiere automáticamente del hostname).
    - Post-Login Routing:
      - Si el usuario tiene acceso a exactamente 1 sucursal: Se establece automáticamente como sucursal activa en el estado global y se redirige a la vista inicial según su rol (ej. `/pos` para cajero, `/dashboard` para administrador).
      - Si el usuario tiene acceso a 2 o más sucursales: Se selecciona por defecto la última utilizada o la principal, mostrando un selector prominente en el Topbar.
    - Componente Selector de Sucursal en Topbar (TailwindAdmin): Dropdown interactivo con lista de sucursales autorizadas del usuario, indicando la sucursal activa actual con un badge.
    - Servicio reactivo `BranchContextService` con Signals de Angular que notifica a toda la aplicación cuando cambia la sucursal activa, refrescando datos de POS, turnos e inventario sin recargar la página.
- **Criterios de Aceptación:**
  - Los usuarios con una única sucursal asignada nunca son obligados a pasar por un paso manual de selección de sucursal.
  - El selector del Topbar solo lista las sucursales a las que el usuario tiene acceso explícito dentro de su tenant.
  - Al cambiar de sucursal en el selector, todas las llamadas subsiguientes de API y consultas locales a IndexedDB apuntan inmediatamente al nuevo `branch_id`.
  - El backend rechaza peticiones con `X-Branch-Id` si la sucursal no pertenece al `tenant_id` del token o si el usuario no tiene permisos en ella (HTTP 403).

---

## 2. Prioridad P1 (Alta Prioridad)

### PB-004: Transferencias de Insumos entre Sucursales (`StockTransfer`)
- **Categoría:** Inventario Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** Las cadenas de restaurantes necesitan mover ingredientes entre sucursales para equilibrar excedentes y faltantes de stock sin recurrir a compras externas redundantes.
- **Alcance Funcional y Técnico:**
  - Solicitud, despacho y confirmación de recepción de insumos entre sucursales del mismo inquilino.
  - Registro de movimientos en Kardex en ambas sucursales con control de stock en tránsito.
  - Validación de existencia suficiente en la sucursal de origen antes de autorizar el despacho.
- **Criterios de Aceptación:**
  - La transferencia genera un movimiento de salida en origen y un movimiento de entrada en destino al confirmarse la recepción.
  - El stock en tránsito no está disponible para venta ni consumo en ninguna de las dos sucursales hasta su recepción efectiva.
  - No se permiten transferencias entre sucursales de distintos tenants.

---

### PB-005: Consolidación Financiera y Reportería Multi-Sucursal
- **Categoría:** Finanzas / BI
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** Los propietarios de negocios con múltiples locales requieren una visión agregada para evaluar el rendimiento comercial y financiero de su cadena.
- **Alcance Funcional y Técnico:**
  - Vista ejecutiva global para el dueño del restaurante con múltiples sucursales.
  - Rentabilidad agregada, comparativa de ventas entre locales y análisis consolidado de mermas y gastos.
  - Exportación de reportes consolidados en formato PDF y Excel (CSV).
- **Criterios de Aceptación:**
  - El dashboard permite filtrar por rango de fechas (respetando zonas horarias de cada sucursal) y agrupar por sucursal.
  - Las cifras consolidadas cuadran al 100% con los balances de arqueo de caja de los turnos cerrados individuales.

---

### PB-011: Desacoplamiento de Repositorios en Módulos de Negocio hacia core.security y core.common
- **Categoría:** Arquitectura Backend / Modularidad (Spring Modulith)
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** La auditoría técnica de la Iteración 1 reveló que `OrderServiceImpl` (en `modules.pos.internal`) inyecta directamente `BranchRepository` (de `core.common.repository`), mientras que `CashShiftService` y `ExpenseService` (en `modules.finance.internal`) inyectan directamente `AppUserRepository` (de `core.security`). Esto viola las fronteras modulares y acopla módulos de negocio con repositorios JPA de persistencia interna.
- **Alcance Funcional y Técnico:**
  - Creación de contratos públicos en subpaquetes `.api`:
    - `core.common.api.BranchPublicApi`: interfaz que expone métodos de consulta como `getBranchZoneId(UUID branchId)`, `findBranchSummary(UUID branchId)`, `isBranchActive(UUID branchId)`.
    - `core.security.api.UserPublicApi`: interfaz que expone métodos como `findUserSummary(UUID userId)`, `isUserActive(UUID userId)`.
  - Creación de implementaciones internas que encapsulan el acceso a `BranchRepository` y `AppUserRepository`.
  - Refactorización de `OrderServiceImpl`, `CashShiftService`, `ExpenseService` y clases de inicialización para inyectar únicamente `BranchPublicApi` y `UserPublicApi`.
  - Restricción de visibilidad de `BranchRepository` y `AppUserRepository` para evitar nuevas inyecciones directas accidentales.
- **Criterios de Aceptación:**
  - Ningún módulo de negocio (`modules.*`) contiene imports directos de `BranchRepository` ni de `AppUserRepository`.
  - Todas las pruebas unitarias y de integración de `pos` y `finance` se ejecutan exitosamente mockeando exclusivamente las APIs públicas.
  - `ApplicationModules.of(RestaurantApplication.class).verify()` se ejecuta y valida la modularidad al 100% sin advertencias de acoplamiento no permitido.

---

### PB-012: Estandarización de Excepciones de Dominio y Formato ProblemDetail (RFC 7807) con @RestControllerAdvice centralizado
- **Categoría:** Arquitectura Backend / API REST & Resiliencia
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** Actualmente, los módulos arrojan excepciones genéricas del JDK (`IllegalStateException`, `NoSuchElementException`, `IllegalArgumentException`) ante fallos de negocio o validación. Esto genera respuestas HTTP inconsistentes (status 500 para errores de cliente, formatos JSON heterogéneos) dificultando el manejo de errores en el frontend y en la capa offline.
- **Alcance Funcional y Técnico:**
  - Definición de jerarquía de excepciones de dominio en `core.common.exception`:
    - `DomainException` (clase base abstracta con código de error y mensaje).
    - `EntityNotFoundException` (HTTP 404).
    - `BusinessRuleException` (HTTP 422 / 400).
    - `ConflictException` (HTTP 409).
    - `InsufficientStockException`, `ShiftClosedException`, etc.
  - Implementación de `@RestControllerAdvice` centralizado en `core.common` que capture estas excepciones y genere respuestas estándar basadas en RFC 7807 (`org.springframework.http.ProblemDetail`).
  - Enriquecimiento del `ProblemDetail` con propiedades adicionales: `timestamp`, `errorCode`, `tenantId`, y lista detallada de errores de campos para validaciones `@Valid`.
  - Actualización de los servicios de negocio para lanzar las nuevas excepciones tipadas en lugar de excepciones de runtime genéricas.
  - Interceptor HTTP en Angular para deserializar y presentar notificaciones consistentes ante respuestas `application/problem+json`.
- **Criterios de Aceptación:**
  - El 100% de los endpoints de la API REST responden con formato estándar `application/problem+json` ante errores 4xx y 5xx.
  - La respuesta contiene campos RFC 7807 (`type`, `title`, `status`, `detail`, `instance`) más extensiones de auditoría sin filtrar stack traces sensibles en producción.
  - Suite de pruebas unitarias cubriendo `@RestControllerAdvice` para cada tipo de excepción.

---

### PB-016: Capa de Repositorio Local en Frontend (Data Access Layer para Dexie.js aislando appDb)
- **Categoría:** Frontend / Arquitectura y Dexie.js
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** En el frontend, servicios como `PosService` y `FinanceService` importan directamente la instancia singleton `appDb` de Dexie (`RestaurantPosDb`) y realizan operaciones directas sobre las tablas IndexedDB (`appDb.orders.put()`, `appDb.shifts.toArray()`). Esto acopla la lógica de negocio con la implementación concreta de base de datos del navegador y dificulta la realización de pruebas unitarias aisladas.
- **Alcance Funcional y Técnico:**
  - Diseño e implementación de la capa Data Access Layer (DAL) en `frontend/src/app/core/repositories/`:
    - `LocalOrderRepository`: encapsula persistencia de órdenes offline, actualización de estado sincronizado, conteo y filtrado.
    - `LocalCatalogRepository`: encapsula sincronización local de platos y categorías, búsquedas rápidas por texto y filtrado por categoría.
    - `LocalShiftRepository`: encapsula almacenamiento y consulta del turno activo local.
    - `LocalExpenseRepository`: encapsula almacenamiento local de gastos y evidencias.
  - Refactorizar `PosService` y `FinanceService` para inyectar estos repositorios locales mediante inyección de dependencias de Angular (`inject(...)`), eliminando toda referencia directa a `appDb`.
  - Definición de interfaces de repositorio para facilitar la creación de mocks en tests unitarios.
- **Criterios de Aceptación:**
  - Ningún archivo fuera de `frontend/src/app/core/repositories/` o `app.database.ts` importa directamente `appDb`.
  - Los tests unitarios de `PosService` y `FinanceService` pueden ejecutarse con mocks de repositorios sin inicializar IndexedDB real.
  - El comportamiento offline-first (persistencia local inmediata, cola de sincronización e idempotencia con `X-Client-Transaction-Id`) se mantiene 100% operativo sin regresiones.

---

### PB-020: Motor de Promociones, Combos, Cupones y Modificadores de Precio en POS
- **Categoría:** POS / Ventas y Marketing
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** La operación real de un restaurante requiere promociones y flexibilidad de precios: descuentos porcentuales o de monto fijo, combos o paquetes (ej. plato + bebida a precio promocional), cupones de descuento temporales y modificadores de productos (ingredientes extras con recargo o variantes de tamaño).
- **Alcance Funcional y Técnico:**
  - **Backend:**
    - Tablas y entidades JPA para:
      - `promotions`: Reglas de descuento (porcentaje, monto fijo, 2x1, vigencia por fecha y horario, días de la semana, sucursales aplicables).
      - `combos` y `combo_items`: Definición de combos con selección de platos o bebidas alternativas y precio cerrado.
      - `coupons`: Cupones de descuento con código alfanumérico, límite de usos y fecha de caducidad.
      - `modifiers`: Modificadores de plato (ej. "Término de carne", "Extra queso +$1.50", "Sin cebolla").
    - Motor de precios en `modules.pos`: Servicio evaluador de promociones y recálculo de precios de orden que aplica descuentos en cascada y valida restricciones.
    - Persistencia en `orders` y `order_items` de los modificadores elegidos, cupones aplicados y desglose de descuentos para trazabilidad tributaria y financiera.
    - Integración con Kardex: Las recetas de los combos y de platos con modificadores (ej. extra tocino) deben reflejarse en deducciones automáticas de stock.
  - **Frontend:**
    - Interfaz en POS para selección de modificadores y extras al hacer clic en un plato.
    - Sección de Combos en catálogo de POS con flujo interactivo de armado de combo.
    - Input de aplicación de cupón de descuento en el resumen del carrito con validación inmediata.
- **Criterios de Aceptación:**
  - El carrito del POS y el ticket final reflejan con exactitud los precios base, modificadores agregados, descuentos aplicados y subtotal neto.
  - Las promociones con restricción horaria o de día se aplican únicamente si coinciden con la hora local de la sucursal activa.
  - Las deducciones en Kardex descuentan correctamente los insumos correspondientes a combos y modificadores adicionales.
  - Soporte de almacenamiento de reglas de promociones activas en Dexie.js para funcionamiento en modo offline.

---

### PB-021: Flujo Coherente de Inicialización de Stock y Recepción de Insumos por Sucursal
- **Categoría:** Inventario / Kardex & Abastecimiento
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** Existe una confusión operativa entre registrar un insumo en el Catálogo Maestro y asignarle stock físico en una sucursal. Además, se requiere un flujo ordenado y auditable para el ingreso y ajuste de mercadería diferenciando estrictamente: (1) Carga de Apertura (`INITIAL_STOCK`), (2) Recepción de Compras de Proveedores (`PURCHASE`), y (3) Ajustes físicos por merma, rotura o desvío (`ADJUSTMENT`).
- **Alcance Funcional y Técnico:**
  - **Diferenciación conceptual:**
    - Catálogo Maestro (`ingredients`): Define nombre, unidad de medida (`KG`, `LT`, `UNIT`), stock mínimo y costo de referencia. No posee stock físico directo.
    - Stock por Sucursal (`branch_stock`): Vincula un insumo con una sucursal específica, almacenando saldo actual y costo promedio ponderado local.
  - **Backend:**
    - Endpoint y servicio de Inicialización de Stock (`INITIAL_STOCK`): Permite establecer el saldo inicial de un insumo en una sucursal una única vez. Si el insumo ya registra movimientos previos en la sucursal, la operación se rechaza.
    - Endpoint y flujo de Recepción de Compras (`PURCHASE`): Registro de recepción con proveedor, número de factura/guía, fecha de compra, insumos recibidos, cantidades y costos unitarios de compra. Actualiza automáticamente el costo ponderado y registra entradas en `kardex_movements`.
    - Endpoint de Ajustes de Inventario (`ADJUSTMENT`): Ajustes por inventario físico con clasificación obligatoria (`MERMA`, `VENCIMIENTO`, `DIFERENCIA_CONTEO`), tipo de movimiento (`ADJUSTMENT_IN` / `ADJUSTMENT_OUT`), motivo y usuario responsable.
  - **Frontend:**
    - Pantalla de Recepción de Compras: Formulario intuitivo con selección de proveedor, fecha, carga de ítems y costo total.
    - Pantalla de Ajustes de Stock: Formulario para ingresar recuento físico real, cálculo automático de diferencia y selección obligatoria del motivo del ajuste.
    - Historial de movimientos de Kardex con filtros por tipo (`INITIAL_STOCK`, `PURCHASE`, `SALE_DEDUCTION`, `ADJUSTMENT`).
- **Criterios de Aceptación:**
  - El sistema rechaza un registro `INITIAL_STOCK` si el insumo ya cuenta con movimientos en el Kardex de esa sucursal (debe utilizarse compra o ajuste).
  - Toda compra recibida actualiza el saldo físico en `branch_stock` e incrementa el Kardex de forma atómica e inmutable.
  - Los ajustes de stock requieren obligatoriamente motivo descriptivo y usuario autenticado en la auditoría.
  - La fórmula de costo promedio ponderado se aplica correctamente en cada recepción de compra con precio diferente al costo anterior.

---

### PB-022: Corrección y Estabilización de Suite de Pruebas Frontend en CI Pipeline (Vitest / JSDOM en GitHub Actions)
- **Categoría:** CI/CD & Calidad / Infraestructura Frontend
- **Prioridad:** `P0 - Crítica (Bloqueante en CI)`
- **Estado:** `Implementado / Resuelto`
- **Contexto:** En cada commit y push hacia el repositorio remoto, el job `Frontend CI (Angular 22)` en GitHub Actions (`.github/workflows/ci.yml`) fallaba a los 23 segundos con error de configuración al intentar ejecutar `npm test -- --watch=false --browsers=ChromeHeadless`. Angular 22 utiliza el nuevo builder `@angular/build:unit-test` respaldado por Vitest y JSDOM. El argumento legado `--browsers` exigía la presencia de runners de navegador Vitest (`@vitest/browser-playwright`, etc.) no instalados, interrumpiendo el flujo continuo de integración.
- **Alcance Funcional y Técnico:**
  - Actualizar `.github/workflows/ci.yml` eliminando el flag legado `--browsers=ChromeHeadless` y empleando `npm test -- --watch=false`.
  - Habilitar la ejecución nativa y headless con JSDOM en Node.js 22.
  - Verificar localmente la suite completa de 103 pruebas unitarias de frontend, linting estricto y pruebas modulares/unitarias de backend (49 pruebas).
- **Criterios de Aceptación:**
  - El workflow `ci.yml` ejecuta los tests unitarios frontend en modo CI sin fallos de flags de navegador.
  - La suite de 103 tests en Angular 22 pasa al 100% en < 5 segundos en entorno JSDOM headless.

---

## 3. Prioridad P2 (Media Prioridad)

### PB-006: Desacoplamiento y Compatibilidad Multi-Motor SQL
- **Categoría:** Arquitectura de Datos
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** Aunque el sistema se basa en PostgreSQL, se debe asegurar que las sentencias y estructuras de base de datos no utilicen dependencias propietarias innecesarias que impidan la portabilidad futura a otros motores relacionales.
- **Alcance Funcional y Técnico:**
  - Complementar documentación y patrones de diseño (Repository, Strategy, abstracción de dialectos) para permitir la integración con otros motores relacionales sin requerir reescrituras profundas de código de negocio.
  - Asegurar que consultas nativas en JPA se mantengan en el estándar ANSI SQL siempre que sea posible.
- **Criterios de Aceptación:**
  - Las entidades y consultas JPQL/HQL se ejecutan sin errores con dialectos estándares alternativos en pruebas de concepto.

---

### PB-007: Facturación Electrónica Oficial
- **Categoría:** Integraciones Fiscales
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** Cumplimiento de normativas tributarias nacionales para emisión de comprobantes fiscales electrónicos autorizados.
- **Alcance Funcional y Técnico:**
  - Emisión de facturas electrónicas e integración con servicios tributarios nacionales normados (SIN / AFIP / SAT / DIAN según país).
  - Generación de Código Único de Factura, firma digital en XML/JSON y código QR tributario en ticket.
- **Criterios de Aceptación:**
  - La orden facturada genera el paquete fiscal firmado y obtiene código de autorización del ente tributario.
  - En caso de indisponibilidad tributaria, la factura se encola para emisión por contingencia.

---

### PB-008: Módulo de Impresión Física ESC/POS (Comanderas de Red)
- **Categoría:** Hardware / POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** En la cocina y el salón del restaurante, los tickets deben imprimirse automáticamente en impresoras térmicas ESC/POS conectadas a la red local.
- **Alcance Funcional y Técnico:**
  - Envío directo de tickets de comanda y recibos a impresoras térmicas ESC/POS conectadas a la red local (TCP/IP).
  - Ruteo por estación: Platos calientes a cocina, bebidas a barra, recibo al cajero.
- **Criterios de Aceptación:**
  - Al confirmarse una orden en el POS, las comandas se envían a las impresoras de red asignadas según la categoría del ítem.

---

### PB-013: Evolución a Modelo de Dominio Rico (Rich Domain Model) en Entidades Transaccionales (Order y CashShift)
- **Categoría:** Arquitectura de Dominio / DDD
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** Actualmente, `Order` y `CashShift` operan como entidades anémicas dominadas por getters y setters públicos. La lógica de negocio fundamental (cálculo de totales, transiciones de estado de orden, apertura y cierre de turno, validación de diferencias de arqueo y registro de gastos) reside dispersa en servicios de aplicación (`OrderServiceImpl`, `CashShiftService`), violando el encapsulamiento y el paradigma de Modelo de Dominio Rico.
- **Alcance Funcional y Técnico:**
  - Enriquecimiento de `Order`:
    - Encapsular colección de `order_items` (lista inmutable hacia afuera vía `Collections.unmodifiableList()`).
    - Métodos de negocio: `addItem(DishDto dish, int quantity, BigDecimal unitPrice)`, `removeItem(UUID itemId)`, `markAsPaid(String paymentMethod)`, `cancel(String reason)`.
    - Recálculo interno automático de subtotal, descuentos, impuestos y total ante cualquier mutación de ítems.
  - Enriquecimiento de `CashShift`:
    - Métodos de negocio: `closeShift(BigDecimal actualCash, String notes)`, `registerExpense(BigDecimal amount)`, `reconcileDifference()`.
    - Validación de invariantes: Un turno cerrado no puede volver a cerrarse ni registrar gastos; la fecha de cierre debe ser posterior a la de apertura.
  - Refactorizar servicios para que actúen estrictamente como orquestadores de infraestructura (cargar agregado de base de datos, invocar método de dominio y persistir).
- **Criterios de Aceptación:**
  - No es posible mutar el estado o totales de `Order` o `CashShift` omitiendo sus métodos de dominio e invariantes.
  - Las pruebas unitarias de las entidades `Order` y `CashShift` validan el 100% de las reglas de negocio sin requerir dependencias de Spring ni base de datos.
  - Se eliminan setters públicos indiscriminados en las entidades transaccionales.

---

### PB-014: Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex (Map<InventoryDeductionMode, DeductionStrategy>)
- **Categoría:** Patrones de Diseño / Módulo Inventory
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** En `KardexService`, las estrategias de deducción de inventario (`ManualIsolatedStrategy`, `AutomaticPermissiveStrategy`, `AutomaticStrictStrategy`) están inyectadas individualmente en el constructor como clases concretas. El servicio debe evaluar manualmente cuál ejecutar según el enum de configuración, violando el Principio Abierto/Cerrado (OCP) e impidiendo la adición de nuevas estrategias sin modificar `KardexService`.
- **Alcance Funcional y Técnico:**
  - Modificar la interfaz `DeductionStrategy` para incluir el método: `InventoryDeductionMode getMode();`.
  - Configurar inyección dinámica en Spring inyectando `List<DeductionStrategy>` y construyendo en el constructor un `Map<InventoryDeductionMode, DeductionStrategy>` indexado por el modo, o mediante inyección directa de mapa de Spring.
  - Simplificar `KardexService`: recuperar la estrategia correspondiente mediante `strategies.get(mode)` y ejecutar `strategy.deduct(...)`.
  - Manejo de error estructurado si se solicita un modo que no cuenta con estrategia registrada.
- **Criterios de Aceptación:**
  - `KardexService` no contiene inyecciones de clases concretas de estrategia ni estructuras `if-else` / `switch` dependientes del tipo de estrategia.
  - Agregar una nueva estrategia de deducción solo requiere implementar `DeductionStrategy` y anotar con `@Component`, sin modificar una sola línea de `KardexService`.
  - Pruebas unitarias de `KardexService` verifican la correcta delegación a cada estrategia según el modo provisto.

---

### PB-015: Extracción de Generador de Secuencia Diaria de Comandas (SRP en POS)
- **Categoría:** Clean Code & Concurrencia / Módulo POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Contexto:** En `OrderServiceImpl`, el método privado `generateDailyTicketNumber()` asume la responsabilidad de consultar husos horarios de sucursal, calcular el rango de inicio y fin del día, contar pedidos existentes en la base de datos y generar la cadena formateada `T-1001`. Esto vulnera el Principio de Responsabilidad Única (SRP) y en escenarios de alta concurrencia puede producir duplicidad de números de ticket ante pedidos simultáneos.
- **Alcance Funcional y Técnico:**
  - Extraer la lógica a un servicio dedicado `DailyTicketNumberGenerator` (o `TicketSequenceService`) en `modules.pos.internal`.
  - Diseñar una estrategia atómica o segura ante concurrencia (evaluar secuencias PostgreSQL dinámicas por sucursal/día o bloqueo pesimista/atómico en registro de control diario).
  - Integrar el generador en `OrderServiceImpl` como dependencia inyectada simple.
  - Permitir parametrización del formato de ticket por tenant (ej. prefijo personalizable `T-`, `FAC-`, etc.).
- **Criterios de Aceptación:**
  - `OrderServiceImpl` delega la generación del correlativo al nuevo servicio sin conocer detalles de conteo o formateo.
  - Tests de concurrencia validan que 50 órdenes simultáneas en la misma sucursal obtienen números de ticket estrictamente secuenciales y únicos sin colisiones.
  - El reinicio diario del correlativo se ejecuta a medianoche según la zona horaria de la sucursal.

---

## 4. Prioridad P3 (Baja Prioridad / Futuro)

### PB-009: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Contexto:** Los mozos y administradores de salón se benefician de una vista visual espacial del restaurante en lugar de un listado plano de números de mesa.
- **Alcance Funcional y Técnico:**
  - Configuración y visualización gráfica del salón por zonas/mesas con indicación de estado en tiempo real (libre, ocupada, por cobrar).
  - Editor visual drag & drop para diagramar salones y terrazas.
- **Criterios de Aceptación:**
  - El estado de la mesa cambia reactivamente cuando se abre o cancela una orden asociada a la misma.

---

### PB-010: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Contexto:** Cocinas de alto volumen requieren reemplazar las comandas de papel por pantallas táctiles interactivas (Kitchen Display System).
- **Alcance Funcional y Técnico:**
  - Pantalla táctil para cocineros con cronómetros por plato/comanda y métricas de tiempos de preparación.
  - Notificaciones sonoras y alertas visuales por comandas demoradas.
- **Criterios de Aceptación:**
  - Los cocineros pueden cambiar el estado del plato (`EN_PREPARACION` -> `LISTO`) con un toque en la pantalla táctil.