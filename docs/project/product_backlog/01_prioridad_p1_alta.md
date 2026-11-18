---
title: "Product Backlog: Prioridad P1 (Alta Prioridad)"
category: "product-backlog"
priority: "P1"
tags:
  - backlog
  - p1
  - high-priority
  - operations
  - security
  - mvp-v1
last_updated: "2026-11-17"
status: "active"
---

# 🟠 Product Backlog — Prioridad P1 (Alta Prioridad)

> [!NOTE]
> **Definición de Prioridad P1:** Funcionalidades críticas para la operación cotidiana del restaurante (ventas, finanzas, inventario, control de sesiones, roles personalizados, despliegue cloud y madurez del producto) que completan la experiencia operativa.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P2](./02_prioridad_p2_media.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P1

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-004** | Transferencias de Insumos entre Sucursales (`StockTransfer`) | Inventario Multi-Sucursal | En Backlog | `TASK-332` (Por Planificar) |
| **PB-005** | Consolidación Financiera y Reportería Multi-Sucursal | Finanzas / BI | En Backlog | Por Planificar |
| **PB-011** | Desacoplamiento de Repositorios en Módulos hacia APIs Públicas | Arquitectura Spring Modulith | Completado (Sprint 2) | `TASK-228` |
| **PB-012** | Estandarización de Excepciones y Formato ProblemDetail RFC 7807 | Arquitectura REST & Resiliencia | Completado (Sprint 2) | `TASK-229` |
| **PB-016** | Capa de Repositorio Local en Frontend (DAL para Dexie.js) | Frontend / Dexie.js | Completado (Sprint 2) | `TASK-230` |
| **PB-020** | Motor de Promociones, Combos, Modificadores y Notas de Cocina | Motor de Precios & POS | Completado (Sprint 5) | `TASK-208`–`210`, `501`, `502`, `634` |
| **PB-021** | Inicialización de Stock, Recepción de Compras, Lotes y Mermas | Inventario & Kardex | Parcial (`TASK-206`, `635` / `636`) | `TASK-206`, `TASK-635` / `TASK-636` |
| **PB-024** | Panel de Analítica, Métricas de Negocio y Rentabilidad Neta | BI / Analítica Financiera | En Backlog (Pospuesto) | Por Planificar |
| **PB-025** | Infraestructura de Despliegue, Contenerización y Certificación Cloud | DevOps & Rollout Cloud | Parcial (Docker/CI Listo; OCI `TASK-609`) | `TASK-601`, `606`, `608`, `610` / `609` |
| **PB-027** | Ciclo de Vida de Sesiones (JWT Expiration & Silent Refresh) | Seguridad & Autenticación | Completado (Sprint 3) | `TASK-314`, `315` *(timeout `TASK-640`)* |
| **PB-028** | Diseño Responsivo Integral y Navegación Adaptativa Móvil | Frontend UI/UX | Completado (Sprint 4) | `TASK-212`, `316`, `317`, `411` |
| **PB-029** | Visor y Control de Ventas del Turno Activo para Cajero | Finanzas & POS | Completado (Sprint 4) | `TASK-410` |
| **PB-030** | Histórico Gerencial de Turnos y Auditoría de Ventas Multicriterio | Finanzas & Reporting | Completado (Sprint 5) | `TASK-508`, `TASK-626` |
| **PB-031** | Flujo de Cancelación de Pedidos, Devolución y Kardex (Mermas vs Reintegro) | POS & Kardex | Completado (Sprint 5) | `TASK-503`, `TASK-504` |
| **PB-032** | Modalidad de Pedidos "Para Llevar" (`Takeout`) y `Delivery` | POS / Flujo Operativo | Completado (Sprint 5) | `TASK-505` *(parámetros `TASK-640`)* |
| **PB-033** | Modelo de Inventario Parametrizable (Almacén General vs Sucursal) | Inventario & Arquitectura | En Backlog | `TASK-331`, `TASK-332` |
| **PB-034** | Menús Diarios, Plantillas, Sesiones Compartidas y Cupos | Turnos & Catálogo POS | Completado (Sprint 4 / Amp. S6) | `TASK-407`–`409`, `634` |
| **PB-035** | Motor Desacoplado de Almacenamiento Agnóstico (Local + S3/R2/OCI) | Arquitectura Cloud & Storage | Completado (Sprint 6) | `TASK-603`, `TASK-629` |
| **PB-037** | Exclusión de Insumos BOM y Notas de Cocina en Comanda | Catálogo, POS & Kardex | Completado (Sprint 5) | `TASK-501`, `TASK-502` |
| **PB-039** | Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y RBAC | Seguridad & RBAC | Completado (Sprint 3) | `TASK-336`–`338` |
| **PB-042** | Design System de Restaurante y Suite de Componentes Standalone | UI/UX & Frontend | Completado (Sprint 4) | `TASK-404`, `405` |
| **PB-043** | Motor Unificado de Notificaciones (`Toasts` en `z-[9999]`) | UI/UX & Feedback | Completado (Sprint 4) | `TASK-406` |
| **PB-044** | Modelo Flexible de Recetas (BOM vs Unidad vs Libre) | Catálogo & Kardex | Completado (Sprint 4) | `TASK-407`–`409` *(UI `TASK-640`)* |
| **PB-046** | Batería E2E Crítica de Negocio y Gobernanza de Testing | Calidad & Testing | Completado (Sprint 4/5) | `TASK-412`, `TASK-511` |
| **PB-047** | Scripts de Reset, Estandarización de Semillas y Aprovisionamiento Dev | Infraestructura & DB / Calidad | Completado (Sprint 4) | `TASK-413` |
| **PB-049** | Rediseño Arquitectónico y UX de Cero del Tutorial y Tour Guiado Interactivo | Onboarding & UX | En Backlog (Retrospectiva S4) | Por Planificar |
| **PB-057** | Arranque Limpio Go-Live (`Baseline System Startup` sin Tenancies Demo) | Startup & Seguridad | Completado (Sprint 6) | `TASK-602`, `TASK-612` |
| **PB-058** | Auditoría Local DevSecOps: Secretos, SAST/SCA e Imágenes Docker | DevSecOps & Seguridad | Completado (Sprint 6) | `TASK-607` |
| **PB-060** | Estabilización MVP v1: Catálogo Sucursal, Sesiones Compartidas, Combos Dinámicos e Insumos | Catálogo, POS & Seguridad | Completado (Sprint 6) | `TASK-628`–`635` |
| **PB-062** | Wiki / Manual Web Integrado en la Aplicación para Usuario Final | Documentación In-App / UX | En Backlog (Versión Futura) | `TASK-639` |
| **PB-063** | Completar Cableado de Parámetros Declarados, Selector `InventoryStrategy` y Caché Offline Combos/Menús | Config, POS, Catálogo & Dexie | En Backlog (Versión Futura) | `TASK-640` |

---

## Detalle Exhaustivo de Requerimientos P1

<a id="pb-004"></a>
### PB-004 [INVENTORY] [NEGOCIO]: Transferencias de Insumos entre Sucursales (`StockTransfer`)
- **Categoría:** Inventario Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Alcance:** Solicitud, despacho y confirmación de recepción de insumos entre sucursales de un mismo inquilino, con control de stock en tránsito y movimientos Kardex correlacionados.

---

<a id="pb-005"></a>
### PB-005 [FINANCE] [BI]: Consolidación Financiera y Reportería Multi-Sucursal
- **Categoría:** Finanzas / BI
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Alcance:** Vista ejecutiva consolidada para el propietario con múltiples locales: rentabilidad neta agregada, comparativas de ventas entre sucursales y análisis consolidado de gastos y mermas.

---

<a id="pb-011"></a>
### PB-011 [GLOBAL] [ARQUITECTURA]: Desacoplamiento de Repositorios en Módulos de Negocio hacia APIs Públicas Modulares
- **Categoría:** Arquitectura & Gobernanza / Spring Modulith
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 2 — TASK-228)`
- **Alcance:** Sustitución de inyecciones directas de repositorios ajenos por contratos `.api` (`BranchPublicApi`, `UserPublicApi`), garantizando cumplimiento con `ApplicationModules.verify()`.

---

<a id="pb-012"></a>
### PB-012 [GLOBAL] [ARQUITECTURA]: Estandarización de Excepciones de Dominio y Formato ProblemDetail (RFC 7807)
- **Categoría:** Arquitectura REST & Resiliencia
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 2 — TASK-229)`
- **Alcance:** Jerarquía de excepciones de negocio (`DomainException`), `@RestControllerAdvice` centralizado y manejo estandarizado `application/problem+json` con interceptor HTTP en frontend.

