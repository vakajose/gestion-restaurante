---
title: "Diagrama C4 Nivel 2: Topología de Contenedores"
category: "architecture"
level: "C4-Containers"
module: "global"
tags:
  - architecture
  - c4-model
  - containers
  - mermaid
  - sdd
  - spring-boot
  - angular
  - postgresql
  - dexie
last_updated: "2026-10-25"
status: "verified"
---

# 📦 Diagrama C4 Nivel 2: Topología de Contenedores

Este documento especifica la **Arquitectura C4 Nivel 2 (Contenedores)** del Sistema Integral de Gestión para Restaurante (POS & ERP). Modela los límites de ejecución de software de alto nivel, los entornos de ejecución, los almacenes de datos y los protocolos de comunicación que interconectan la solución.

---

## 1. Diagrama C4 de Contenedores

```mermaid
graph TD
    subgraph Actores["👥 Actores del Sistema"]
        U_POS["Cajero / Mozo<br/>(CASHIER / WAITER)"]
        U_MGR["Administrador / Gerente<br/>(BRANCH_MANAGER / ADMIN_TENANT)"]
        U_ADM["Superadministrador de Plataforma<br/>(SUPERADMIN)"]
    end

    subgraph Browser["🌐 Navegador Web del Cliente (Client Browser Environment)"]
        subgraph C_SPA["Contenedor: Frontend Single Page Application (Angular 22)"]
            UI_View["Componentes UI Standalone & Layout<br/>(TailwindAdmin v2.0, Sidebar 270px, Topbar)"]
            UI_State["Capa de Estado Reactivo<br/>(Angular Signals & Computed Values)"]
            UI_Core["Servicios Core & HTTP Interceptor<br/>(Auth, Pos, Catalog, Inventory, Finance)"]
        end

        subgraph C_DEXIE["Contenedor: Almacenamiento Local IndexedDB (Dexie.js)"]
            DAL_DB["RestaurantPosDb (appDb)<br/>- cachedCategories (id, name, sortOrder)<br/>- cachedProducts (id, categoryId, code, price)<br/>- pendingOrders (localId, clientTxId, status)<br/>- activeShift (id, branchId, status)"]
        end
    end

    subgraph C_BACKEND["⚙️ Contenedor: Servidor Backend API (Spring Boot 4.1.x / Java 25)"]
        SEC_FILTER["Filtro de Seguridad & Contexto Multi-Tenant<br/>(JwtAuthenticationFilter + TenantContextHolder)"]
        
        subgraph MOD_CORE["Núcleo Modular (Spring Modulith Core)"]
            MOD_SEC["core.security (AuthService, JwtService)"]
            MOD_CFG["core.config (TenantConfigService, BranchConfigService)"]
            MOD_COM["core.common (BaseTenantEntity, BaseBranchEntity)"]
        end

        subgraph MOD_BIZ["Módulos de Negocio (Spring Modulith Business)"]
            MOD_CAT["modules.catalog (CatalogPublicApi, Platos, Recetas)"]
            MOD_POS["modules.pos (PosPublicApi, Checkout, Reconciliación)"]
            MOD_INV["modules.inventory (InventoryPublicApi, Kardex, Stock)"]
            MOD_FIN["modules.finance (FinancePublicApi, Arqueo, Gastos)"]
        end

        EVT_BUS["Bus de Eventos Transaccional Spring Modulith<br/>(Transactional Event Publication Registry)"]
        LOOM["Runtime Concurrente Java 25<br/>(Project Loom Virtual Threads)"]
    end

    subgraph C_DATABASE["🗄️ Contenedor: Base de Datos Relacional (PostgreSQL 16+)"]
        DB_SCHEMA["Esquema Relacional Multi-Tenant<br/>(18 Tablas: tenants, branches, orders, kardex...)<br/>Aislamiento por tenant_id & branch_id"]
        DB_EVENTS["Registro Transaccional de Eventos (Outbox)<br/>- event_publication<br/>- event_publication_archive"]
    end

    %% Interacciones de Usuarios
    U_POS -->|"Opera POS, Comandas y Turno [HTTPS]"| UI_View
    U_MGR -->|"Gestiona Catálogo, Recetas, Stock y Gastos [HTTPS]"| UI_View
    U_ADM -->|"Configura Tenants y Sucursales [HTTPS]"| UI_View

    %% Comunicación interna en Navegador
    UI_View --> UI_State
    UI_State --> UI_Core
    UI_Core <-->|"Lectura/Escritura Local Offline-First<br/>[IndexedDB API / Asíncrono W3C]"| DAL_DB

    %% Comunicación Cliente -> Backend
    UI_Core -->|"REST API sobre HTTPS / JSON<br/>Encabezado: Bearer JWT<br/>Encabezado: X-Client-Transaction-Id<br/>Respuestas: ProblemDetail (RFC 7807)"| SEC_FILTER

    %% Enrutamiento Backend
    SEC_FILTER --> MOD_SEC
    SEC_FILTER --> MOD_CFG
    SEC_FILTER --> MOD_CAT
    SEC_FILTER --> MOD_POS
    SEC_FILTER --> MOD_INV
    SEC_FILTER --> MOD_FIN

    %% Eventos Backend
    MOD_POS -->|"Publica OrderPaidEvent"| EVT_BUS
    EVT_BUS -->|"Consumo Asíncrono (@ApplicationModuleListener)"| MOD_INV
    EVT_BUS -->|"Consumo Asíncrono (@ApplicationModuleListener)"| MOD_FIN

    %% Backend a Base de Datos
    MOD_CORE <-->|"Persistencia & Consultas Transaccionales<br/>[JDBC / HikariCP Pool / Flyway]"| DB_SCHEMA
    MOD_BIZ <-->|"Persistencia & Consultas Transaccionales<br/>[JDBC / HikariCP Pool / Flyway]"| DB_SCHEMA
    EVT_BUS <-->|"Registro Atómico de Publicación / Completado<br/>[JDBC / event_publication]"| DB_EVENTS
```

