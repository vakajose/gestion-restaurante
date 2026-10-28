---
title: "Product Backlog y Registro de Nuevas Funcionalidades"
category: "backlog"
tags:
  - backlog
  - requirements
  - user-stories
  - roadmap
last_updated: "2026-10-28"
status: "verified"
---

# Product Backlog y Registro de Nuevas Funcionalidades

Este documento centraliza el Product Backlog del sistema, ordenado estrictamente por **nivel de prioridad**. Aquí se registran las iniciativas, requerimientos, historias de usuario y decisiones arquitectónicas que surgen durante el desarrollo pero que **no se implementarán de forma inmediata**, sirviendo de repositorio para la planificación de futuros sprints.

Cada iniciativa incluye obligatoriamente en su título dos etiquetas canónicas:
1. **`[MODULO_CONCEPTUAL]`**: Indica el módulo funcional o capa arquitectónica a la que pertenece (`[GLOBAL]`, `[CORE]`, `[CONFIG]`, `[AUTH]`, `[COMPANY]`, `[POS]`, `[INVENTORY]`, `[FINANCE]`, `[CATALOG]`, `[STORAGE]`, `[SALON]`, `[UI/UX]`, `[DEVOPS]`, `[DOCS]`).
2. **`[TIPO_CAMBIO]`**: Indica la naturaleza técnica de la iniciativa (`[NEGOCIO]`, `[ARQUITECTURA]`, `[DISEÑO]`, `[SEGURIDAD]`, `[UI/UX]`, `[BD]`, `[INFRA]`, `[CALIDAD]`, `[BI]`).

---

## Matriz de Priorización

| Nivel | Clasificación | Criterio de Entrada |
| :--- | :--- | :--- |
| **P0** | **Crítica / Máxima** | Bloqueante estructural para el modelo de negocio, gobernanza SaaS, seguridad/RBAC, parametrización transversal o ciclo de vida inicial del inquilino (previo a rollout). |
| **P1** | **Alta** | Ampliación clave de la operación del cliente (multi-sucursal, flujo de ventas/caja, adaptabilidad móvil, catálogo avanzado, resiliencia arquitectónica y desacoplamiento). Programado para Sprint 2 y Sprint 3. |
| **P2** | **Media** | Integraciones periféricas (hardware comanderas, tributaria, soporte multi-motor, imágenes en POS, refactors internos de dominio y polimorfismo). |
| **P3** | **Baja / Futuro** | Mejoras de experiencia operativa avanzada (reservas de mesas/sillas, KDS táctil avanzado, plano interactivo de mesas). |

---

## Índice Rápido del Product Backlog