---

<a id="pb-016"></a>
### PB-016 [POS] [ARQUITECTURA]: Capa de Repositorio Local en Frontend (Data Access Layer para Dexie.js aislando `appDb`)
- **Categoría:** Frontend / Arquitectura y Dexie.js
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 2 — TASK-230)`
- **Alcance:** Aislar el acceso a IndexedDB mediante repositorios tipados en `src/app/core/repositories/`, desacoplando `PosService` y `FinanceService` de la instancia global `appDb`.

---

<a id="pb-020"></a>
### PB-020 [POS] [NEGOCIO]: Motor de Promociones, Combos, Modificadores y Extras en POS
- **Categoría:** Motor de Precios & POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Base Combos/Promos/Modificadores en Sprint 2 — TASK-208..210; Suite Completa de Extras y Notas en Sprint 5 — TASK-501/502 / ADR-0018; Combos Dinámicos MENU_COURSE en Sprint 6 — TASK-634)`
- **Alcance:** Packs gastronómicos, Happy Hour según huso de sucursal, modificadores con/sin costo, exclusiones con deducción configurable en Kardex, notas de cocina y combos configurables por tiempos de menú. *(Nota: Cupones promocionales avanzados podrán plantearse como extensión futura puntual).*

---

<a id="pb-021"></a>
### PB-021 [INVENTORY] [NEGOCIO]: Inicialización de Stock, Recepción de Compras, Lotes y Mermas
- **Categoría:** Inventario & Kardex / Ciclo de Abastecimiento
- **Prioridad:** `P1 - Alta`
- **Estado:** `Parcial (INITIAL_STOCK completado en TASK-206; saneamiento CRUD insumos y costo desde lote en TASK-635; estudio completo de compras/pre-elaborados/Kardex diferido a Post-MVP en TASK-636)`
- **Alcance:** Apertura de stock, edición segura de insumos y costeo aproximado por lote en MVP (`TASK-635`), y evolución hacia recepción de compras, insumos pre-elaborados y flujo completo de mermas (`TASK-636`).

