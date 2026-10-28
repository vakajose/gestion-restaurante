---
title: "Product Backlog y Registro de Nuevas Funcionalidades"
category: "backlog"
tags:
  - backlog
  - requirements
  - user-stories
  - roadmap
last_updated: "2026-10-27"
status: "verified"
---

# Product Backlog y Registro de Nuevas Funcionalidades


Este documento centraliza el Product Backlog del sistema, ordenado estrictamente por **nivel de prioridad**. Aquí se registran las iniciativas, requerimientos, historias de usuario y decisiones arquitectónicas que surgen durante el desarrollo pero que **no se implementarán de forma inmediata**, sirviendo de repositorio para la planificación de futuros sprints. Incluye la sincronización integral con la [Retrospectiva Funcional del Sprint 1](file:///home/jvaca/proyectos/gestion-restaurante/docs/project/retrospectiva_negocio_sprint1.md).

---

## Matriz de Priorización

| Nivel | Clasificación | Criterio de Entrada |
| :--- | :--- | :--- |
| **P0** | **Crítica / Máxima** | Bloqueante estructural para el modelo de negocio, gobernanza SaaS, seguridad/RBAC o ciclo de vida inicial del inquilino (previo a rollout). |
| **P1** | **Alta** | Ampliación clave de la operación del cliente (multi-sucursal, catálogo avanzado, resiliencia arquitectónica y desacoplamiento). |
| **P2** | **Media** | Integraciones periféricas (hardware, tributaria, soporte multi-motor, refactors internos de dominio y polimorfismo). |
| **P3** | **Baja / Futuro** | Mejoras de experiencia operativa avanzada (KDS avanzado, plano interactivo de mesas). |

---

## Índice Rápido del Product Backlog

| ID | Título de la Iniciativa | Prioridad | Estado / Sprint | Relación Retrospectiva |
| :--- | :--- | :---: | :---: | :--- |
| **PB-001** | Onboarding del Cliente y Configuración Inicial de Empresa | P0 | En Backlog | Eje A (Tenancy) |
| **PB-002** | Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner) | P0 | En Backlog | Eje B (RBAC) |
| **PB-003** | Backoffice Independiente (Frontend Desacoplado + BFF Dedicado) | P0 | En Backlog | Eje A & B |
| **PB-017** | Gestión y Configuración de Empresa (Tenant) y Sucursales | P0 | Sprint 2 | UC-203 (Eje A) |
| **PB-018** | Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios | P0 | Sprint 2 | UC-202, UC-204 (Eje B) |
| **PB-019** | Login Calificado por Tenant y Selector Contextual de Sucursal | P0 | Sprint 2 | UC-201, UC-205 (Eje C) |
| **PB-022** | Corrección y Estabilización de Suite de Pruebas Frontend en CI | P0 | Resuelto (Hito 1) | Infraestructura CI |
| **PB-023** | Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD) | P0 | Resuelto (Hito 1) | Gobernanza SDD |
| **PB-004** | Transferencias de Insumos entre Sucursales (`StockTransfer`) | P1 | En Backlog | Inventario Multi-Sede |
| **PB-005** | Consolidación Financiera y Reportería Multi-Sucursal | P1 | En Backlog | Finanzas |
| **PB-011** | Desacoplamiento de Repositorios hacia APIs Públicas Modulares | P1 | Sprint 2 | Gobernanza Modulith |
| **PB-012** | Estandarización de Excepciones de Dominio (RFC 7807 ProblemDetail) | P1 | Sprint 2 | Resiliencia REST |
| **PB-016** | Capa de Repositorios Locales (DAL) para Dexie.js aislando `appDb` | P1 | Sprint 2 | DAL Frontend Dexie |
| **PB-020** | Motor de Promociones, Combos, Descuentos Directos y Cupones en POS | P1 (Fase 1) / P2 (Fase 2) | Sprint 2-3 | UC-207, UC-208, UC-209 (Eje E) |
| **PB-021** | Inicialización de Stock, Recepción de Compras y Mermas | P1 | Sprint 2 | UC-206 (Eje D) |
| **PB-024** | Panel de Analítica, Métricas de Negocio y Rentabilidad Neta | P1 | Sprint 3 (Pospuesto) | Sección 1.2 Retrospectiva |
| **PB-025** | Infraestructura de Despliegue, Contenerización Docker y Certificación Cloud | P1 | En Backlog (DevOps) | Rollout Nube & Certificación |
| **PB-006** | Desacoplamiento y Compatibilidad Multi-Motor SQL | P2 | En Backlog | Base de Datos |
| **PB-007** | Facturación Electrónica Oficial | P2 | En Backlog | Fiscal |
| **PB-008** | Módulo de Impresión Física ESC/POS (Comanderas de Red) | P2 | En Backlog | Hardware POS |
| **PB-013** | Evolución a Modelo de Dominio Rico en Order y CashShift | P2 | En Backlog | DDD Core |
| **PB-014** | Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex | P2 | En Backlog | Refactor Inventory |
| **PB-015** | Extracción de Generador de Secuencia Diaria de Comandas | P2 | En Backlog | SRP POS |
| **PB-009** | Mapa Visual Interactivo de Distribución de Mesas | P3 | En Backlog | UX Salón |
| **PB-010** | Panel KDS Táctil con Métricas de Despacho en Cocina | P3 | En Backlog | Cocina KDS |

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
- **Contexto:** Para que un restaurante opere comercialmente de forma autónoma en un entorno SaaS, el administrador debe poder gestionar la información legal, operativa y geográfica de su empresa y de todas sus sucursales (activación, horarios, zona horaria, moneda, políticas fiscales y datos de contacto). Actualmente la base de datos cuenta con tablas `tenants` y `branches` creadas por scripts SQL, pero no existen endpoints de administración ni interfaz visual para su gestión dinámica.
- **Alcance Funcional y Técnico:**
  - **Nivel SUPERADMIN (Operador de Plataforma SaaS):**
    - CRUD global de Tenants: Razón Social, identificador único alfanumérico (`slug`, ej. `don-pollo`, `pastas-luigi`), Identificación Fiscal/NIT/RUT, correo de contacto y cupo máximo de sucursales contratadas.
    - Gobernanza de suscripciones: activación, suspensión temporal o baja por mora que invalida accesos a nivel de inquilino.
    - Alta del primer usuario con rol `ADMIN_TENANT` para la empresa cliente.
  - **Nivel ADMIN_TENANT (Dueño / Administrador de Empresa):**
    - **Perfil de Empresa ("Mi Restaurante"):** Edición de razón social, nombre comercial, NIT, logo institucional (utilizado para impresión de comandas y tickets) y moneda de curso legal (símbolo monetario y cantidad de decimales).
    - **CRUD Completo de Sucursales:**
      - Alta y edición de sucursales: Nombre (ej. "Sucursal Miraflores", "Express Mall"), código interno único, dirección física y fiscal, teléfono de contacto.
      - **Configuración de Huso Horario IANA Obligatorio:** Asignación explícita de timezone ([ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_and_timezone_handling.md), ej. `'America/La_Paz'`, `'America/Lima'`) para el cómputo exacto de cierres de turno, ventas del día y promociones por hora.
      - **Interruptor de Estado Operativo:** Switch activo/inactivo (cierre temporal por refacción o clausura) que **bloquea inmediatamente la apertura de nuevos turnos de caja y el registro/cobro de pedidos** en POS mientras esté inactiva.
      - **Inicialización Automática:** Al crear una sucursal, el sistema debe registrar en segundo plano sus `branch_settings` por defecto (ej. modo de inventario `INVENTORY_DEDUCTION_MODE` heredado de la configuración del tenant, tolerancia de arqueo de caja y límites de caja chica).
  - **Aseguramiento de Aislamiento:** Filtro estricto mediante `TenantContextHolder` (prohibido que un tenant acceda o modifique sucursales de otro tenant).
  - **Frontend (TailwindAdmin v2.0):**
    - Vista "Configuración de Empresa": Formulario con datos fiscales, subida/gestión de logo y parámetros monetarios.
    - Vista "Gestión de Sucursales": Tarjetas y tabla de sucursales, badges visuales de estado operativo (verde *"Abierta/Activa"*, rojo *"Inactiva"*), formulario modal interactivo para creación y edición de locales con selector de zona horaria IANA.