| ID | Iniciativa y Etiquetas | Prioridad | Estado / Sprint | Relación Operativa / Negocio |
| :--- | :--- | :---: | :---: | :--- |
| **PB-001** | `[GLOBAL] [NEGOCIO]` Onboarding del Cliente y Configuración Inicial de Empresa | P0 | En Backlog | Eje A (Tenancy Inicial) |
| **PB-002** | `[GLOBAL] [SEGURIDAD]` Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner) | P0 | En Backlog | Eje B (RBAC Supra-Tenant) |
| **PB-003** | `[GLOBAL] [ARQUITECTURA]` Backoffice Independiente (Frontend Desacoplado + BFF Dedicado) | P0 | En Backlog | Plataforma Proveedor |
| **PB-017** | `[COMPANY] [NEGOCIO]` Gestión y Configuración de Empresa (Tenant) y Sucursales | P0 | Sprint 2 (Completado) | UC-203 (Red Sucursales) |
| **PB-018** | `[AUTH] [SEGURIDAD]` Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios | P0 | Sprint 2 (Completado) | UC-202, UC-204 (Colaboradores) |
| **PB-019** | `[AUTH] [SEGURIDAD]` Login Calificado por Tenant y Selector Contextual de Sucursal | P0 | Sprint 2 (Completado) | UC-201, UC-205 (Sesión Activa) |
| **PB-022** | `[DEVOPS] [CALIDAD]` Corrección y Estabilización de Suite de Pruebas Frontend en CI | P0 | Resuelto (Hito 1) | Pipeline GitHub Actions |
| **PB-023** | `[DOCS] [ARQUITECTURA]` Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD) | P0 | Resuelto (Hito 1) | Gobernanza SDD |
| **PB-026** | `[CONFIG] [ARQUITECTURA]` Sistema Unificado de Parametrización Multinivel con Hot-Reload y Metadatos | P0 | Sprint 3 (Prioritario) | Parametría Central y Reglas |
| **PB-004** | `[INVENTORY] [NEGOCIO]` Transferencias de Insumos entre Sucursales (`StockTransfer`) | P1 | En Backlog | Inventario Multi-Sede |
| **PB-005** | `[FINANCE] [BI]` Consolidación Financiera y Reportería Multi-Sucursal | P1 | En Backlog | Finanzas Consolidadas |
| **PB-011** | `[GLOBAL] [ARQUITECTURA]` Desacoplamiento de Repositorios hacia APIs Públicas Modulares | P1 | Sprint 2 (Pendiente) | Gobernanza Modulith |
| **PB-012** | `[GLOBAL] [ARQUITECTURA]` Estandarización de Excepciones de Dominio (RFC 7807 ProblemDetail) | P1 | Sprint 2 (Pendiente) | Resiliencia REST |
| **PB-016** | `[POS] [ARQUITECTURA]` Capa de Repositorios Locales (DAL) para Dexie.js aislando `appDb` | P1 | Sprint 2 (Pendiente) | DAL Frontend Dexie |
| **PB-020** | `[POS] [NEGOCIO]` Motor de Promociones, Combos, Descuentos Directos y Cupones en POS | P1 | Sprint 2 (En Progreso) | UC-207, UC-208, UC-209 |
| **PB-021** | `[INVENTORY] [NEGOCIO]` Inicialización de Stock, Recepción de Compras y Mermas | P1 | Sprint 2 (En Progreso) | UC-206, Abastecimiento |
| **PB-024** | `[FINANCE] [BI]` Panel de Analítica, Métricas de Negocio y Rentabilidad Neta | P1 | Sprint 3 (Pospuesto) | BI & Margen Real |
| **PB-025** | `[DEVOPS] [INFRA]` Infraestructura de Despliegue, Contenerización Docker y Certificación Cloud | P1 | En Backlog (DevOps) | Rollout Nube & Certificación |
| **PB-027** | `[AUTH] [SEGURIDAD]` Gestión del Ciclo de Vida de Sesiones de Usuario (JWT Expiration & Silent Refresh) | P1 | Sprint 3 | Seguridad de Sesión |
| **PB-028** | `[UI/UX] [ARQUITECTURA]` Diseño Responsivo Integral y Navegación Adaptativa Móvil | P1 | Sprint 2-3 | Shell & Vistas Operativas |
| **PB-029** | `[FINANCE] [NEGOCIO]` Visor y Control de Ventas del Turno Activo para Cajero en Finanzas | P1 | Sprint 3 | Auditoría de Turno |
| **PB-030** | `[FINANCE] [BI]` Histórico Gerencial de Sesiones/Turnos y Auditoría de Ventas Multicriterio | P1 | Sprint 3 | Reportería de Caja |
| **PB-031** | `[POS] [NEGOCIO]` Flujo de Cancelación de Pedidos, Devolución de Importes y Tratamiento en Kardex | P1 | Sprint 3 | Devoluciones & Mermas |
| **PB-032** | `[POS] [NEGOCIO]` Modalidad de Pedidos "Para Llevar" (Takeout), Encargos y Delivery | P1 | Sprint 3 | Tipos de Despacho |
| **PB-033** | `[INVENTORY] [ARQUITECTURA]` Modelo de Inventario Parametrizable (Almacén General vs Sucursal) | P1 | Sprint 3 | Topología de Abastecimiento |
| **PB-034** | `[POS] [NEGOCIO]` Apertura de Turno con Selección de Menú, Cupos Máximos y Alerta de Stock | P1 | Sprint 3 | Control Operativo de Turno |
| **PB-035** | `[STORAGE] [ARQUITECTURA]` Motor Desacoplado de Procesamiento y Almacenamiento de Medios | P1 | Sprint 3 | Storage S3/Local & WebP |
| **PB-037** | `[CATALOG] [NEGOCIO]` Modificaciones de Estructura de Plato en Pedido (Exclusiones BOM y Notas) | P1 | Sprint 3 | Personalización de Comanda |
| **PB-006** | `[CORE] [ARQUITECTURA]` Desacoplamiento y Compatibilidad Multi-Motor SQL | P2 | En Backlog | Abstracción ANSI SQL |
| **PB-007** | `[POS] [NEGOCIO]` Facturación Electrónica Oficial | P2 | En Backlog | Fiscal & Impuestos |
| **PB-008** | `[POS] [INFRA]` Módulo de Impresión Física ESC/POS (Comanderas de Red) | P2 | En Backlog | Hardware de Red |
| **PB-013** | `[CORE] [ARQUITECTURA]` Evolución a Modelo de Dominio Rico en Order y CashShift | P2 | En Backlog | DDD Core Transaccional |
| **PB-014** | `[INVENTORY] [ARQUITECTURA]` Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex | P2 | En Backlog | OCP & Dynamic Injection |
| **PB-015** | `[POS] [ARQUITECTURA]` Extracción de Generador de Secuencia Diaria de Comandas | P2 | En Backlog | SRP POS & Concurrencia |
| **PB-036** | `[CATALOG] [UI/UX]` Catálogo Visual con Imágenes de Platos en Punto de Venta y Toggle Parametrizable | P2 | Sprint 3 | Experiencia Visual POS |
| **PB-009** | `[SALON] [UI/UX]` Mapa Visual Interactivo de Distribución de Mesas | P3 | En Backlog | Plano de Salón |
| **PB-010** | `[POS] [UI/UX]` Panel KDS Táctil con Métricas de Despacho en Cocina | P3 | En Backlog | Cocina KDS |
| **PB-038** | `[SALON] [NEGOCIO]` Módulo de Gestión de Reservas, Capacidad de Salón y Asignación de Mesas y Sillas | P3 | En Backlog | Reservas y Asignación |

---

## 1. Prioridad P0 (Máxima Prioridad)