---

<a id="pb-024"></a>
### PB-024 [FINANCE] [BI]: Panel de Analítica, Métricas de Negocio y Rentabilidad Neta
- **Categoría:** Business Intelligence / Analítica Financiera
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Pospuesto formalmente hasta consolidar ciclo de abastecimiento)`
- **Alcance:** Utilidad neta real (Ventas POS - Costo Kardex - Gastos Operativos), ticket promedio, ranking de platos y filtros multi-sucursal aislados por RBAC.

---

<a id="pb-025"></a>
### PB-025 [DEVOPS] [INFRA]: Infraestructura de Despliegue, Contenerización Docker y Certificación Cloud
- **Categoría:** DevOps & Rollout Cloud / Infraestructura
- **Prioridad:** `P1 - Alta`
- **Estado:** `Parcial (Contenerización Multi-Stage, Stack testing-local, Backups GPG y CI/CD 4 Ambientes completados en TASK-601, 606, 608 y 610; Provisión OCI y Smoke HTTPS Staging en curso paralelo en TASK-609)`
- **Alcance:** Multi-Stage Dockerfiles non-root, Twelve-Factor App, Caddy, Dozzle, scripts GPG de backup/restore, CI/CD GitHub Actions por tags/ambientes y despliegue vivo en Oracle Cloud Infrastructure (OCI).

---

<a id="pb-027"></a>
### PB-027 [AUTH] [SEGURIDAD]: Gestión del Ciclo de Vida de Sesiones de Usuario (`JWT Expiration`, `Silent Refresh` y `Timeout`)
- **Categoría:** Seguridad & Autenticación / Módulo Auth
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 3 — TASK-314 / ADR-0011 y TASK-315; pendiente cablear parámetro security.session_inactivity_timeout_minutes en TASK-640 / PB-063)`
- **Especificación Formal:** [ADR-0011](../../adr/0011_session_lifecycle_and_silent_refresh.md), [use_cases_sessions.md](../architecture/03-use-cases/use_cases_sessions.md), [sequence_session_lifecycle_and_refresh.md](../architecture/04-behavior-and-flows/sequence_session_lifecycle_and_refresh.md).
- **Alcance Funcional y Técnico:** Access Token corto + Refresh Token rotativo (`V8`), interceptor de renovación silenciosa, revocación de sesiones e `IdleTimerService` con advertencia preventiva.