- **Criterios de Aceptación:**
  - Un administrador de tenant solo puede listar, consultar y modificar las sucursales pertenecientes a su propio `tenant_id`. Intentar acceder a otra sucursal retorna HTTP 404/403.
  - La creación y actualización de sucursal exige una zona horaria IANA válida y un código interno único por tenant.
  - Al dar de alta una sucursal, se generan automáticamente sus `branch_settings` predeterminadas en base de datos.
  - La desactivación de una sucursal bloquea inmediatamente la apertura de nuevos turnos de caja y el registro de órdenes para esa sucursal en POS y API REST.
  - La interfaz de usuario refleja los cambios inmediatamente y proporciona feedback claro de guardado y errores de validación.

---

### PB-018: Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios por Tenant
- **Categoría:** Seguridad / Autenticación y Autorización
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog`
- **Contexto:** Se requiere una administración granular de los colaboradores del restaurante. Cada usuario debe tener roles predeterminados con alcances y restricciones inviolables de negocio. En la versión actual del frontend, todos los usuarios comparten la misma navegación en Sidebar y no existen `roleGuards`, lo que permitiría a cajeros o cocineros acceder a vistas financieras o administrativas sin autorización.
- **Alcance Funcional y Técnico:**
  - **Matriz de los 5 Roles y Restricciones Inviolables de Negocio:**
    - `SUPERADMIN`: Control global de plataforma multi-tenant. *Restricción:* Prohibido registrar ventas, abrir turnos de caja chica o alterar recetas.
    - `ADMIN_TENANT`: Dueño/Administrador de empresa; acceso total a sucursales, usuarios, catálogo maestro, finanzas y reportes de su tenant. *Restricción:* Prohibido operar en nombre de otra empresa cliente.
    - `BRANCH_MANAGER`: Gerente de sucursal(es) asignada(s); carga de stock inicial, recepción de compras, mermas, sobreescritura de precios locales (`branch_dishes`), aprobación de gastos y supervisión de turnos locales. *Restricción:* Prohibido crear nuevos platos en el catálogo maestro corporativo o ver datos de otras sucursales sin autorización.
    - `CASHIER`: Cajero; apertura/cierre de turnos de caja asignados, cobro de tickets en POS y solicitud de egresos menores. ***Restricción Inviolable:*** **Prohibido ver costos de insumos, fórmulas de recetas (BOM), catálogos de proveedores o utilidades del restaurante.**
    - `KITCHEN`: Personal de cocina / KDS; visualización de comandas en cola (`WAITING`, `IN_PREPARATION`) y despacho de platos (`READY`). ***Restricción Inviolable:*** **Prohibido acceso a montos de dinero, cobros, cierres de caja, costos o gestión de usuarios.**
  - **Backend:**
    - Entidades y tablas relacionales para asignación de usuarios a sucursales (`user_branches` vinculando `app_users` con `branches`).
    - Endpoints CRUD de usuarios (`/api/v1/users`) protegidos por `@PreAuthorize` permitiendo a administradores crear, editar, suspender y asignar sucursales a colaboradores.
    - **Desactivación de Cuentas (Baja Lógica):** Interruptor de habilitación/desactivación sin borrado físico (`is_active = false`), garantizando integridad referencial en auditorías, pedidos históricos y arqueos de caja.
    - Endpoints de perfil personal (`/api/v1/auth/me` GET/PUT) para actualizar datos personales (nombre, teléfono, correo) y cambio seguro de contraseña (validando contraseña actual con Argon2id).
  - **Frontend:**
    - **Navegación Condicional en Sidebar:**
      - Estructura tipada `NavItem { label: string; icon: string; route: string; allowedRoles: Role[]; }`.
      - Agrupación por categorías en Sidebar: *Operaciones Diarias* (POS), *Gestión de Sucursal* (Inventario, Finanzas), *Administración Corporativa* (Catálogo, Empresa/Sucursales, Usuarios).
      - Menú reactivo evaluado mediante `computed()` conectado al estado de `authService.currentUser()`.
    - **Route Guards Estrictos (`roleGuard`):** Protección de rutas con metadata `data: { roles: ['ADMIN_TENANT', 'BRANCH_MANAGER'] }`. Redirección automática a zona permitida (ej. `/pos`) con alerta visual ante accesos no autorizados.
    - **Gestión de Usuarios por `ADMIN_TENANT`:** Tabla de colaboradores con buscador, badges de rol y estado activo/inactivo, modal de creación con nombres, apellidos, `username`, `email` opcional, contraseña inicial, rol y selector multiselección de sucursales autorizadas.
    - **Vista "Mi Perfil" y Humanización de Identidad:**
      - Diccionario de presentación para roles (no mostrar códigos crudos como `ADMIN_TENANT` o `CASHIER`):
        - `SUPERADMIN` $\rightarrow$ *"Administrador Global"*
        - `ADMIN_TENANT` $\rightarrow$ *"Administrador de Empresa"*
        - `BRANCH_MANAGER` $\rightarrow$ *"Gerente de Sucursal"*
        - `CASHIER` $\rightarrow$ *"Cajero / Atención"*
        - `KITCHEN` $\rightarrow$ *"Personal de Cocina"*
      - Badges temáticos en TailwindAdmin: `.bg-lightprimary .text-primary`, `.bg-lightsecondary .text-secondary`, `.bg-lightsuccess .text-success`, `.bg-lightwarning .text-warning`.
      - Visualización de datos personales y sucursales asignadas.
      - Formulario para autogestión de cambio de contraseña (contraseña actual, nueva y confirmación) y switch para alternar temas (Claro / Oscuro).
- **Criterios de Aceptación:**
  - Un usuario con rol `CASHIER` solo visualiza en el Sidebar las opciones de POS y Turnos de Caja; intentar ingresar manualmente a `/finance`, `/inventory` o `/company` redirige con mensaje de acceso denegado.
  - El personal de cocina (`KITCHEN`) no puede ver montos de dinero, totales de comanda ni pantallas de cobro o costos.
  - La desactivación de una cuenta impide el login de forma inmediata sin alterar registros históricos en comandas o turnos.
  - La vista "Mi Perfil" permite actualizar nombre y contraseña pero prohíbe auto-escalar roles.
  - Al dar de alta un usuario, la contraseña se hashea con Argon2id y se valida la unicidad de `username` dentro del `tenant_id`.

---

### PB-019: Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa
- **Categoría:** Autenticación / UX Multi-Tenant
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog`
- **Contexto:** En la arquitectura multi-inquilino actual, la unicidad de usuario está restringida por empresa (`uq_user_tenant_username: UNIQUE (tenant_id, username)`). Sin embargo, el endpoint de login busca actualmente por username de forma no calificada a nivel global (`findByUsernameOrEmail`), lo que causa colisión sistémica (`NonUniqueResultException` arrojando HTTP 500 o autenticación en el restaurante equivocado) si dos clientes distintos poseen un colaborador con el mismo nombre (ej. `cajero1` o `admin`). Adicionalmente, el Topbar actual contiene un nombre de sucursal estático y quemado en código ("Central La Paz"), imposibilitando la conmutación entre locales para usuarios con múltiples sucursales asignadas.
- **Alcance Funcional y Técnico:**
  - **Login Calificado por Identificador de Empresa (Backend & Frontend):**
    - Formulario de Login adaptativo que solicita obligatoriamente 3 campos:
      1. **Empresa / Código:** Slug corporativo único (ej. `don-pollo`, `pastas-luigi`) o Identificación Fiscal/NIT.
      2. **Usuario:** Nombre de usuario o correo electrónico.
      3. **Contraseña:** Clave secreta hasheada con Argon2id.
    - Soporte opcional de resolución automática de subdominio (ej. si se accede desde `donpollo.restaurante.com`, se autocompleta e infiere el slug sin requerir el campo).
    - El backend valida primero la existencia y estado activo del tenant mediante su `slug`; posteriormente busca las credenciales del usuario acotadas estrictamente a dicho `tenant_id`.
    - Token JWT enriquecido con claims: `tenantId`, `userId`, `role`, `username` y lista de `branchIds` autorizadas.
  - **Flujo Post-Login y Selector Contextual en Topbar (TailwindAdmin v2.0):**
    - **Usuarios Monosucursal (`CASHIER`, `KITCHEN`, `BRANCH_MANAGER` local):**
      - Si el usuario cuenta con exactamente 1 sucursal asignada, el sistema la establece automáticamente en el contexto global (`activeBranch`).
      - Redirección directa e inmediata a su área de trabajo autorizada (`/pos` para cajero, `/kitchen` para cocina).
      - En el Topbar se renderiza una insignia fija con el nombre de su local (sin menú desplegable ni flecha interactiva).
    - **Usuarios Multisucursal y `ADMIN_TENANT`:**
      - Si el usuario administra 2 o más sucursales, el Topbar renderiza un **Selector Desplegable de Sucursal (Dropdown)** estilizado en TailwindAdmin v2.0 en el extremo superior izquierdo.
      - Componentes del Dropdown: avatar con iniciales de la sucursal, badge indicador de estado operativo (verde *"Abierta"*, rojo *"Inactiva/Cerrada"*), flecha chevron animada y listado interactivo de sucursales activas del tenant.
      - Al seleccionar una nueva sucursal:
        - Se actualiza la `Signal` reactiva `activeBranch` en `BranchContextService`.
        - Se propaga la cabecera HTTP `X-Branch-Id` en todas las peticiones posteriores mediante el interceptor de Angular.
        - Microanimación sutil de carga (skeleton loading) mientras se refrescan los catálogos, stocks de inventario, comandas y turnos de caja sin recargar la página completa ni forzar re-login.
