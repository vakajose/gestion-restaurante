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
last_updated: "2026-11-17"
status: "active"
---

# 🔴 Product Backlog — Prioridad P0 (Máxima Prioridad / Crítica)

> [!IMPORTANT]
> **Definición de Prioridad P0:** Requerimientos fundacionales sin los cuales la plataforma multi-tenant no puede operar comercialmente, o defectos bloqueantes de infraestructura/seguridad.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P1](./01_prioridad_p1_alta.md) | [Prioridad P2](./02_prioridad_p2_media.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P0

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-001** | Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante) | Ciclo de Vida del Tenant | Completado (Sprint 3) | `TASK-333`–`335`, `339` |
| **PB-002** | Rol "Árbitro del Sistema" (`SUPERADMIN` / Backoffice SaaS) | Gobernanza SaaS / Seguridad | Completado (Sprint 6) | `TASK-602`, `TASK-604` |
| **PB-003** | Backoffice Independiente (SPA React LTS + Módulo Spring Modulith) | Arquitectura / Plataforma | Completado (Sprint 6) | `TASK-605`, `TASK-616`–`625` |
| **PB-017** | Gestión y Configuración de Empresa (Tenant) y Sucursales | Configuración Organizacional | Completado (Sprint 2) | `TASK-202`, `TASK-614` |
| **PB-018** | Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios | Seguridad & RBAC | Completado (Sprint 2) | `TASK-203`, `204`, `625`, `628` |
| **PB-019** | Autenticación con Tenant Identifier y Selector de Sucursal | Seguridad & Tenancy | Completado (Sprint 2) | `TASK-201` |
| **PB-022** | Corrección y Estabilización de Suite de Pruebas Frontend en CI | CI/CD & Calidad | Completado (Sprint 1) | `TASK-011` |
| **PB-023** | Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD) | Arquitectura & Gobernanza | Completado (Sprint 1) | `TASK-012`–`017` |
| **PB-026** | Sistema Unificado de Parametrización Multinivel con Hot-Reload | Arquitectura & Core | Completado (Sprint 3) | `TASK-311`–`313` |
| **PB-041** | Invariante de Venta Exclusiva en Turno Abierto y Blindaje POS | Integridad Financiera / POS | Completado (Sprint 4) | `TASK-401`–`403` |
| **PB-068** | Desacoplamiento de Infraestructura Compartida (`Caddy`/`Dozzle`) vs Servicios de Producción en OCI | Infraestructura & DevOps | En Backlog (Próximo Sprint) | Por Planificar |

---

## Detalle Exhaustivo de Requerimientos P0

