---
title: "Product Backlog: Prioridad P0 (Máxima Prioridad / Crítica)"
category: "product-backlog"
priority: "P0"
tags:
  - backlog
  - p0
  - critical
  - tenant-lifecycle
  - security
last_updated: "2026-10-28"
status: "verified"
---

# 🔴 Product Backlog — Prioridad P0 (Máxima Prioridad / Crítica)

> [!IMPORTANT]
> **Definición de Prioridad P0:** Requerimientos fundacionales sin los cuales la plataforma multi-tenant no puede operar comercialmente, o defectos bloqueantes de infraestructura/seguridad.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P2](./02_prioridad_p2_media.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P0

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-001** | Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante) | Ciclo de Vida del Tenant | Completado | `TASK-333` (Diseño), `TASK-334`, `TASK-335`, `TASK-339` (Completado) |
| **PB-002** | Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner) | Gobernanza SaaS / Seguridad | En Backlog | Por Diseñar |
| **PB-003** | Backoffice Independiente (Frontend Desacoplado + BFF Dedicado) | Arquitectura / Plataforma | En Backlog | Por Diseñar |
| **PB-017** | Gestión y Configuración de Empresa (Tenant) y Sucursales | Configuración Organizacional | Completado | `TASK-202` (Sprint 2) |
| **PB-018** | Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios | Seguridad & RBAC | Completado | `TASK-203`, `TASK-204` (Sprint 2) |
| **PB-019** | Autenticación con Tenant Identifier y Selector de Sucursal | Seguridad & Tenancy | Completado | `TASK-201` (Sprint 2) |
| **PB-022** | Corrección y Estabilización de Suite de Pruebas Frontend en CI | CI/CD & Calidad | Completado | `TASK-011` (Sprint 1) |
| **PB-023** | Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD) | Arquitectura & Gobernanza | Completado | `TASK-012` a `TASK-017` |
| **PB-026** | Sistema Unificado de Parametrización Multinivel con Hot-Reload | Arquitectura & Core | Completado | `TASK-311` (Diseño), `TASK-312`, `TASK-313` |

---

## Detalle Exhaustivo de Requerimientos P0