- **Criterios de Aceptación:**
  - Dos usuarios con el mismo username `cajero1` pertenecientes a empresas distintas pueden autenticarse concurrentemente sin colisiones ni errores 500.
  - Intentar iniciar sesión con un slug de empresa inexistente o inactiva retorna HTTP 401 estructurado (*"Empresa no encontrada o inactiva"*).
  - Los usuarios con una única sucursal asignada nunca son obligados a pasar por un paso manual de selección de sucursal.
  - El selector del Topbar solo lista las sucursales a las que el usuario tiene acceso explícito dentro de su tenant.
  - Al conmutar de sucursal en el selector, todas las llamadas subsiguientes de API transmiten la cabecera `X-Branch-Id` correspondiente.
  - El backend valida estrictamente que la sucursal enviada en `X-Branch-Id` pertenezca al `tenant_id` del token JWT y que el usuario tenga permisos en ella (HTTP 403 en caso contrario).

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

### PB-020: Motor de Promociones, Combos, Descuentos Directos y Cupones en POS
- **Categoría:** POS / Ventas y Motor Comercial
- **Prioridad:** `P1 - Alta` *(Fase 1: Descuentos y Cupones P1; Fase 2: Combos y Happy Hour P2)*
- **Estado:** `En Backlog`
- **Contexto:** La operación real de un restaurante gastronómico no se limita a vender platos individuales a precio de lista. Estrategias comerciales esenciales como combos familiares, descuentos de cortesía o fidelización, cupones publicitarios y promociones automáticas por horario ("Happy Hour") son los principales motores de volumen y facturación. En la Iteración 1, el POS solo permite agregar platos simples a su precio base.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Descuentos Directos y Cupones en POS (Prioridad P1 - Sprint 2 / UC-207):**
    - **Tipos de Descuento en Comanda:**
      - **Porcentual (%):** Reducción de un porcentaje sobre el subtotal de la comanda (ej. 10% por convenio corporativo).
      - **Monto Fijo ($ / Bs.):** Rebaja de un monto monetario específico sobre el total (ej. Bs. 15 de descuento).
    - **Gobernanza y Matriz de Autorizaciones:**
      - Límite máximo de descuento autónomo configurable para el rol `CASHIER` (ej. hasta 5%).
      - Si el descuento supera dicho umbral o se trata de una cortesía al 100%, la interfaz exige obligatoriamente credenciales o PIN de autorización del `BRANCH_MANAGER`.
      - Registro obligatorio del motivo del descuento (`reason`: Cortesía, Empleado, Convenio, Queja de Calidad) y del usuario autorizador (`authorized_by_user_id`).
    - **Cupones de Descuento (`coupons`):**
      - Creación de códigos alfanuméricos promocionales (ej. `BIENVENIDO2026`).
      - Restricciones: Límite global de canjes, fecha de caducidad, consumo mínimo en ticket y reglas de exclusión de platos que ya tengan precio promocional.
    - **Experiencia de Usuario en Carrito POS (TailwindAdmin v2.0):**
      - Sección colapsable interactiva al pie del carrito: `[ % Aplicar Descuento / Cupón ]`.
      - Desglose transparente en el resumen de cobro:
        - `Subtotal: Bs. 120.00`
        - `Descuento (Cupón 10%): - Bs. 12.00` (en color `.text-error` o `.text-primary`)
        - `Total a Pagar: Bs. 108.00` (en tipografía destacada `.text-link`)
      - Modal seguro para solicitud de PIN supervisor cuando el descuento excede el límite del cajero.
  - **Fase 2: Combos Compuestos y Promociones Temporales (Prioridad P2 - Sprint 2-3 / UC-208, UC-209):**
    - **Gestión de Combos (Packs gastronómicos):**
      - Entidad en Catálogo con `dish_type = 'COMBO'` y composición en tabla `combo_items` (vincula múltiples sub-platos o bebidas a un precio cerrado).
      - **Deducción Recursiva Transparente en Kardex:** Al cobrarse un combo en POS mediante `OrderPaidEvent`, el listener de inventario resuelve recursivamente los ingredientes de cada sub-plato contenido en el combo y descuenta las existencias de todos los insumos en una única transacción atómica.
    - **Reglas de Promociones Temporales Automatizadas (Happy Hour):**
      - Parametrización por calendario (fecha inicio y fin) y ventana horaria por días de la semana (ej. Martes y Jueves de 17:00 a 20:00).
      - **Cálculo Local de Hora Sucursal (ADR-0006):** La comprobación horaria se evalúa obligatoriamente contra el huso horario local de la sucursal (`branches.timezone`), aplicando automáticamente el precio de oferta o el descuento promocional en POS al abrir comanda dentro de la ventana activa.