<a id="pb-001"></a>
### PB-001 [GLOBAL] [NEGOCIO]: Onboarding del Cliente y Configuración Inicial de Empresa (Restaurante)
- **Categoría:** Ciclo de Vida del Tenant / Onboarding & Alta de Empresa
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 3 — TASK-333 / ADR-0009, TASK-334, TASK-335 y TASK-339; confirmación de clave en TASK-621)`
- **Contexto:** Se requería un flujo guiado, sencillo y automatizado para dar de alta a nuevos restaurantes, tanto en modalidad de autoregistro (*Self-Service*) como mediante aprovisionamiento asistido desde el Backoffice de la plataforma.
- **Alcance Funcional y Técnico:**
  - **Asistente Guiado de Onboarding:**
    1. *Identidad Fiscal y Comercial:* Razón Social, Nombre Comercial, `tenant_slug` automático (editable) y NIT/Tax ID opcional (`NULLABLE` en BD con índice condicional único `uq_tenant_nit`).
    2. *Sucursal Principal Matriz:* Nombre de sucursal matriz, dirección física y huso horario local obligatorio según [ADR-0006](../../adr/0006_datetime_and_timezone_handling.md) (`America/La_Paz` por defecto).
    3. *Moneda y Símbolo:* Código ISO y símbolo monetario (`BOB` / `Bs` por defecto).
    4. *Preset Operativo Semilla (`ADR-0009`):* Comida Rápida, Restaurante Tradicional, Bar/Cafetería o En Blanco.
    5. *Cuenta Administradora:* Nombre completo (`full_name`), usuario (`username`), correo de contacto opcional y contraseña hasheada con Argon2id.
  - **Transacción Atómica de Alta en Backend (`OnboardingService`):** Creación coordinada de `tenants`, `branches`, `app_users` y parámetros semilla en `system_parameter_values` (*Sparse Storage*), retornando JWT y `X-Branch-Id`.
- **Criterios de Aceptación:**
  - Creación atómica de empresa, sucursal, administrador y parámetros semilla sin inserciones masivas redundantes.
  - Sesión autenticada tras el registro con ingreso directo a la operación.

---

<a id="pb-002"></a>
### PB-002 [GLOBAL] [SEGURIDAD]: Rol "Árbitro del Sistema" (Platform Superadmin / SaaS Owner)
- **Categoría:** Gobernanza SaaS / Seguridad
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 6 — TASK-602 / ADR-0027 y TASK-604 / ADR-0029)`
- **Contexto:** Se requería un rol administrativo exclusivo para los operadores de plataforma, completamente desacoplado de las cuentas operativas de los restaurantes inquilinos (`app_users.tenant_id`).
- **Alcance Funcional y Técnico:**
  - **Aislamiento Supra-Tenant:** Tabla dedicada `platform_admins` (`V17`), tokens JWT con audiencia aislada `platform-backoffice` y módulo Spring Modulith independiente (`modules.backoffice`) + `core.entitlements`.
  - **Gestión de Inquilinos (Tenants) y Suscripciones:** Creación, activación, suspensión (`POS_AND_SHIFT_ONLY` / `TOTAL`), planes, pagos FIFO/crédito, periodos de gracia, gestión de usuarios del tenant (`/tenants/:id` en `TASK-625`) y telemetría.
  - **Parámetros Globales & Storage:** Edición de `platform_settings` (`V18`) para estrategia de storage `LOCAL` vs `S3`, límites y prueba de conexión.
  - **Observabilidad y Control de Logs en Caliente:** Consumo seguro de Spring Boot Actuator (`/actuator/health`, métricas, disco), cambio dinámico de niveles de log (`/actuator/loggers`) y auditoría paginada (`platform_audit_logs`).
- **Criterios de Aceptación:**
  - `/api/v1/backoffice/**` rechaza estrictamente cualquier token ERP sin audiencia de plataforma y privilegios `ROLE_SUPERADMIN`/`PLATFORM_OPERATOR`.
  - La suspensión de un tenant bloquea su operación en la siguiente petición.

---