---

## 2. Catálogo de Contenedores y Responsabilidades

| Contenedor | Tecnología Principal | Entorno de Ejecución | Responsabilidad Primaria | Estrategia de Aislamiento / Multi-Tenancy | Almacenamiento / Persistencia |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Frontend SPA** | Angular 22, Tailwind CSS (TailwindAdmin v2.0), TypeScript | Navegador Web (Chrome, Edge, Firefox, Safari) | Interfaz de usuario interactiva y reactiva; gestión de sesiones; procesamiento de ventas táctiles en POS; arqueos de caja; cálculo reactivo de subtotales; operación en modo offline. | Contexto de sesión obtenido del JWT (`tenantId`, `branchId`, `role`) almacenado en memoria reactiva (`AuthService.currentUser`). | Memoria de la aplicación y llamadas a la capa DAL local. |
| **Almacenamiento Local** | IndexedDB API, Dexie.js v4+ (`RestaurantPosDb`) | Sandbox de Almacenamiento del Navegador (IndexedDB) | Persistencia local offline-first del catálogo descargado, turno activo de la estación y cola transaccional de ventas pendientes de sincronización. | Aislado por origen de dominio web; las transacciones contienen `branchId` y `clientTransactionId` asignados. | IndexedDB (`RestaurantPosDb` con `cachedCategories`, `cachedProducts`, `pendingOrders`, `activeShift`). |
| **Servidor Backend API** | Spring Boot 4.1.x, Spring Modulith, Spring Security, Java 25 LTS (Amazon Corretto) | Máquina Virtual Java (JVM) con Project Loom Virtual Threads | Orquestación del dominio de restaurante; validación de reglas de negocio; autenticación Argon2id y emisión de JWT; despacho de eventos de integración intermodular; idempotencia de mutaciones. | `TenantContextHolder` (propagación segura en `ThreadLocal` compatible con Virtual Threads) extraído por `JwtAuthenticationFilter`. | Sin estado (Stateless API). Todo estado transaccional reside en PostgreSQL. |
| **Base de Datos Relacional** | PostgreSQL 16+ con Flyway Migrations | Servidor de Base de Datos / Contenedor PostgreSQL | Almacenamiento persistente ACID de todo el modelo de datos empresarial (18 tablas transaccionales), bitácora de auditoría y almacén de eventos de Spring Modulith. | Multi-tenancy por discriminador de columnas (`tenant_id`, `branch_id`) e índices compuestos denormalizados en todas las tablas transaccionales. | Disco persistente PostgreSQL (`restaurant_db`, tablas de negocio y `event_publication`). |

---

## 3. Matriz de Protocolos e Intercambio de Datos

```mermaid
flowchart LR
    Browser["Navegador Cliente<br/>(Angular 22)"]
    Dexie["IndexedDB Local<br/>(Dexie.js)"]
    Backend["Backend Spring Boot<br/>(Java 25)"]
    Postgres["PostgreSQL 16+<br/>(Relacional)"]

    Browser <-->|"1. W3C IndexedDB API (Async)"| Dexie
    Browser -->|"2. HTTPS / REST (JSON + Headers)"| Backend
    Backend <-->|"3. PostgreSQL Wire Protocol (JDBC / HikariCP)"| Postgres
```

### Detalle de Protocolos y Encabezados

1. **Cliente $\leftrightarrow$ IndexedDB (`RestaurantPosDb`):**
   - **Protocolo:** W3C IndexedDB API asíncrona mediante el wrapper Dexie.js.
   - **Garantía:** Transacciones ACID locales a nivel de navegador. Permite a los cajeros seguir facturando aunque no haya conexión a Internet (`isOnline = false`).
   - **Objetos Persistidos:** Entidades tipadas (`CachedCategory`, `CachedProduct`, `PendingOrderEntity`, `ActiveShiftEntity`).