### PB-001 [GLOBAL] [NEGOCIO]: Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante)
- **Categoría:** Ciclo de Vida del Tenant / Onboarding & Alta de Empresa
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Diseño en TASK-333 / ADR-0009; Implementación en TASK-334, TASK-335 y TASK-339)`
- **Contexto:** Actualmente el sistema asume la existencia estática o previa de tenants creados directamente en base de datos. Se requiere un flujo guiado, sencillo y automatizado para dar de alta a nuevos restaurantes, tanto en modalidad de autoregistro (*Self-Service*) como mediante aprovisionamiento asistido desde el Backoffice de la plataforma.
- **Alcance Funcional y Técnico:**
  - **Asistente Guiado de Onboarding (3 a 5 preguntas esenciales):**
    1. *Identidad Fiscal y Comercial:* Razón Social, Nombre Comercial, `tenant_slug` automático (inferido con debounce de 350ms, editable) y NIT/Tax ID opcional (`NULLABLE` en BD con índice condicional único `uq_tenant_nit`).
    2. *Sucursal Principal Matriz:* Nombre de sucursal matriz, dirección física, ubicación interactiva en mapa OpenStreetMap/Leaflet y huso horario local obligatorio según ADR-0006 (`America/La_Paz` por defecto).
    3. *Moneda y Símbolo:* Código ISO y símbolo monetario (`BOB` / `Bs` por defecto).
    4. *Preset Operativo Semilla (Alineado a PB-026 / TASK-311 / ADR-0009):*
       - *Comida Rápida (Fast Food):* Descuento automático permisivo, modo para llevar por defecto, cobro visual ultra-rápido.
       - *Restaurante Tradicional (A la carta):* Consumo en salón por defecto, deducción estricta, fotos de platos y notas/exclusiones de cocina.
       - *Bar / Cafetería:* Consumo rápido en barra, happy hour y sin bloqueo de PIN para agilidad de servicio.
       - *En Blanco / Personalizado:* Menú limpio para configuración manual o subida Excel.
    5. *Catálogo Semilla de Demostración:* Hasta 3 productos reales con insumos básicos para probar ventas de inmediato (editables/eliminables).
    6. *Cuenta Administradora:* Nombre completo (`full_name`), usuario (`username`), correo de contacto opcional y contraseña hasheada con Argon2id.
  - **Transacción Atómica de Alta en Backend (`OnboardingService`):**
    - Creación coordinada de `tenants`, `branches`, `app_users` y aplicación de los 2-3 parámetros semilla en `system_parameter_values` (aprovechando *Sparse Storage*, sin registros masivos en BD).
    - Retorno inmediato de credenciales con token JWT y contexto de sucursal activa (`X-Branch-Id`) para entrar directamente al POS (`/pos`) "Listo para Vender".
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
  - **Evolución en Sprint 3:** Ampliado hacia Roles Personalizados Dinámicos, Matriz de Privilegios Agrupados, Asignación Multi-Sucursal y Expiración Temporal de Roles en [`PB-039`](./01_prioridad_p1_alta.md#pb-039) (`Completado` en `TASK-336`, `TASK-337` y `TASK-338`).

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
  - **Evolución en Sprint 3:** Ampliado con Ciclo de Vida de Sesiones Seguras (Access Token + Refresh Token rotativo, Silent Refresh e Inactividad Parametrizable) en [`PB-027`](./01_prioridad_p1_alta.md#pb-027) (`Completado` en `TASK-314` y `TASK-315`).

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
- **Estado:** `Completado (Diseño en TASK-311 / ADR-0008; Implementación en TASK-312 y TASK-313)`
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

### PB-041 [POS] [SEGURIDAD/NEGOCIO]: Invariante de Venta Exclusiva en Turno Abierto y Blindaje Financiero en POS
- **Prioridad:** `P0 - Crítica (Bloqueante de Integridad Financiera)`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-POS-01)`
- **Contexto:** Durante la retrospectiva del Sprint 3 se detectó que el POS permite seleccionar platos, armar comandas y cobrar ventas en efectivo incluso cuando la caja está cerrada y no existe una sesión de turno (`cash_shifts`) abierta para el cajero o la sucursal. Esto destruye la trazabilidad contable: el dinero cobrado no ingresa a ningún turno, no se computa en el arqueo teórico de caja ni puede conciliarse en el cierre del día.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Análisis y Diseño (Spec-First):**
    - Definir invariante transaccional en backend y frontend: "Toda mutación de venta o cobro exige un `shift_id` activo (`OPEN`) vinculado a la sucursal del cajero".
    - Contrato de error RFC 7807: Código `SHIFT_REQUIRED_ERROR` (HTTP 409 Conflict) con mensaje: *"No existe un turno de caja abierto para registrar ventas"*.
    - Máquina de estados en frontend: Bloqueo reactivo de la interfaz del POS si `currentShift()` es `null` o no está `OPEN`.
  - **Fase 2: Implementación Backend y Frontend:**
    - Backend: Validación en `OrderService` / `modules.pos` antes de persistir o publicar `OrderPaidEvent`. El backend rechaza la transacción con HTTP 409 si no hay turno activo.
    - Frontend:
      - Banner de alerta visual en POS: *"Caja Cerrada — Debes abrir un turno para comenzar a vender"*.
      - Bloqueo de botones de agregar plato y cobrar comanda mientras la caja esté cerrada.
      - Botón de acción directa (CTA) que abre el modal de "Apertura de Turno" con fondo inicial sin salir del POS.
      - Eliminación del tag espurio "Pendiente" y clarificación del indicador de conectividad "En línea" (verde = sincronizado, amarillo = modo offline Dexie, rojo = sin red).
- **Criterios de Aceptación:**
  - Es físicamente imposible registrar una orden o cobro en backend y frontend sin un turno en estado `OPEN`.
  - Si el cajero intenta cobrar con el turno cerrado, la UI no lo permite y el backend rechaza cualquier petición manipulada con HTTP 409 ProblemDetail.
  - Al abrir el turno con fondo inicial, el POS se desbloquea reactivamente de inmediato sin recargar la página.