### PB-001 [GLOBAL] [NEGOCIO]: Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante)
- **Categoría:** Ciclo de Vida del Tenant / Onboarding & Alta de Empresa
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog (Sprint 3 - Alta Prioridad)`
- **Contexto:** Actualmente el sistema asume la existencia estática o previa de tenants creados directamente en base de datos. Se requiere un flujo guiado, sencillo y automatizado para dar de alta a nuevos restaurantes, tanto en modalidad de autoregistro (*Self-Service*) como mediante aprovisionamiento asistido desde el Backoffice de la plataforma.
- **Alcance Funcional y Técnico:**
  - **Asistente Guiado de Onboarding (3 a 5 preguntas esenciales):**
    1. *Identidad Fiscal y Comercial:* Razón Social, Nombre Comercial, `tenant_slug` automático y NIT/Tax ID.
    2. *Sucursal Principal Matriz:* Nombre de sucursal matriz, dirección física, teléfono y huso horario local obligatorio según ADR-0006 (`America/La_Paz` por defecto).
    3. *Moneda y Símbolo:* Código ISO y símbolo monetario (`BOB` / `Bs`, `USD` / `$`).
    4. *Preset Operativo Semilla (Alineado a PB-026 / TASK-311):*
       - *Comida Rápida (Fast Food):* Descuento automático permisivo, modo para llevar por defecto, cobro visual ultra-rápido.
       - *Restaurante Tradicional (A la carta):* Consumo en salón por defecto, deducción estricta, fotos de platos y notas/exclusiones de cocina.
       - *Bar / Cafetería:* Consumo rápido en barra, happy hour y sin bloqueo de PIN para agilidad de servicio.
    5. *Cuenta Administradora:* Nombre de usuario (`username`), correo de contacto opcional y contraseña segura hasheada con Argon2id.
  - **Transacción Atómica de Alta en Backend (`OnboardingService`):**
    - Creación coordinada de `tenants`, `branches`, `app_users` y aplicación de los 2-3 parámetros semilla en `system_parameter_values` (aprovechando *Sparse Storage*, sin registros masivos en BD).
    - Retorno inmediato de credenciales con token JWT y contexto de sucursal activa (`X-Branch-Id`) para entrar directamente al sistema "Listo para Vender".
  - **Soporte Dual:** Mismo servicio de dominio utilizado para autoregistro público (`POST /api/v1/auth/onboarding`) y para aprovisionamiento por Superadmin desde Backoffice (`POST /api/v1/backoffice/tenants`).
- **Criterios de Aceptación:**
  - El asistente completa la creación de la empresa, sucursal, administrador y parámetros semilla en menos de 10 segundos en una única transacción atómica.
  - No se insertan filas innecesarias en la base de datos: el nuevo tenant hereda limpiamente el 95%+ de configuraciones desde el catálogo en código (*Sparse Storage*).
  - El usuario queda autenticado de inmediato tras el registro y aterriza en el panel principal con su sucursal matriz activa.

---

### PB-002 [GLOBAL] [SEGURIDAD]: Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner)
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

### PB-003 [GLOBAL] [ARQUITECTURA]: Backoffice Independiente (Frontend Desacoplado + BFF Dedicado)
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

### PB-017 [COMPANY] [NEGOCIO]: Gestión y Configuración de Empresa (Tenant) y Sucursales
- **Categoría:** Configuración Organizacional / Core Negocio
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Sprint 2 (Completado en TASK-202)`
- **Contexto:** Para que un restaurante opere comercialmente de forma autónoma en un entorno SaaS, el administrador debe poder gestionar la información legal, operativa y geográfica de su empresa y de todas sus sucursales.
- **Alcance Funcional y Técnico:**
  - CRUD global y por tenant de sucursales con aislamiento estricto vía `TenantContextHolder`.
  - Configuración obligatoria de huso horario IANA ([ADR-0006](file:///home/jvaca/proyectos/gestion-restaurante/docs/adr/0006_datetime_and_timezone_handling.md)) para cómputo de turnos y promociones.
  - Vistas operativas en TailwindAdmin v2.0 (`/settings/company` y `/settings/branches`).
- **Criterios de Aceptación:**
  - 100% implementado y validado en Sprint 2 con pruebas unitarias, modulares y E2E.

---

### PB-018 [AUTH] [SEGURIDAD]: Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios por Tenant
- **Categoría:** Seguridad & RBAC / Gestión de Personal
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Sprint 2 (Completado en TASK-203 y TASK-204)`
- **Contexto:** Aislamiento y control de privilegios para colaboradores (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`).
- **Alcance Funcional y Técnico:**
  - Permisos granulares por recurso en tokens JWT (`permissions`).
  - Route Guards en Angular Zoneless y filtrado dinámico del Sidebar según rol.
  - CRUD de usuarios con hashing Argon2id y vista "Mi Perfil".
- **Criterios de Aceptación:**
  - 100% implementado y verificado en Sprint 2.

---

### PB-019 [AUTH] [SEGURIDAD]: Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa
- **Categoría:** Seguridad & Tenancy / Experiencia de Acceso
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Sprint 2 (Completado en TASK-201)`
- **Contexto:** Autenticación cualificada por inquilino mediante slug comercial o NIT, con selección de sucursal de trabajo.
- **Alcance Funcional y Técnico:**
  - DTO `LoginRequest` con `tenantIdentifier`.
  - Inyección de cabecera `X-Branch-Id` en interceptor HTTP y validación contra pertenencia del usuario.
  - Selector interactivo en Topbar con señales reactivas de Angular.
- **Criterios de Aceptación:**
  - 100% implementado y verificado en Sprint 2.

---

### PB-022 [DEVOPS] [CALIDAD]: Corrección y Estabilización de Suite de Pruebas Frontend en CI Pipeline
- **Categoría:** CI/CD & Calidad / Infraestructura Frontend
- **Prioridad:** `P0 - Crítica (Bloqueante en CI)`
- **Estado:** `Implementado / Resuelto (TASK-011)`
- **Contexto:** En cada commit y push hacia el repositorio remoto, el job `Frontend CI (Angular 22)` en GitHub Actions (`.github/workflows/ci.yml`) fallaba a los 23 segundos con error de configuración al intentar ejecutar `npm test -- --watch=false --browsers=ChromeHeadless`. Angular 22 utiliza el nuevo builder `@angular/build:unit-test` respaldado por Vitest y JSDOM. El argumento legado `--browsers` exigía runners de navegador no instalados.
- **Criterios de Aceptación:**
  - El workflow `ci.yml` ejecuta los tests en modo headless JSDOM (`npm test -- --watch=false`), aprobando los 103 tests en < 5s.

---

### PB-023 [DOCS] [ARQUITECTURA]: Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD)
- **Categoría:** Arquitectura & Gobernanza / Specification-Driven Development (SDD)
- **Prioridad:** `P0 - Crítica (Fundacional)`
- **Estado:** `Implementado / Resuelto (TASK-012 a TASK-017)`
- **Contexto:** Formalización de la Bóveda de Arquitectura viva en Obsidian (`docs/project/architecture/`) con 16 diagramas Mermaid esenciales y protocolo SDD.

---

### PB-026 [CONFIG] [ARQUITECTURA]: Sistema Unificado de Parametrización Multinivel con Hot-Reload y Metadatos
- **Categoría:** Arquitectura & Core / Parametría del Sistema
- **Prioridad:** `P0 - Crítica (Columna Vertebral de Reglas de Negocio)`
- **Estado:** `En Backlog (Sprint 3 - Alta Prioridad)`
- **Contexto:** Para evitar la sobre-especialización o rigidez ante clientes diversos, cualquier regla o comportamiento de negocio debe ser completamente parametrizable. Actualmente existen tablas relacionales `tenant_settings` y `branch_settings`, pero se requiere un motor integral optimizado, jerárquico y transversal, similar a las configuraciones de Visual Studio Code (búsqueda rápida, categorización transversal y herencia de ámbitos).
- **Alcance Funcional y Técnico:**
  - **Fase 1: Análisis y Diseño Arquitectónico (Spec-First):**
    - Especificar la jerarquía de resolución de parámetros: `GLOBAL (Sistema)` $\rightarrow$ `TENANT (Empresa)` $\rightarrow$ `BRANCH (Sucursal)`.
    - Diseñar catálogo tipado de metadatos de configuración en código/BD: clave (`key`), tipo de dato (`BOOLEAN`, `NUMBER`, `STRING`, `DURATION`, `ENUM`), valor por defecto, validador (rango, regex), nivel jerárquico permitido y categoría funcional transversal (`POS`, `INVENTORY`, `FINANCE`, `SECURITY`, `CATALOG`).
    - Estrategia de persistencia: evaluación de modelo híbrido relacional/JSONB vs tablas clave-valor normalizadas, con soporte de auditoría de modificaciones.
    - Mecanismo de **Hot-Reload e Invalidación de Caché**: actualización atómica en memoria compartida o caché Spring/Redis, propagando el cambio en tiempo de ejecución sin reinicio de servicios ni desincronización de contexto.
    - Especificación de la experiencia de usuario (UX): centro de configuración con buscador reactivo por texto/etiqueta, filtros por módulo y selector de ámbito.
  - **Fase 2: Implementación Backend y Frontend:**
    - Módulo `modules.configuration` en Spring Modulith con `ConfigurationPublicApi`.
    - Endpoints REST para consulta resuelta por contexto y mutación tipada con auditoría.
    - Vista interactiva en Angular 22 / TailwindAdmin v2.0 (`/settings/parameters`) con acordeones por módulo, buscador instantáneo, badges de ámbito y validación en tiempo real.
- **Criterios de Aceptación:**
  - La alteración de un parámetro desde la interfaz aplica de inmediato en la siguiente operación del sistema en el ámbito correspondiente sin reiniciar el backend.
  - Los parámetros no definidos a nivel de sucursal heredan limpiamente el valor de la empresa, y estos a su vez el valor global por defecto.
  - La interfaz permite encontrar cualquier configuración por nombre o palabra clave en menos de 2 segundos.

---

## 2. Prioridad P1 (Alta Prioridad)

### PB-004 [INVENTORY] [NEGOCIO]: Transferencias de Insumos entre Sucursales (`StockTransfer`)
- **Categoría:** Inventario Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Alcance:** Solicitud, despacho y confirmación de recepción de insumos entre sucursales de un mismo inquilino, con control de stock en tránsito y movimientos Kardex correlacionados.

---

### PB-005 [FINANCE] [BI]: Consolidación Financiera y Reportería Multi-Sucursal
- **Categoría:** Finanzas / BI
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Alcance:** Vista ejecutiva consolidada para el propietario con múltiples locales: rentabilidad neta agregada, comparativas de ventas entre sucursales y análisis consolidado de gastos y mermas.

---

### PB-011 [GLOBAL] [ARQUITECTURA]: Desacoplamiento de Repositorios en Módulos de Negocio hacia APIs Públicas Modulares
- **Categoría:** Arquitectura & Gobernanza / Spring Modulith
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (Pendiente en TASK-228)`
- **Alcance:** Sustitución de inyecciones directas de repositorios ajenos por contratos `.api` (`BranchPublicApi`, `UserPublicApi`), garantizando cumplimiento con `ApplicationModules.verify()`.

---

### PB-012 [GLOBAL] [ARQUITECTURA]: Estandarización de Excepciones de Dominio y Formato ProblemDetail (RFC 7807)
- **Categoría:** Arquitectura REST & Resiliencia
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (Pendiente en TASK-229)`
- **Alcance:** Jerarquía de excepciones de negocio (`DomainException`), `@RestControllerAdvice` centralizado y manejo estandarizado `application/problem+json` con interceptor HTTP en frontend.

---

### PB-016 [POS] [ARQUITECTURA]: Capa de Repositorio Local en Frontend (Data Access Layer para Dexie.js aislando `appDb`)
- **Categoría:** Frontend / Arquitectura y Dexie.js
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (Pendiente en TASK-230)`
- **Alcance:** Aislar el acceso a IndexedDB mediante repositorios tipados en `src/app/core/repositories/`, desacoplando `PosService` y `FinanceService` de la instancia global `appDb`.

---

### PB-020 [POS] [NEGOCIO]: Motor de Promociones, Combos, Descuentos Directos y Cupones en POS
- **Categoría:** Motor de Precios & POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (En Progreso: Combos, Promociones y Modificadores completados; Cupones pendiente en TASK-207)`
- **Alcance:** Packs gastronómicos, Happy Hour según huso de sucursal, modificadores con precio, y cupones de descuento manual/código.

---

### PB-021 [INVENTORY] [NEGOCIO]: Inicialización de Stock, Recepción de Compras y Mermas
- **Categoría:** Inventario & Kardex / Ciclo de Abastecimiento
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (En Progreso: INITIAL_STOCK completado en TASK-206; Compras y Mermas pendiente en TASK-227)`
- **Alcance:** Apertura de stock, registro de compras con costo ponderado y conciliación de mermas con conteo físico.

---

### PB-024 [FINANCE] [BI]: Panel de Analítica, Métricas de Negocio y Rentabilidad Neta
- **Categoría:** Business Intelligence / Analítica Financiera
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 3 (Pospuesto formalmente por dependencias de abastecimiento y ventas)`
- **Alcance:** Utilidad neta real (Ventas POS - Costo Kardex - Gastos Operativos), ticket promedio, ranking de platos y filtros multi-sucursal aislados por RBAC.

---

### PB-025 [DEVOPS] [INFRA]: Infraestructura de Despliegue, Contenerización Docker y Certificación Cloud
- **Categoría:** DevOps & Rollout Cloud / Infraestructura
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (DevOps - TASK-301 a TASK-307)`
- **Alcance:** Multi-Stage Dockerfiles, Twelve-Factor App, `docker-compose.staging.yml`, almacenamiento S3, aprovisionamiento VPS/OCI, SSL Let's Encrypt, CI/CD GitHub Actions y backups automatizados.

---

### PB-027 [AUTH] [SEGURIDAD]: Gestión del Ciclo de Vida de Sesiones de Usuario (JWT Expiration, Silent Refresh y Timeout Parametrizable)
- **Categoría:** Seguridad & Autenticación / Módulo Auth
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Actualmente los tokens JWT carecen de un ciclo de vida con expiración estricta y renovación transparente. Una sesión puede permanecer abierta indefinidamente, vulnerando las directrices de seguridad. El tiempo de expiración debe ser parametrizable por el administrador de la empresa en función de las políticas operativas de cada restaurante.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Análisis y Diseño (Spec-First):**
    - Especificar la arquitectura de tokens duales: *Access Token* de corta duración (ej. 15-30 minutos) y *Refresh Token* seguro (HTTP-Only cookie o almacenamiento protegido en Dexie).
    - Integrar la expiración con el motor de parámetros (`PB-026`): clave `SESSION_INACTIVITY_TIMEOUT_MINUTES` con valor por defecto según rol (operativo POS vs administrativo).
    - Mecanismo de revocación inmediata: versión de sesión o blacklist en base de datos para invalidar accesos ante cambio de credenciales o baja de usuario.
  - **Fase 2: Implementación Backend y Frontend:**
    - Endpoint `/api/v1/auth/refresh` y validación de rotación de tokens.
    - Interceptor HTTP en Angular para renovación silenciosa (*silent refresh*) transparente mientras el usuario interactúa activamente con el sistema.
    - Modal de advertencia de sesión por expirar ante inactividad y panel de auditoría de sesiones activas en "Mi Perfil".
- **Criterios de Aceptación:**
  - Si el usuario deja de interactuar con el sistema por el lapso configurado, la sesión caduca automáticamente y redirige al login.
  - Mientras el usuario interactúe con el POS o la administración, el token se renueva en segundo plano sin interrumpir la operación ni perder comandas en curso.

---

### PB-028 [UI/UX] [ARQUITECTURA]: Diseño Responsivo Integral y Navegación Adaptativa Móvil (Sidebar Colapsable, POS Móvil e Inventario)
- **Categoría:** Frontend UI/UX / Shell y Vistas Operativas
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 2 Corrección Inmediata / Sprint 3 Vistas)`
- **Contexto:** La interfaz actual no se adapta adecuadamente a pantallas móviles o tablets pequeñas. Existe un defecto crítico en el layout donde el menú lateral (Sidebar) no se colapsa en pantallas pequeñas, ocupando el espacio visual e impidiendo operar desde teléfonos celulares.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Corrección Inmediata de Shell y Navegación (Sprint 2/3):**
    - Implementar comportamiento de *Drawer / Off-canvas* para el Sidebar en pantallas `< 1024px` con botón de hamburguesa en Topbar y fondo oscuro con backdrop (`overlay`).
    - Cierre automático del menú al navegar o al hacer clic fuera del panel en dispositivos móviles.
  - **Fase 2: Adaptabilidad del Punto de Venta (POS Móvil):**
    - Rediseño responsivo de la interfaz POS: catálogo en tarjetas compactas o listas táctiles, carrito de compras flotante o deslizable inferior (*bottom-sheet*) que permite seleccionar platos y abrir el resumen de cobro con una sola mano.
  - **Fase 3: Adaptabilidad de Inventario y Finanzas:**
    - Conversión de tablas complejas de Kardex, arqueo de caja y compras a formato de tarjetas apilables con información clave en pantallas pequeñas.
- **Criterios de Aceptación:**
  - En pantallas de smartphone (`< 768px`), el menú lateral permanece oculto por defecto y se despliega limpiamente como overlay al tocar el botón hamburguesa.
  - Un cajero o mesero puede registrar un pedido completo en POS y cobrarlo desde un teléfono móvil sin desbordamientos de pantalla ni scroll horizontal.

---

### PB-029 [FINANCE] [NEGOCIO]: Visor y Control de Ventas del Turno Activo para Cajero en Módulo de Finanzas
- **Categoría:** Finanzas & POS / Control de Caja
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Actualmente el cajero no dispone de una vista para auditar o revisar los pedidos cobrados durante su turno en curso. Requiere acceder al listado de comandas pagadas, su método de pago y monto para validar el estado de su sesión antes de realizar el arqueo.
- **Alcance Funcional y Técnico:**
  - Incorporar pestaña "Ventas del Turno" en la vista de finanzas/caja (`/finance`), complementando las pestañas existentes de Arqueo y Gastos Menores.
  - Consulta filtrada por el `cash_shift_id` activo del cajero en sesión.
  - Tabla interactiva con: número de ticket, hora de cobro, total facturado, método de pago (`CASH`, `CARD`, `QR`, mixto), estado y botón para ver el detalle de los ítems de la comanda.
  - Tarjetas de resumen rápido en cabecera: Total vendido en el turno, cantidad de tickets y desglose de totales por medio de pago.
- **Criterios de Aceptación:**
  - El cajero visualiza únicamente las ventas asociadas a su turno abierto en tiempo real.
  - Los totales desglosados coinciden de forma exacta con los importes teóricos computados en el panel de arqueo de caja.

---

### PB-030 [FINANCE] [BI]: Histórico Gerencial de Sesiones/Turnos y Auditoría de Ventas Multicriterio con Filtros Consolidados
- **Categoría:** Finanzas & Reporting / Módulo Finance
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** El administrador y supervisor de restaurante necesitan auditar turnos pasados cerrados, consultar el histórico de ventas y movimientos de caja por rango de fechas, sucursal, cajero y platillos vendidos.
- **Alcance Funcional y Técnico:**
  - Vista gerencial de reportería histórica de turnos: listado de turnos cerrados con fechas de apertura/cierre (convertidas al huso local de la sucursal según ADR-0006), cajero responsable, saldo inicial, total ventas, total egresos, saldo real arqueado y diferencia (cuadre, sobrante o faltante).
  - Inspección en profundidad: al seleccionar un turno, desplegar todas las ventas, egresos y movimientos de auditoría que ocurrieron durante dicha sesión.
  - Filtros avanzados: por rango de fechas, sucursal activa, cajero y tipo de resultado de arqueo.
  - Exportación de resumen de turno para archivo administrativo.
- **Criterios de Aceptación:**
  - El administrador puede reconstruir cualquier turno histórico y ver cada venta individual con sus medios de pago.
  - Las consultas de fechas respetan estrictamente la zona horaria de la sucursal consultada.

---

### PB-031 [POS] [NEGOCIO]: Flujo de Cancelación de Pedidos, Devolución de Importes y Tratamiento de Kardex (Mermas vs Reintegro)
- **Categoría:** POS & Kardex / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** En la operación real, existen clientes que desisten de una orden después de pagada o durante su preparación. El sistema debe gestionar la devolución del dinero y decidir el tratamiento de inventario según las reglas parametrizables del restaurante: si el plato no se cocinó, los insumos se reintegran; si ya se preparó, los insumos deben registrarse como merma/desperdicio.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Diseño de Flujo (Spec-First):**
    - Máquina de estados de la orden incorporando `CANCELLED` y motivos tipados de cancelación.
    - Matriz de impacto en inventario parametrizable vía `PB-026`:
      - *Cancelación antes de preparación:* Insumos descontados se reintegran automáticamente al stock disponible en `branch_stock` con movimiento Kardex `CANCELLATION_REVERSAL`.
      - *Cancelación después de preparación / plato servido:* El inventario NO regresa al stock disponible; se registra automáticamente un movimiento Kardex de pérdida/desperdicio `WASTE_ADJUSTMENT` asociado a la orden cancelada.
    - Flujo financiero: autorización requerida (PIN o credencial de supervisor/admin), registro de movimiento de egreso por devolución (`REFUND`) en el turno activo para que el arqueo de efectivo mantenga cuadre exacto.
  - **Fase 2: Implementación Backend y Frontend:**
    - Evento `OrderCancelledEvent` consumido por Kardex y Finanzas mediante `@ApplicationModuleListener`.
    - Diálogo modal de cancelación en POS con selección de motivo, confirmación de estado de elaboración y captura de autorización.
- **Criterios de Aceptación:**
  - Cancelar un pedido pagado registra la devolución en el turno activo y cuadra la caja física.
  - Los insumos se reintegran o se registran como merma de forma determinista y auditable según el estado de preparación.

---

### PB-032 [POS] [NEGOCIO]: Modalidad de Pedidos "Para Llevar" (Takeout), Encargos Programados y Control de Despacho
- **Categoría:** Punto de Venta / Flujo Operativo
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Los pedidos no se limitan a consumo en mesa local; gran parte del volumen de ventas corresponde a pedidos para llevar (en espera presencial), encargos programados con retiro diferido y despachos para delivery propio o plataformas externas.
- **Alcance Funcional y Técnico:**
  - Selector de modalidad de servicio en la cabecera de la comanda en POS: `DINE_IN` (Mesa/Local), `TAKEOUT` (Para llevar presencial), `SCHEDULED_PICKUP` (Encargo programado) y `DELIVERY` (Envío a domicilio).
  - Campos contextuales según modalidad: nombre del cliente, teléfono de contacto, hora pactada de entrega y observaciones de empaque.
  - Distintivo visual claro y destacado en ticket impreso y pantalla de cocina (KDS).
  - Filtro por modalidad de despacho en el listado de ventas activas.
- **Criterios de Aceptación:**
  - El ticket de comanda destaca prominentemente el rótulo "PARA LLEVAR" o "DELIVERY" con los datos de contacto y hora pactada.
  - El cajero puede cambiar entre modalidades de despacho con un solo toque en el POS antes o durante el cobro.

---

### PB-033 [INVENTORY] [ARQUITECTURA]: Modelo de Inventario Parametrizable (Almacén General Centralizado vs Directo por Sucursal)
- **Categoría:** Inventario & Arquitectura Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Los modelos de cadena gastronómica varían: algunos restaurantes compran y reciben directamente en cada sucursal (modelo descentralizado actual), mientras que otros disponen de un centro de distribución o almacén central que recibe compras al por mayor y distribuye a las sucursales mediante transferencias internas.
- **Alcance Funcional y Técnico:**
  - Parametrizar a nivel de empresa (`PB-026`) el parámetro `INVENTORY_DISTRIBUTION_MODEL`:
    - `DIRECT_PER_BRANCH`: Cada sucursal gestiona sus propias compras y recepciones directamente.
    - `CENTRAL_WAREHOUSE_HUB`: Se designa una sucursal o sede física como "Almacén Central", canalizando las órdenes de compra maestras y restringiendo las recepciones de sucursales operativas a transferencias de stock internas (`PB-004`).
  - Adaptar permisos RBAC y validaciones en el módulo de compras para respetar el modelo configurado.
- **Criterios de Aceptación:**
  - Si una empresa activa el modelo centralizado, las sucursales satélite reciben insumos exclusivamente vía transferencia desde el almacén central.

---

### PB-034 [POS] [NEGOCIO]: Apertura de Turno con Selección de Menú, Cupos Máximos y Alerta de Stock de Insumos
- **Categoría:** Turnos & Catálogo POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Al iniciar una jornada o turno de atención, no siempre se comercializa el 100% de los platos del catálogo maestro. El administrador de la sucursal requiere seleccionar qué platos estarán activos en la sesión y opcionalmente definir cupos máximos de preparación diaria (ej. "Hoy solo disponemos de 30 platos del menú especial"). Además, el sistema debe alertar preventivamente si el stock físico de insumos no cubre las porciones proyectadas.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Diseño (Spec-First):**
    - Flujo extendido en modal de apertura de turno: además del fondo inicial, opción de "Configurar Menú del Turno".
    - Algoritmo de verificación de stock teórico: compara las porciones proyectadas contra la receta BOM y el saldo actual en `branch_stock`, señalando en amarillo/rojo los insumos insuficientes sin bloquear obligatoriamente la apertura (advertencia informativa).
    - Parámetro de configuración (`PB-026`): `SHIFT_MENU_SELECTION_MODE` (`DISABLED`, `OPTIONAL`, `MANDATORY`).
  - **Fase 2: Implementación Backend y Frontend:**
    - Entidad `shift_menu_overrides` asociando `cash_shift_id`, `dish_id` y `daily_quota`.
    - En POS, decremento reactivo del cupo disponible por plato con indicativo visual de "Agotado en turno" al alcanzar el límite pactado.
- **Criterios de Aceptación:**
  - Al abrir turno, el sistema emite un reporte preventivo indicando si los insumos disponibles son suficientes para las cantidades estimadas.
  - Al venderse la última unidad del cupo pactado, el plato se bloquea en POS evitando sobreventas.

---

### PB-035 [STORAGE] [ARQUITECTURA]: Motor Desacoplado de Procesamiento y Almacenamiento de Medios (Optimización WebP y Soporte S3/Local)
- **Categoría:** Arquitectura Cloud & Media / Storage
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** El almacenamiento de fotos de platos, logos corporativos y comprobantes de gastos (`evidence_url`) no debe almacenarse en base de datos ni depender del sistema de archivos efímero de contenedores Docker. Se requiere una arquitectura sin estado (*stateless*), desacoplada y con optimización automática de imágenes para garantizar rendimiento y mínimo ancho de banda.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Diseño (Spec-First):**
    - Contrato de servicio agnóstico `MediaStorageService` en `core.storage`: métodos `upload(File, path)`, `delete(path)`, `resolveUrl(path)`.
    - Pipeline de procesamiento y optimización de imágenes en servidor (ej. librería `TwelveMonkeys` o `Thumbnailator`): conversión automática a formato WebP, compresión inteligente y generación de versiones escaladas (miniatura `120x120` para POS, mediana `400x400` para catálogo y evidencia original optimizada para gastos).
    - Soporte multi-entorno: almacenamiento en disco local mapeado en volumen para desarrollo/staging, y cliente S3/Cloudflare R2/OCI Object Storage para producción mediante variables de entorno (`PB-025` / Twelve-Factor App).
  - **Fase 2: Implementación del Motor:**
    - Implementación completa del servicio, endpoints de subida multipart con validación estricta de tipo MIME y tamaño máximo, y reutilización en gastos de caja chica (`expenses`) y catálogo de platos.
- **Criterios de Aceptación:**
  - Una imagen de 5MB subida por un usuario se comprime y convierte a WebP de menos de 100KB en miniatura sin degradación visual en POS.
  - El cambio de almacenamiento de disco local a S3 en producción se realiza únicamente mediante variables de entorno sin modificar código de aplicación.

---

### PB-037 [CATALOG] [NEGOCIO]: Modificaciones de Estructura de Plato en Pedido: Exclusión de Insumos BOM y Notas de Cocina
- **Categoría:** Catálogo, POS & Kardex / Recetas y Variaciones
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** Los comensales solicitan personalizaciones estructurales sobre los platos estándar (ej. "Sin arroz", "Sin ensalada", "Poco picante"). Actualmente se cuenta con modificadores que agregan extras con precio (`TASK-210`), pero no existe un mecanismo para excluir ingredientes de la receta base ni registrar notas libres de cocina con control parametrizable sobre el inventario.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Diseño (Spec-First):**
    - Extensión de la selección de ítem en comanda POS: acordeón "Quitar ingredientes" listando los componentes de la receta BOM del plato que admiten exclusión.
    - Parámetro configurable por empresa/sucursal (`PB-026`): `EXCLUSION_REDUCES_KARDEX_DEDUCTION` (`true`: si pide "sin arroz", se descuenta menos arroz del Kardex; `false`: se descuenta la porción estándar y se asume porción desechada o no recalculable).
    - Campo de notas libres de cocina por ítem ("Bien cocido", "Salsa aparte") que viaja a comanda y KDS sin alterar inventario ni precio.
  - **Fase 2: Implementación Backend y Frontend:**
    - Entidad `order_item_exclusions`, adaptación del evento `OrderPaidEvent` y consumo en `KardexService` para aplicar la deducción ajustada según el parámetro activo.
    - Interfaz visual en modal de plato POS con checkboxes de exclusión de ingredientes y campo de nota rápida.
- **Criterios de Aceptación:**
  - Si un plato se pide con exclusión y la regla activa reduce el inventario, el movimiento Kardex descuenta únicamente los insumos efectivamente consumidos.
  - Las notas de cocina y exclusiones se imprimen claramente en la comanda para el cocinero.

---

## 3. Prioridad P2 (Media Prioridad)

### PB-006 [CORE] [ARQUITECTURA]: Desacoplamiento y Compatibilidad Multi-Motor SQL
- **Categoría:** Arquitectura de Datos
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Abstracción de dialectos relacionales en consultas JPA manteniendo estándar ANSI SQL para garantizar portabilidad futura.

---

### PB-007 [POS] [NEGOCIO]: Facturación Electrónica Oficial
- **Categoría:** Integraciones Fiscales
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Emisión de facturas electrónicas oficiales con firma digital, QR tributario y contingencia offline ante servicios de impuestos nacionales.

---

### PB-008 [POS] [INFRA]: Módulo de Impresión Física ESC/POS (Comanderas de Red)
- **Categoría:** Hardware / POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Envío directo de tickets vía TCP/IP a impresoras térmicas ESC/POS distribuidas por estaciones operativas (Cocina, Barra, Caja).

---

### PB-013 [CORE] [ARQUITECTURA]: Evolución a Modelo de Dominio Rico (Rich Domain Model) en Entidades Transaccionales
- **Categoría:** Arquitectura de Dominio / DDD
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Encapsulamiento de lógica transaccional dentro de `Order` y `CashShift`, evitando modelos anémicos y dispersión de reglas en servicios.

---

### PB-014 [INVENTORY] [ARQUITECTURA]: Polimorfismo e Inyección Dinámica del Strategy Pattern en Kardex
- **Categoría:** Patrones de Diseño / Módulo Inventory
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Inyección dinámica de estrategias mediante `Map<InventoryDeductionMode, DeductionStrategy>`, garantizando cumplimiento del Principio Abierto/Cerrado (OCP).

---

### PB-015 [POS] [ARQUITECTURA]: Extracción de Generador de Secuencia Diaria de Comandas (SRP en POS)
- **Categoría:** Clean Code & Concurrencia / Módulo POS
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog`
- **Alcance:** Delegación de correlativos diarios (`T-1001`) a un servicio atómico especializado seguro ante alta concurrencia.