<a id="pb-003"></a>
### PB-003 [GLOBAL] [ARQUITECTURA]: Backoffice Independiente (SPA React LTS + Módulo Backend Segregado)
- **Categoría:** Arquitectura / Plataforma
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 6 — TASK-605, TASK-616..625)`
- **Contexto y Justificación:** Las herramientas del Árbitro del Sistema no deben coexistir dentro del frontend Angular de los restaurantes ni sobrecargar su bundle o superficie de ataque.
- **Lineamientos Arquitectónicos:**
  - **Frontend Independiente en React 19 LTS:** Aplicación SPA hermana en `backoffice-frontend/` con TypeScript + Tailwind CSS, primitivas UI reutilizables (`Modal`, `Card`, `EmptyState`, `Badge`), modo oscuro (`ThemeProvider`), runtime `env.js` y `Dockerfile` multi-stage non-root.
  - **Backend Desacoplado en Spring Modulith:** Módulo `modules.backoffice` bajo rutas `/api/v1/backoffice/**` en la instancia PostgreSQL compartida con aislamiento lógico estricto.
  - **Documentación SDD Admin:** Formalizado en [`docs/backoffice/`](../../backoffice/README.md).
- **Criterios de Aceptación:**
  - `frontend/` Angular no contiene código ni rutas del Backoffice.
  - `backoffice-frontend/` cuenta con pruebas automatizadas Vitest/RTL (`11` archivos / `22` tests), lint limpio y build independiente.

---

<a id="pb-017"></a>
### PB-017 [COMPANY] [NEGOCIO]: Gestión y Configuración de Empresa (Tenant) y Sucursales
- **Categoría:** Configuración Organizacional / Core Negocio
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 2 — TASK-202; refresco reactivo mejorado en TASK-614)`
- **Contexto:** Permite al administrador gestionar la información legal, operativa y geográfica de su empresa y de todas sus sucursales.
- **Alcance Funcional y Técnico:**
  - CRUD global y por tenant de sucursales con aislamiento estricto vía `TenantContextHolder`.
  - Configuración obligatoria de huso horario IANA ([ADR-0006](../../adr/0006_datetime_and_timezone_handling.md)) para cómputo de turnos y promociones.
  - Vistas operativas en TailwindAdmin (`/company`).
- **Criterios de Aceptación:**
  - Implementado y validado con pruebas unitarias, modulares y actualización reactiva de `BranchContextService`.

---

<a id="pb-018"></a>
### PB-018 [AUTH] [SEGURIDAD]: Control de Acceso Basado en Roles (RBAC) y Gestión de Usuarios por Tenant
- **Categoría:** Seguridad & RBAC / Gestión de Personal
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 2 — TASK-203 y TASK-204; ampliado en PB-039, TASK-625 y TASK-628)`
- **Contexto:** Aislamiento y control de privilegios para colaboradores (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`) y roles personalizados.
- **Alcance Funcional y Técnico:**
  - Permisos granulares por recurso en tokens JWT (`permissions`).
  - Route Guards en Angular Zoneless (`authGuard`, `permissionGuard`) y filtrado dinámico del Sidebar según privilegios.
  - CRUD de usuarios con hashing Argon2id, vista "Mi Perfil" y cumplimiento de `mustChangePassword` gobernado por parámetro (`TASK-628`).
- **Criterios de Aceptación:**
  - Implementado y verificado; evolucionado hacia Roles Personalizados Dinámicos en [`PB-039`](./01_prioridad_p1_alta.md#pb-039).

---

<a id="pb-019"></a>
### PB-019 [AUTH] [SEGURIDAD]: Flujo de Autenticación con Tenant Identifier y Selector Contextual de Sucursal Activa
- **Categoría:** Seguridad & Tenancy / Experiencia de Acceso
- **Prioridad:** `P0 - Crítica`
- **Estado:** `Completado (Sprint 2 — TASK-201; ampliado en PB-027)`
- **Contexto:** Autenticación cualificada por inquilino mediante slug comercial o NIT, con selección de sucursal de trabajo.
- **Alcance Funcional y Técnico:**
  - DTO `LoginRequest` con `tenantIdentifier`.
  - Inyección de cabecera `X-Branch-Id` en interceptor HTTP y validación contra pertenencia del usuario.
  - Selector interactivo en Topbar con señales reactivas de Angular.
- **Criterios de Aceptación:**
  - Implementado y verificado; ampliado con rotación de tokens e inactividad en [`PB-027`](./01_prioridad_p1_alta.md#pb-027).

---

<a id="pb-022"></a>
### PB-022 [DEVOPS] [CALIDAD]: Corrección y Estabilización de Suite de Pruebas Frontend en CI Pipeline
- **Categoría:** CI/CD & Calidad / Infraestructura Frontend
- **Prioridad:** `P0 - Crítica (Bloqueante en CI)`
- **Estado:** `Completado (Sprint 1 — TASK-011)`
- **Contexto:** En `.github/workflows/ci.yml`, el builder `@angular/build:unit-test` (Vitest + JSDOM) fallaba al pasar el argumento legado `--browsers=ChromeHeadless`.
- **Criterios de Aceptación:**
  - El workflow `ci.yml` ejecuta `npm test -- --watch=false` en JSDOM headless sin dependencias de navegador local.

---

<a id="pb-023"></a>
### PB-023 [DOCS] [ARQUITECTURA]: Bóveda de Arquitectura Viva y Especificaciones en Mermaid (SDD)
- **Categoría:** Arquitectura & Gobernanza / Specification-Driven Development (SDD)
- **Prioridad:** `P0 - Crítica (Fundacional)`
- **Estado:** `Completado (Sprint 1 — TASK-012 a TASK-017; mantenido continuamente bajo ADR-0007)`
- **Contexto:** Formalización de la Bóveda de Arquitectura viva (`docs/project/architecture/`) con diagramas Mermaid C4, ERD, Casos de Uso y Flujos de Comportamiento, gobernada por el ciclo SDD-SDLC.

---

<a id="pb-026"></a>
### PB-026 [CONFIG] [ARQUITECTURA]: Sistema Unificado de Parametrización Multinivel con Hot-Reload y Metadatos
- **Categoría:** Arquitectura & Core / Parametría del Sistema
- **Prioridad:** `P0 - Crítica (Columna Vertebral de Reglas de Negocio)`
- **Estado:** `Completado (Sprint 3 — TASK-311 / ADR-0008, TASK-312 y TASK-313; humanizado en PB-059 / TASK-510)`
- **Contexto:** Motor jerárquico y transversal para reglas de negocio configurables en caliente sin reiniciar servicios.
- **Alcance Funcional y Técnico:**
  - Jerarquía de resolución: `GLOBAL (Sistema)` $\rightarrow$ `TENANT (Empresa)` $\rightarrow$ `BRANCH (Sucursal)` con *Sparse Storage* (`system_parameter_values`, `V5`).
  - Módulo `modules.configuration` con `ConfigurationPublicApi`, catálogo JSON de metadatos (`parameters-catalog.json`) e invalidación inmediata de caché.
  - Vista interactiva `/settings/parameters` con buscador y selector de ámbito.
- **Criterios de Aceptación:**
  - Los cambios aplican en la siguiente operación en el ámbito correspondiente sin reiniciar el backend, con herencia limpia entre niveles.

---

<a id="pb-041"></a>
### PB-041 [POS] [SEGURIDAD/NEGOCIO]: Invariante de Venta Exclusiva en Turno Abierto y Blindaje Financiero en POS
- **Categoría:** Integridad Financiera / POS
- **Prioridad:** `P0 - Crítica (Bloqueante de Integridad Financiera)`
- **Estado:** `Completado (Sprint 4 — TASK-401 / ADR-0013, TASK-402 y TASK-403)`
- **Contexto:** Garantizar la trazabilidad contable impidiendo que se registren ventas o cobros en el POS cuando no existe un turno de caja (`cash_shifts`) en estado `OPEN`.
- **Alcance Funcional y Técnico:**
  - Backend: Validación estricta en `OrderServiceImpl` vía `FinancePublicApi`, retornando HTTP `409 Conflict` RFC 7807 (`SHIFT_REQUIRED_ERROR`) si la caja está cerrada y asociando `cashShiftId` y `user_id` a cada orden.
  - Frontend: Bloqueo reactivo del catálogo y comanda en POS con banner de caja cerrada y botón directo CTA para abrir turno sin salir de `/pos`; barra ámbar condicional para modo offline Dexie.js.
- **Criterios de Aceptación:**
  - Es imposible registrar una orden en backend o frontend sin un turno en estado `OPEN`.
  - Al abrir el turno con fondo inicial, el POS se desbloquea reactivamente de inmediato sin recargar la página.

---

<a id="pb-068"></a>
### PB-068 [DEVOPS/INFRA] [ARQUITECTURA]: Desacoplamiento de Infraestructura Compartida (`Caddy`/`Dozzle`) vs Servicios de Producción en OCI
- **Categoría:** Infraestructura & DevOps / Aislamiento de Ambientes
- **Prioridad:** `P0 - Crítica`
- **Estado:** `En Backlog (Próximo Sprint)`
- **Contexto:** Actualmente `caddy` y `dozzle` (servicios compartidos agnósticos al ambiente en `vnic-vm1-apps`) residen dentro de `docker/docker-compose.prod.yml`. Al levantar la infraestructura base de la VM, se levantan inadvertidamente los contenedores de aplicación de producción (`restaurant-prod-backend`, `restaurant-prod-backoffice`, `restaurant-prod-erp`) antes del Go-Live oficial, consumiendo RAM y exponiendo procesos innecesarios.
- **Alcance Técnico:**
  1. Extraer `caddy` y `dozzle` a un `docker/docker-compose.infra.yml` independiente (red Docker externa compartida).
  2. Dejar `docker/docker-compose.prod.yml` exclusivamente con los servicios de aplicación de producción (`backend`, `backoffice`, `erp`), análogo a `docker/docker-compose.staging.yml`.
  3. Actualizar los runbooks y workflows de despliegue (`cd-staging.yml` / `cd-production.yml`) para operar con `docker-compose.infra.yml` separado.
- **Criterios de Aceptación:**
  - `staging` puede operar con `infra` (Caddy + Dozzle) sin que exista ningún contenedor `restaurant-prod-*` de aplicación corriendo.
  - El despliegue de producción (`cd-production.yml`) levanta únicamente la app de `prod` conectada a la red de `infra`.