---

<a id="pb-028"></a>
### PB-028 [UI/UX] [ARQUITECTURA]: Diseño Responsivo Integral, Navegación Adaptativa Móvil y Ergonomía de Espacio
- **Categoría:** Frontend UI/UX / Shell y Vistas Operativas
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 2–4 — TASK-212, TASK-316, TASK-317 y TASK-411)`
- **Alcance Funcional y Técnico:** Sidebar colapsable (mini-rail `76px` en escritorio, drawer en móvil), supresión de subtítulos redundantes en smartphones (`hidden sm:block`), menú contextual `[···]` en POS y tipografía tabular `font-mono` en Inventario/Kardex.

---

<a id="pb-029"></a>
### PB-029 [FINANCE] [NEGOCIO]: Visor y Control de Ventas del Turno Activo para Cajero (`/cash-control`)
- **Categoría:** Finanzas & POS / Control de Caja
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 — TASK-410 / ADR-0016)`
- **Alcance Funcional y Técnico:** Separación arquitectónica entre operativa diaria (`/cash-control`) y auditoría gerencial (`/finance`), turnos concurrentes multi-cajero (`finance.shift_concurrency_mode`), KPIs de gaveta y listado cronológico en vivo de comandas con filtros rápidos.

---

<a id="pb-030"></a>
### PB-030 [FINANCE] [BI]: Histórico Gerencial de Sesiones/Turnos y Auditoría de Ventas Multicriterio
- **Categoría:** Finanzas & Reporting / Módulo Finance
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 5 — TASK-508 / ADR-0023; filtros nulos PostgreSQL corregidos con JpaSpecificationExecutor en TASK-626)`
- **Alcance Funcional y Técnico:** Endpoints `/shifts/history` (huso local de sucursal `ADR-0006`) y `/shifts/{id}/audit-detail`, modal drill-down `<app-shift-detail-modal>` y exportador CSV con BOM UTF-8 (`;`).

---

<a id="pb-031"></a>
### PB-031 [POS] [NEGOCIO]: Flujo de Cancelación de Pedidos, Devolución de Importes y Tratamiento de Kardex (Mermas vs Reintegro)
- **Categoría:** POS & Kardex / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 5 — TASK-503 / ADR-0019 y TASK-504)`
- **Alcance Funcional y Técnico:** Máquina de estados `CANCELLED`, autorización dual (PIN in-situ y `<app-authorization-queue>`), evento `OrderCancelledEvent` con reversión `CANCELLATION_REVERSAL` vs `WASTE_ADJUSTMENT`, y reintegro en `total_cash_refunds`.

---

<a id="pb-032"></a>
### PB-032 [POS] [NEGOCIO]: Modalidad de Pedidos "Para Llevar" (`Takeout`), Encargos Programados y `Delivery`
- **Categoría:** Punto de Venta / Flujo Operativo
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 5 — TASK-505 / ADR-0020; pendiente cableado dinámico de parámetros pos.allow_scheduled_pickup y pos.delivery_fee_* en TASK-640 / PB-063)`
- **Alcance Funcional y Técnico:** Modalidades `DINE_IN`, `TAKEOUT`, `SCHEDULED_PICKUP` y `DELIVERY` (`V13`), tarifa `deliveryFee`, distintivos destacados en ticket y filtros en `/cash-control`.

---

<a id="pb-033"></a>
### PB-033 [INVENTORY] [ARQUITECTURA]: Modelo de Inventario Parametrizable (Almacén General Centralizado vs Directo por Sucursal)
- **Categoría:** Inventario & Arquitectura Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog`
- **Alcance:** Parámetro `INVENTORY_DISTRIBUTION_MODEL` (`DIRECT_PER_BRANCH` vs `CENTRAL_WAREHOUSE_HUB`) articulado con transferencias internas (`PB-004`).