- **Criterios de Aceptación:**
  - El carrito del POS y el ticket final reflejan con exactitud los precios base, descuentos aplicados, código de cupón y subtotal neto liquidado.
  - Los descuentos superiores al límite permitido para cajeros son bloqueados hasta ingresar el PIN de un `BRANCH_MANAGER`.
  - La deducción de inventario ante la venta de un combo descuenta la totalidad de los insumos de cada sub-plato sin dejar stock huérfano.
  - Las promociones con ventana horaria se activan y desactivan automáticamente según el huso horario IANA de la sucursal.
  - Las reglas de descuentos y cupones se persisten localmente en Dexie.js para operar bajo contingencia offline.

---

### PB-021: Flujo Coherente de Inicialización de Stock, Recepción de Compras y Conciliación de Mermas
- **Categoría:** Inventario / Kardex & Abastecimiento
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Contexto:** En la Iteración 1 se evidenció una confusión operativa entre registrar un insumo en el Catálogo Maestro y asignarle stock físico en una sucursal: el insumo en catálogo es un concepto abstracto y corporativo ("Carne de Res", "Aceite Vegetal") que no posee existencias físicas en sí mismo. Para que una sucursal opere, se requiere un flujo formal que delimite con precisión: (1) Carga de Apertura (`INITIAL_STOCK`), (2) Recepción de Compras de Proveedores (`PURCHASE`), y (3) Ajustes físicos por merma o recuento de auditoría.
- **Alcance Funcional y Técnico:**
  - **Ciclo de Vida Estandarizado en 5 Etapas:**
    1. **Catálogo Maestro Central:** El `ADMIN_TENANT` define el insumo maestro en `ingredients` (nombre, unidad de medida `KG`, `LT`, `UNIT`, stock mínimo). No altera ningún Kardex ni genera existencias físicas.
    2. **Habilitación en Sucursal con Stock Inicial (`INITIAL_STOCK` / UC-206):**
      - Al abrir una sucursal o comenzar a emplear un insumo en ella, el encargado registra el saldo base y el costo unitario de apertura.
      - Genera registro en `branch_stock` (`current_quantity = Q`, `average_unit_cost = C`) y movimiento Kardex inmutable: `INITIAL_INVENTORY`.
      - **Regla de integridad inviolable:** Solo se permite registrar `INITIAL_STOCK` una única vez por insumo y sucursal. Si el insumo ya cuenta con movimientos previos en el Kardex local, la operación se rechaza estrictamente.
    3. **Abastecimiento Recurrente por Compras (`PURCHASE`):**
      - Módulo formal de registro de compras a proveedores (`purchases` y `purchase_items`): proveedor, número de factura/guía, fecha de compra, insumos recibidos, cantidades y costos unitarios de compra.
      - Incrementa el stock disponible y recalcula automáticamente el costo unitario promedio ponderado móvil:
        $$\bar{C}_{\text{nuevo}} = \frac{(Q_{\text{actual}} \times \bar{C}_{\text{actual}}) + (Q_{\text{comprada}} \times C_{\text{compra}})}{Q_{\text{actual}} + Q_{\text{comprada}}}$$
      - Genera movimiento Kardex inmutable: `PURCHASE_IN`.
    4. **Consumo por Venta (`SALE_OUT`):**
      - Al cobrarse un ticket en POS, el evento `OrderPaidEvent` desencadena la deducción automática por receta según la estrategia (`AUTOMATIC_PERMISSIVE` o `AUTOMATIC_STRICT`), generando movimientos `SALE_OUT`.
    5. **Conciliación Física y Mermas:**
      - **Merma Operativa (`WASTE_ADJUSTMENT`):** Salida justificada por descomposición, rotura, vencimiento o mala manipulación de insumos. Disminuye el stock disponible y calcula el costo total de la pérdida monetaria para finanzas.
      - **Conteo Físico de Auditoría (`PHYSICAL_COUNT_ADJUSTMENT`):** Ajuste donde el recuento real reemplaza al saldo teórico, asentando la diferencia (positiva o negativa) con notas obligatorias del auditor.
  - **Frontend (TailwindAdmin v2.0):**
    - **Modal de Stock Inicial Guiado:** Ventana modal con selector del insumo maestro abstracto, campos claros de cantidad y costo unitario de adquisición, y alerta informativa en tono `lightprimary`: *"Esta acción registra la existencia inicial para esta sucursal y apertura su libro mayor en Kardex."*
    - **Pantalla de Recepción de Compras:** Formulario con selector de proveedor, subida/ingreso de número de factura, tabla dinámica de insumos y costos totales.
    - **Pantalla de Ajustes y Mermas:** Formulario de registro de mermas con selector de motivos obligatorios y recuento físico con cálculo de diferencias.
    - Historial de movimientos de Kardex con filtros por tipo (`INITIAL_INVENTORY`, `PURCHASE_IN`, `SALE_OUT`, `WASTE_ADJUSTMENT`, `PHYSICAL_COUNT_ADJUSTMENT`).