---

### PB-036 [CATALOG] [UI/UX]: Catálogo Visual con Imágenes de Platos en Punto de Venta y Toggle Parametrizable
- **Categoría:** Catálogo & POS / UI Operativa
- **Prioridad:** `P2 - Media`
- **Estado:** `En Backlog (Sprint 3)`
- **Contexto:** En el punto de venta, la visualización de fotos de platos agiliza la identificación visual para el cajero. Sin embargo, en dispositivos de baja potencia o conexiones lentas, puede ser preferible una vista ligera de botones compactos.
- **Alcance Funcional y Técnico:**
  - Asociación de imagen al plato en catálogo mediante el motor `PB-035`.
  - Renderizado de tarjetas visuales en la cuadrícula de POS con miniaturas optimizadas WebP y carga diferida (*lazy loading*), con fallback a icono temático de categoría si no hay foto.
  - Parámetro de sucursal (`PB-026`): `POS_DISPLAY_DISH_IMAGES` (`true`: modo galería con fotos, `false`: modo compacto de texto y botones rápidos).
- **Criterios de Aceptación:**
  - El cajero puede activar o desactivar la visualización de imágenes desde la cabecera del POS según su preferencia o capacidad del dispositivo.
  - Las imágenes no ralentizan el scroll del catálogo ni consumen memoria excesiva en el navegador.