---

<a id="pb-034"></a>
### PB-034 [CATALOG/POS] [NEGOCIO]: Gestión de Menús Diarios, Plantillas Reutilizables, Sesiones de Servicio y Cupos
- **Categoría:** Catálogo & Operación POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 — TASK-407..409 / ADR-0015; ampliado con BranchServiceSession, cupos ilimitados y combos dinámicos en Sprint 6 — TASK-634)`
- **Alcance Funcional y Técnico:** Plantillas multi-día en `/catalog/menus` (`V9`, `V23`, `V24`), instanciación de menú a jornada compartida o turno, cupos finitos con decremento atómico (`@Version`) o ilimitados (`is_unlimited`), bloqueo `AGOTADO` y ajuste supervisado.

---

<a id="pb-035"></a>
### PB-035 [STORAGE] [ARQUITECTURA]: Motor Desacoplado de Almacenamiento Agnóstico (Local y S3/R2/OCI Object Storage)
- **Categoría:** Arquitectura Cloud & Media / Storage
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 — TASK-603 / ADR-0028; resolución de URLs /uploads/* en TASK-629)`
- **Alcance Funcional y Técnico:** Contrato `MediaStorageService` en `core.storage` con proveedores intercambiables `LOCAL` y `S3`, validación de *magic bytes*, configuración dinámica en `platform_settings` (`V18`) desde Backoffice y proxy `/uploads/*` en Caddy.

---

<a id="pb-037"></a>
### PB-037 [CATALOG] [NEGOCIO]: Modificaciones de Estructura de Plato en Pedido: Exclusión de Insumos BOM y Notas de Cocina
- **Categoría:** Catálogo, POS & Kardex / Recetas y Variaciones
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 5 — TASK-501 y TASK-502 / ADR-0018)`
- **Alcance Funcional y Técnico:** Exclusiones configurables en modificadores/comanda con impacto controlado en Kardex y notas libres por ítem impresas en ticket.

---

<a id="pb-039"></a>
### PB-039 [AUTH] [SEGURIDAD]: Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y Asignación de Usuarios
- **Categoría:** Seguridad & RBAC / Gestión Dinámica de Accesos
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 3 — TASK-336 / ADR-0010, TASK-337 y TASK-338)`
- **Alcance Funcional y Técnico:** Roles propios por tenant (`V7`), privilegios agrupados legibles, asignación multi-sucursal (`user_branches`), expiración temporal y centro de administración `/users/roles`.

---

<a id="pb-042"></a>
### PB-042 [UI/UX] [ARQUITECTURA]: Design System de Restaurante, Suite de Componentes Standalone y Estandarización Visual
- **Categoría:** Frontend UI/UX / Design System & Arquitectura de Componentes
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 — TASK-404 / ADR-0014 y TASK-405)`
- **Alcance Funcional y Técnico:** Tokens CSS Custom Properties en caliente, modos Claro/Oscuro WCAG AA, `<app-page-wrapper>`, `<app-button>`, `<app-badge>`, `<app-card>`, `<app-modal>`, `<app-icon>` y `<app-theme-customizer>`.

---

<a id="pb-043"></a>
### PB-043 [UI/UX] [ARQUITECTURA]: Motor Unificado de Notificaciones (`Toasts` con `z-[9999]` y Alertas en Línea)
- **Categoría:** Frontend UI/UX / Feedback al Usuario & Infraestructura
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 — TASK-406)`
- **Alcance Funcional y Técnico:** `NotificationService` + `<app-toast-portal>` en `z-[9999]` visible sobre modales, conectado a `errorInterceptor` RFC 7807, y `<app-form-field>` con validación inline.