- **Criterios de Aceptación:**
  - El sistema rechaza un registro de `INITIAL_STOCK` si el insumo ya registra movimientos previos en el Kardex de esa sucursal (HTTP 409 / 422).
  - Toda compra recibida actualiza el saldo físico en `branch_stock`, recalcula el costo promedio ponderado de acuerdo a la fórmula y asienta el movimiento en Kardex de forma atómica.
  - Los ajustes de merma exigen obligatoriamente motivo descriptivo y usuario responsable.
  - La interfaz de usuario guía al encargado con alertas claras en TailwindAdmin evitando confusiones entre catálogo e inventario físico.

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

### PB-023: Bóveda de Arquitectura Viva y Especificaciones en Mermaid (Specification-Driven Development - Hito 1)
- **Categoría:** Arquitectura & Gobernanza / Specification-Driven Development (SDD)
- **Prioridad:** `P0 - Crítica (Fundacional de Proyecto)`
- **Estado:** `Implementado / Resuelto`

- **Contexto:** Al concluir el Hito 1 (Iteración 1), el sistema cuenta con módulos operativos maduros (Seguridad/Tenancy, Catálogo con Overrides, POS Offline-First con Dexie.js, Kardex asíncrono con Strategy Pattern y Finanzas con Turnos/Gastos). Para sustentar la evolución del sistema y garantizar gobernanza técnica sin degradación arquitectónica, se requiere formalizar la Bóveda de Arquitectura en Obsidian usando Mermaid bajo el paradigma de Specification-Driven Development (SDD).
- **Alcance Funcional y Técnico:**
  - Instituir la Skill `.agents/skills/sdd-architecture-diagramming/SKILL.md` y la Regla 7 en `.agents/rules/architectural-governance.md` y `AGENTS.md`.
  - Crear la Bóveda de Arquitectura compatible con Obsidian en `docs/project/architecture/` con frontmatter YAML y enlaces bidireccionales.
  - Generar 16 diagramas Mermaid esenciales distribuidos en 4 niveles:
    1. C4 Nivel 2 (Contenedores) y Nivel 3 (Componentes Spring Modulith y Frontend Angular Zoneless).
    2. Modelo de Dominio Conceptual, ERD Físico PostgreSQL (18 tablas) y Esquema Local Dexie.js.
    3. Catálogo de Casos de Uso Macro y por Módulo (`catalog`, `pos`, `inventory`, `finance`).
    4. Flujos Dinámicos, Secuencias (POS $\rightarrow$ Kardex/Finanzas, Offline-First Sync), Estados (`Order`, `CashShift`/`Expense`) y Actividad (Strategy Pattern).
  - Actualizar `README.md`, SDD y ADRs para sincronizar referencias cruzadas.