2. **Cliente $\rightarrow$ Backend API:**
   - **Protocolo:** HTTPS (TLS 1.3) / RESTful API con formato de carga útil JSON (codificación UTF-8).
   - **Autenticación:** Cabecera `Authorization: Bearer <jwt_token>` validada de forma sin estado en cada petición mediante `JwtAuthenticationFilter`.
   - **Idempotencia Transaccional:** Cabecera `X-Client-Transaction-Id: <UUID>` enviada en transacciones mutables (como creación de comandas `/api/v1/pos/orders` y sincronización por lotes `/api/v1/pos/sync`). El backend descarta o confirma duplicados sin reprocesar recetas ni duplicar cargos.
   - **Manejo Estandarizado de Errores:** Respuestas de error estructuradas conforme al estándar **RFC 7807 (`ProblemDetail`)**, serializando campos `type`, `title`, `status`, `detail`, `instance` y metadatos específicos del tenant.

3. **Backend API $\leftrightarrow$ PostgreSQL 16+:**
   - **Protocolo:** PostgreSQL Wire Protocol sobre TCP (puerto 5432).
   - **Conexiones:** Gestionadas mediante el pool de alto rendimiento **HikariCP**.
   - **Gestión de Esquema:** Versionado declarativo estricto con **Flyway** (`V1__init_schema.sql` para tablas maestras y `V2__spring_modulith_events.sql` para el registro de eventos).
   - **Zonas Horarias:** Conexión forzada a `time_zone = 'UTC'` (`TIMESTAMPTZ`) en base de datos; la conversión a hora de negocio local se efectúa con base en `branches.timezone` (ej. `'America/La_Paz'`).

---

## 4. Decisiones Técnicas y Arquitectónicas Clave (Trazabilidad ADR)

| Código ADR | Título de la Decisión | Impacto en la Topología de Contenedores |
| :--- | :--- | :--- |
| **[ADR-0001](../../../adr/ADR-0001-arquitectura-modular-monolito-spring-modulith.md)** | Monolito Modular con Spring Modulith en lugar de Microservicios | Se mantiene un único contenedor de backend para reducir la latencia de red, eliminar la complejidad de orquestación distribuida (Kubernetes/Istio) y asegurar transacciones ACID locales mediante eventos desacoplados. |
| **[ADR-0002](../../../adr/ADR-0002-estrategia-offline-first-pos-dexie-indexeddb.md)** | Estrategia Offline-First en POS con Dexie.js (IndexedDB) | Despliegue de un contenedor de persistencia en el navegador del cliente que amortigua fallos de conectividad en el salón comedor o caja, permitiendo cobrar y guardar comandas en cola local. |
| **[ADR-0003](../../../adr/ADR-0003-modelo-aislamiento-multitenant.md)** | Aislamiento Multi-Tenant por Columnas Discriminadoras | Un único esquema físico de base de datos PostgreSQL particionado lógicamente por `tenant_id` y `branch_id`, optimizando el uso de memoria del servidor y simplificando el mantenimiento de migraciones Flyway. |
| **[ADR-0004](../../../adr/ADR-0004-concurrencia-project-loom-virtual-threads.md)** | Hilos Virtuales de Java 25 (Project Loom) | El contenedor backend soporta un alto volumen de conexiones concurrentes de POS y sincronización sin agotar el pool de hilos del sistema operativo (`spring.threads.virtual.enabled=true`). |
| **[ADR-0005](../../../adr/ADR-0005-publicacion-eventos-transaccionales-modulith.md)** | Publicación Transaccional de Eventos con Outbox Pattern | Las tablas `event_publication` y `event_publication_archive` dentro de PostgreSQL garantizan que el evento `OrderPaidEvent` emitido por el POS se procese en Kardex y Finanzas con garantía *at-least-once*, evitando pérdidas ante caídas de servidor. |
| **[ADR-0006](../../../adr/ADR-0006-manejo-fechas-zonas-horarias.md)** | Manejo Estricto de Fechas y Zonas Horarias | PostgreSQL almacena instantes transaccionales en UTC (`TIMESTAMPTZ`) y fechas puras sin hora (`DATE`). El backend y el frontend respetan la zona horaria configurada en la sucursal (`branches.timezone`). |

---

## 5. Navegación y Enlaces Relacionados

- **Nivel Superior:** [Índice de la Bóveda de Arquitectura](../README.md)
- **Nivel 3 Backend:** [Componentes de Backend Spring Modulith](./c4_level3_backend_modulith.md)
- **Nivel 3 Frontend:** [Componentes de Frontend Angular Zoneless](./c4_level3_frontend_angular.md)
- **Persistencia Física:** [ERD de Base de Datos PostgreSQL](../02-domain-and-data/database_erd.md) y [Esquema IndexedDB Dexie.js](../02-domain-and-data/dexie_local_schema.md)
- **Flujos Transaccionales:** [Secuencia POS $\rightarrow$ Event $\rightarrow$ Kardex](../04-behavior-and-flows/sequence_pos_order_deduction.md) y [Secuencia Offline-First e Idempotencia](../04-behavior-and-flows/sequence_pos_offline_sync.md)