---

<a id="pb-044"></a>
### PB-044 [CATALOG] [NEGOCIO]: Modelo Flexible de Recetas e Inventario de Platos (BOM Estricto, Unidad Física o Venta Libre)
- **Categoría:** Catálogo & Kardex / Reglas de Negocio Configurables
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado en Motor Backend (Sprint 4 — TASK-407..409 / ADR-0015; pendiente selector individual UI y endpoint PATCH en TASK-640 / PB-063)`
- **Alcance Funcional y Técnico:** Estrategias polimórficas `RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT` y `COMMERCIAL_NO_INVENTORY` en `InventoryStrategyFactory`.

---

<a id="pb-046"></a>
### PB-046 [QA] [CALIDAD]: Batería E2E Crítica de Negocio (Ciclo Integral POS + Turno + Gastos + Arqueo) y Gobernanza de Testing
- **Categoría:** Calidad & Testing / Regresión Transaccional
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 y 5 — TASK-412, TASK-511 / ADR-0017)`
- **Alcance Funcional y Técnico:** Estándar multinivel N1–N4, suites Playwright con Page Object Models (`shift-lifecycle.spec.ts`, `sprint5-journey.spec.ts`) e inventario maestro en `docs/testing/`.

---

<a id="pb-047"></a>
### PB-047 [DEVOPS/DB] [CALIDAD]: Scripts de Reset, Estandarización de Semillas y Aprovisionamiento Limpio de Base de Datos Dev
- **Categoría:** Infraestructura & Base de Datos / Calidad Operativa
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 4 — TASK-413)`
- **Alcance Funcional y Técnico:** Scripts idempotentes `scripts/db/reset_database.sql` y `reset_database.sh` (`--purge`, `--full-recreate`) más enriquecimiento de seeders de desarrollo (`@Profile({"dev", "test"})`).

---

<a id="pb-049"></a>
### PB-049 [ONBOARDING] [UX]: Rediseño Arquitectónico y UX de Cero del Tutorial y Tour Guiado Interactivo
- **Categoría:** Experiencia de Usuario & Onboarding / Gamificación Operativa
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Retrospectiva Sprint 4)`
- **Alcance:** Rediseño integral del tour hacia micro-metas prácticas de restaurante con transiciones limpias entre rutas y soporte móvil táctil.

---

<a id="pb-057"></a>
### PB-057 [DEVOPS/STARTUP] [ARQUITECTURA]: Arranque Limpio Go-Live (`Baseline System Startup` sin Tenancies Demo)
- **Categoría:** Inicialización de Sistema & Seguridad Supra-Tenant
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 — TASK-602 / ADR-0027 y TASK-612)`
- **Alcance:** Aislamiento `@Profile({"dev", "test"})` de seeders demo, tabla `platform_admins` para bootstrap limpio por variables de entorno y ocultamiento de botones demo en login ERP fuera de `dev`.

---

<a id="pb-058"></a>
### PB-058 [DEVSECOPS] [SEGURIDAD]: Auditoría Local DevSecOps (Secretos, Vulnerabilidades SAST/SCA y Escaneo de Contenedores)
- **Categoría:** DevSecOps / Seguridad Continua & Higiene de Código
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 — TASK-607)`
- **Alcance:** Script `scripts/security/run-security-scan.sh`, `.gitleaks.toml`, pre-commit hook, SpotBugs + FindSecBugs (`-Psecurity`), `npm audit --omit=dev` y Trivy FS + imágenes Docker.

---