- **Criterios de Aceptación:**
  - 100% de los diagramas renderizan correctamente en Mermaid y Obsidian sin errores sintácticos.
  - Todos los documentos de arquitectura contienen metadatos YAML válidos y enlaces navegables.
  - Protocolo SDD activo: ninguna modificación futura se da por terminada sin sincronizar sus diagramas.

---

### PB-024: Panel de Analítica, Métricas de Negocio y Dashboard de Rentabilidad Neta (Planificado para Sprint 3)
- **Categoría:** Business Intelligence / Analítica Financiera
- **Prioridad:** `P1 - Alta` *(Planificado formalmente para Sprint 3)*
- **Estado:** `Pospuesto Estratégicamente (Dependencia de Sprint 2)`
- **Contexto y Justificación del Descarte en Sprint 2:**
  En la Retrospectiva Funcional del Sprint 1 ([docs/project/retrospectiva_negocio_sprint1.md](file:///home/jvaca/proyectos/gestion-restaurante/docs/project/retrospectiva_negocio_sprint1.md)), se formalizó la decisión técnica y comercial de **descartar el desarrollo de dashboards analíticos para el Sprint 2 y programarlos para el Sprint 3**. Las razones son contundentes:
  1. *Principio de Calidad de Datos (Garbage In, Garbage Out):* La utilidad neta real exige conocer el costo real de insumos en Kardex mediante la consolidación de `INITIAL_STOCK`, compras a proveedores (`purchases`) y conciliación de mermas (`WASTE_ADJUSTMENT`). Sin estos datos, las métricas financieras serían ficticias.
  2. *Confidencialidad y Fuga de Información por Falta de RBAC:* Sin aislamiento de rutas y menú por roles, un panel analítico expondría márgenes y utilidades a cajeros y cocineros.
  3. *Omisión de Descuentos y Promociones en los Ingresos Reales:* Sin motor de promociones y descuentos en POS, los reportes de ventas no reflejan la realidad comercial de las cajas.
  4. *Priorización de la Gobernanza Operativa:* Es prioritario que los restaurantes puedan crear sucursales, dar de alta usuarios y operar antes de visualizar reportes avanzados.
- **Alcance Funcional y Técnico (Sprint 3):**
  - **Ecuación de Rentabilidad Neta Consolidada:**
    $$\text{Utilidad Neta Real} = \text{Ventas Cobradas (POS)} - \text{Costo Real de Insumos Consumidos (Kardex)} - \text{Gastos Operativos Aprobados (Caja Chica y Fijos)}$$
  - **KPIs y Métricas Ejecutivas:**
    - Utilidad neta del día, semana, mes y acumulada en rango de fechas configurable.
    - Ranking de platos más vendidos (volumen de tickets vs margen de contribución económica).
    - Ticket promedio de comanda y volumen de ventas desglosado por método de pago (Efectivo, QR, Tarjeta).
    - Horas pico de facturación por sucursal.
    - Estimación de punto de equilibrio operativo.
  - **Filtros Dinámicos Multi-Sucursal:**
    - Selector para filtrar por sucursal individual o visión agregada de toda la empresa (respetando los husos horarios locales de cada sucursal según [ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_and_timezone_handling.md)).
  - **Aislamiento Estricto por RBAC:**
    - Acceso reservado exclusivamente para roles `ADMIN_TENANT` (toda la empresa) y `BRANCH_MANAGER` (únicamente su propia sucursal). Oculto para `CASHIER` y `KITCHEN`.
  - **Frontend (TailwindAdmin v2.0):**
    - Tarjetas ejecutivas con KPIs destacados, gráficos interactivos de ventas vs costos y tablas de platos estrella.
- **Criterios de Aceptación:**
  - El dashboard calcula la utilidad neta integrando ventas cobradas en POS, deducciones valorizadas de recetas en Kardex y egresos aprobados en finanzas.
  - El acceso a la vista analítica está bloqueado y oculto para roles `CASHIER` y `KITCHEN`.
  - Las ventas y fechas se computan convirtiendo los timestamps a la zona horaria IANA de la sucursal seleccionada.


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

---

### PB-025: Infraestructura de Despliegue, Contenerización Docker y Certificación Cloud
- **Categoría:** DevOps & Rollout Cloud / Infraestructura
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (DevOps)`
- **Contexto:** Habilitar el empaquetado del software como monolito contenerizado con Docker Compose para despliegue en la nube (VPS / Oracle Cloud Always Free), soportando de 1 a 10 inquilinos iniciales concurrentes con resiliencia offline-first y alta eficiencia de recursos. Incluye un ciclo de certificación de 3 etapas: 1) Simulación en entorno local con Docker Compose, 2) Despliegue en Preproducción (Staging en la nube), y 3) Despliegue en Producción con backups y SSL automatizado.
- **Alcance Funcional y Técnico:**
  - **Multi-Stage Dockerfiles:** Generación de imágenes optimizadas de backend (Maven build + JRE 25 Eclipse Temurin Alpine) y frontend (Node build + Nginx Alpine con proxy inverso para `/api/*` y rutas SPA).
  - **Twelve-Factor App & Actuator:** Parametrización estricta de variables de entorno (`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `SPRING_DATASOURCE_*`), activación de perfiles Spring (`prod`) y monitoreo de probes de salud con Spring Boot Actuator (`/actuator/health`).
  - **Ambiente de Certificación Local:** Orquestación con `docker-compose.staging.yml` reproduciendo el stack idéntico a producción y ejecución de regresión Playwright E2E sobre los contenedores locales.
  - **Estateless Storage de Evidencias:** Abstracción de guardado de comprobantes y recibos de egresos (`evidence_url`) en Object Storage compatible con S3 (ej. Cloudflare R2 u Oracle Object Storage) en lugar del sistema de archivos efímero del contenedor.
  - **Rollout Cloud & Hardening:** Aprovisionamiento de VPS / OCI, configuración de firewall (solo puertos 22 SSH Key, 80 HTTP, 443 HTTPS), certificados Let's Encrypt con renovación automática y aislamiento de red donde PostgreSQL no expone puertos hacia internet.
  - **Pipeline CI/CD con GitHub Actions:** Automatización del flujo de entrega continua mediante `.github/workflows/deploy.yml` utilizando GitHub Secrets, compilación automatizada y despliegue directo por SSH a la VM de Oracle Cloud con recarga de servicios sin intervención manual.
  - **Disaster Recovery Automatizado:** Cron diario en el servidor con volcado `pg_dump` comprimido, cifrado y transferido a un bucket de almacenamiento externo con retención histórica.
- **Criterios de Aceptación:**
  - El stack completo levanta de forma determinista mediante Docker Compose.
  - El pipeline de GitHub Actions compila y actualiza los contenedores en la nube automáticamente tras aprobar cambios en la rama de despliegue.
  - El frontend sirve la SPA en HTTPS sin errores de CORS al comunicarse con el backend a través del proxy reverso interno.
  - La suite de 25 pruebas E2E automatizadas pasa al 100% apuntando contra el entorno contenerizado.
  - La base de datos queda aislada en la red privada de Docker sin acceso público directo.
  - Se valida el proceso de restauración de backups (`pg_restore`) a partir de un volcado automatizado.