---

## 4. Prioridad P3 (Baja Prioridad / Futuro)

### PB-009 [SALON] [UI/UX]: Mapa Visual Interactivo de Distribución de Mesas
- **Categoría:** UX Salón / POS
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Editor interactivo drag & drop para diagramar salones y terrazas, con visualización gráfica del estado de mesas en tiempo real.

---

### PB-010 [POS] [UI/UX]: Panel KDS Táctil con Métricas de Despacho en Cocina
- **Categoría:** Operaciones / Cocina
- **Prioridad:** `P3 - Baja`
- **Estado:** `En Backlog`
- **Alcance:** Pantalla táctil interactiva para cocineros con cronómetros por comanda y métricas de tiempos de despacho.

---

### PB-038 [SALON] [NEGOCIO]: Módulo de Gestión de Reservas, Capacidad de Salón y Asignación de Mesas y Sillas
- **Categoría:** Salón & Atención / Experiencia de Cliente
- **Prioridad:** `P3 - Baja / Futuro no Urgente`
- **Estado:** `En Backlog (Futuras Iteraciones)`
- **Contexto:** Gestión de reservas previas por parte de clientes para asegurar disponibilidad en fechas y horarios pico, con control de capacidad física de comensales.
- **Alcance Funcional y Técnico:**
  - Agenda de reservas por fecha, turno y sucursal.
  - Registro de datos del comensal: nombre, teléfono, cantidad de personas, requerimientos especiales.
  - Asignación de mesas numeradas y sillas específicas para grupos grandes.
  - Estados de reserva: `PENDIENTE`, `CONFIRMADA`, `CLIENTE_SENTADO`, `CANCELADA`, `NO_SHOW` (no asistió).