<a id="pb-060"></a>
### PB-060 [CATALOG/POS] [MVP]: Estabilización MVP v1 — Catálogo Multi-Sucursal, Jornadas Compartidas (`BranchServiceSession`), Combos Dinámicos e Insumos
- **Categoría:** Catálogo, POS, Seguridad & Inventario / Madurez MVP v1
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 — TASK-628 a TASK-635)`
- **Contexto:** Bloque integral de estabilización ejecutado sobre `release/sprint-6` tras pruebas conjuntas de usuario para dejar el MVP v1 coherente de extremo a extremo.
- **Alcance Funcional y Técnico:**
  - Política configurable `security.enforce_password_change_on_reset` y bloqueo de rutas ERP hacia `/profile` (`TASK-628`).
  - Resolución cross-origin de imágenes `/uploads/...` y proxy Caddy (`TASK-629`) + limpieza de jerga técnica en UI (`TASK-630`).
  - Edición completa de platos y edición/eliminación segura de categorías (`TASK-631`); aislamiento por sucursal (`catalog.scope_mode = BRANCH_ISOLATED`) y clonado explícito hacia sucursal destino (`TASK-632`).
  - Jornada de sucursal compartida (`branch_service_sessions`, `V23`, `finance.auto_open_full_catalog_session`), CRUD de combos dinámicos `MENU_COURSE` y grupos de complementos, plantillas multi-día (`V24`), cupos ilimitados (`is_unlimited`), restricción `allow_individual_sale` y selecciones en comanda con deducción en Kardex (`TASK-633`, `TASK-634`).
  - Edición y borrado seguro de insumos, distinción Stock Inicial vs Ajuste y cálculo de costo unitario desde Valor Total del Lote (`TASK-635`).

---

<a id="pb-062"></a>
### PB-062 [DOCS/UX] [ERP/BACKOFFICE]: Wiki / Manual Web Integrado en la Aplicación para Usuario Final
- **Categoría:** Documentación In-App / Experiencia de Usuario
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Versión Futura — TASK-639)`
- **Contexto:** Los usuarios operativos (administradores, cajeros, cocina) necesitan consultar guías paso a paso y ayuda estructurada (hoy disponibles en Markdown bajo [`docs/manuals/restaurante/`](../../manuals/restaurante/README.md)) directamente dentro de la aplicación web.
- **Alcance Funcional y Técnico:**
  - Evaluar librerías comunitarias compatibles con Markdown (peso en bundle, mantenimiento, compatibilidad Angular 22 / React 19) frente a una implementación propia liviana, aplicando estrictamente la **Regla 9** y co-diseño **Regla 8** de `AGENTS.md` antes de decidir.
  - Exponer manual navegable contextualizado por módulo y rol.
- **Criterios de Aceptación:**
  - Evaluación comparativa documentada y aprobada por el usuario antes de implementar; cero impacto negativo en tiempo de carga inicial del POS.

---

<a id="pb-063"></a>
### PB-063 [CONFIG/POS/CATALOG/DEXIE] [ARQUITECTURA]: Completar Cableado de Parámetros Declarados, Selector `InventoryStrategy` y Caché Offline Dexie para Combos/Menús
- **Categoría:** Configuración, POS, Catálogo & Offline-First
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Versión Futura — TASK-640)`
- **Contexto:** Auditoría sobre el MVP detectó parámetros ya declarados en `SystemParameter.java` / `parameters-catalog.json` aún sin consumo en tiempo de ejecución, junto con dos brechas operativas pendientes en Catálogo y Dexie.js.
- **Alcance Funcional y Técnico:**
  1. **Timeout de Inactividad:** Conectar `security.session_inactivity_timeout_minutes` a `IdleTimerService.setTimeoutMinutes(...)` tras login y actualización en `/settings/parameters`.
  2. **Modalidades y Delivery en POS:** Cablear `pos.allow_scheduled_pickup`, `pos.delivery_fee_enabled` y `pos.delivery_fee_default` en POS UI/backend.
  3. **Selector `InventoryStrategy` por Plato (`ADR-0015`):** Exponer `PATCH /api/v1/catalog/dishes/{id}/inventory-strategy` en `CatalogController` y selector UI en `/catalog` (`RECIPE_BOM_STRICT`, `DISH_AS_INVENTORY_UNIT`, `COMMERCIAL_NO_INVENTORY`).
  4. **Caché Offline Dexie para Combos y Menú del Día:** Extender `LocalCatalogRepository` / `appDb` para almacenar y operar offline combos y menús activos del turno/jornada.
- **Criterios de Aceptación:**
  - Los 4 parámetros modifican efectivamente el comportamiento en caliente de sesión/POS; los combos y el menú diario operan offline con sincronización idempotente.