- **Criterios de Aceptación:**
  - La asignación de reservas no permite sobrepasar la capacidad física máxima declarada para el salón en la franja horaria seleccionada.

---

### PB-039 [AUTH] [SEGURIDAD]: Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y Asignación de Usuarios
- **Categoría:** Seguridad & RBAC / Gestión Dinámica de Accesos
- **Prioridad:** `P1 - Alta (Sprint 3 - Prioridad después de TASK-333)`
- **Estado:** `En Backlog (Por Diseñar en TASK-336)`
- **Contexto:** Actualmente los roles del sistema son estáticos (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`). Cada restaurante u operador gastronómico necesita flexibilidad para crear roles a medida (ej. "Encargado de Compras", "Auditor Nocturno", "Jefe de Barra", "Subgerente"), agrupando recursos y operaciones (`read`, `write`, `approve`, `export`) en *Privilegios* cohesivos y asignándolos a los colaboradores.
- **Alcance Funcional y Técnico:**
  - **Catálogo de Recursos y Privilegios:** Definición jerárquica de recursos (`pos`, `inventory`, `finance`, `catalog`, `users`, `settings`) con permisos atómicos agrupados en privilegios comprensibles para el usuario de negocio (ej. *"Operar Punto de Venta"*, *"Autorizar Gastos Menores"*, *"Ajustar Kardex y Mermas"*, *"Ver Reportes Financieros"*).
  - **Presets Semilla por Entorno:** Al darse de alta la empresa en Onboarding (`PB-001`), se inicializan roles predeterminados sugeridos según el tipo de negocio (Pizzería/Comida Rápida, Restaurante a la Carta, Bar/Cafetería).
  - **Gestión de Roles Personalizados por Tenant:** CRUD de roles propios de la empresa (`custom_roles`, `role_permissions` en PostgreSQL), permitiendo al Administrador marcar privilegios mediante switches o checkboxes en la UI.
  - **Resolución Dinámica en Seguridad:** Integración en `UserPublicApi` para mapear los roles y permisos calculados al token JWT y a los Route Guards / Directivas de Angular.
  - **Vistas en Frontend:** Centro de Gestión de Roles y Permisos en `/users/roles` con asignación interactiva a colaboradores.
- **Criterios de Aceptación:**
  - El administrador puede crear un rol con privilegios específicos, asignarlo a un usuario y verificar que los permisos se reflejen de inmediato en su sesión sin reinicio de la plataforma.
  - No se permite eliminar ni despojar de permisos al rol nativo `ADMIN_TENANT` para evitar bloqueos del sistema.
