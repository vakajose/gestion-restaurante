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
last_updated: "2026-10-28"
status: "verified"
---

# 🟠 Product Backlog — Prioridad P1 (Alta Prioridad)

> [!NOTE]
> **Definición de Prioridad P1:** Funcionalidades críticas para la operación cotidiana del restaurante (ventas, finanzas, inventario, control de sesiones, roles personalizados) que completan la experiencia operativa o solventan requerimientos prioritarios de clientes potenciales.
> ⬅️ **Navegación:** [Índice General del Backlog](./README.md) | [Prioridad P0](./00_prioridad_p0_critica.md) | [Prioridad P2](./02_prioridad_p2_media.md) | [Prioridad P3](./03_prioridad_p3_baja_futuro.md)

---

## Índice de Ítems P1

| Identificador | Título | Categoría | Estado | Tareas Asociadas |
| :--- | :--- | :--- | :--- | :--- |
| **PB-004** | Transferencias de Insumos entre Sucursales (`StockTransfer`) | Inventario Multi-Sucursal | En Backlog (Sprint 5) | `TASK-332` |
| **PB-005** | Consolidación Financiera y Reportería Multi-Sucursal | Finanzas / BI | En Backlog | Por Planificar |
| **PB-011** | Desacoplamiento de Repositorios en Módulos hacia APIs Públicas | Arquitectura Spring Modulith | Completado | `TASK-228` (Sprint 2) |
| **PB-012** | Estandarización de Excepciones y Formato ProblemDetail RFC 7807 | Arquitectura REST & Resiliencia | Completado | `TASK-229` (Sprint 2) |
| **PB-016** | Capa de Repositorio Local en Frontend (DAL para Dexie.js) | Frontend / Dexie.js | Completado | `TASK-230` (Sprint 2) |
| **PB-020** | Motor de Promociones, Combos, Descuentos Directos y Cupones | Motor de Precios & POS | En Progreso (Parcial) | `TASK-208`, `TASK-209`, `TASK-210` (Hecho) / `TASK-207` |
| **PB-021** | Inicialización de Stock, Recepción de Compras y Mermas | Inventario & Kardex | En Progreso (Parcial) | `TASK-206` (Hecho) / `TASK-227` |
| **PB-024** | Panel de Analítica, Métricas de Negocio y Rentabilidad Neta | BI / Analítica Financiera | En Backlog (Pospuesto) | Por Planificar |
| **PB-025** | Infraestructura de Despliegue, Contenerización y Certificación Cloud | DevOps & Rollout Cloud | En Progreso (Docker/CI Listo; OCI en `TASK-609`) | `TASK-601`, `TASK-606`, `TASK-608` (Hecho) / `TASK-609` |
| **PB-027** | Ciclo de Vida de Sesiones (JWT Expiration & Silent Refresh) | Seguridad & Autenticación | Completado | `TASK-314` (Diseño), `TASK-315` (Completado) |
| **PB-028** | Diseño Responsivo Integral y Navegación Adaptativa Móvil | Frontend UI/UX | Completado (TASK-212, TASK-316, TASK-317, TASK-411) | `TASK-212`, `TASK-316`, `TASK-317`, `TASK-411` (Completado) |
| **PB-029** | Visor y Control de Ventas del Turno Activo para Cajero | Finanzas & POS | Completado (Sprint 4) | `TASK-410` |
| **PB-030** | Histórico Gerencial de Turnos y Auditoría de Ventas Multicriterio | Finanzas & Reporting | Completado (Sprint 5) | `TASK-508` |
| **PB-031** | Flujo de Cancelación de Pedidos, Devolución y Kardex (Mermas vs Reintegro) | POS & Kardex | Completado (Sprint 5) | `TASK-503`, `TASK-504` |
| **PB-032** | Modalidad de Pedidos "Para Llevar" (Takeout) y Delivery | POS / Flujo Operativo | Completado (Sprint 5) | `TASK-505` |
| **PB-033** | Modelo de Inventario Parametrizable (Almacén General vs Sucursal) | Inventario & Arquitectura | En Backlog | `TASK-331` (Diseño), `TASK-332` |
| **PB-034** | Apertura de Turno con Selección de Menú, Cupos y Alerta de Stock | Turnos & Catálogo POS | Completado (Sprint 4) | `TASK-407`, `TASK-408`, `TASK-409` |
| **PB-035** | Motor Desacoplado de Almacenamiento Agnóstico (Local + S3/R2/OCI) | Arquitectura Cloud & Storage | Completado (Sprint 6) | `TASK-603` |
| **PB-037** | Exclusión de Insumos BOM y Notas de Cocina en Comanda | Catálogo, POS & Kardex | Completado (Sprint 5) | `TASK-501`, `TASK-502` |
| **PB-039** | Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y RBAC | Seguridad & RBAC | Completado | `TASK-336` (Diseño), `TASK-337`, `TASK-338` (Completado) |
| **PB-042** | Design System de Restaurante y Suite de Componentes Standalone | UI/UX & Frontend | Completado (Sprint 4) | `TASK-404`, `TASK-405` |
| **PB-043** | Motor Unificado de Notificaciones (Toasts en z-[9999]) | UI/UX & Feedback | Completado (Sprint 4) | `TASK-406` |
| **PB-044** | Modelo Flexible de Recetas (BOM vs Unidad vs Libre) | Catálogo & Kardex | Completado (Sprint 4) | `TASK-407`, `TASK-408`, `TASK-409` |
| **PB-046** | Batería E2E Crítica de Negocio y Gobernanza de Testing | Calidad & Testing | Completado (Sprint 4) | `TASK-412` |
| **PB-047** | Scripts de Reset, Estandarización de Semillas y Aprovisionamiento Limpio de Base de Datos Dev | Infraestructura & DB / Calidad | Completado (Sprint 4) | `TASK-413` |
| **PB-049** | Rediseño Arquitectónico y UX de Cero del Tutorial y Tour Guiado Interactivo | Onboarding & UX | En Backlog (Retrospectiva S4) | Por Planificar |
| **PB-057** | Arranque Limpio Go-Live (Baseline System Startup sin Tenancies Demo) | Startup & Seguridad | Completado (Sprint 6) | `TASK-602` |
| **PB-058** | Auditoría Local DevSecOps: Escaneo de Secretos, SAST/SCA e Imágenes Docker | DevSecOps & Seguridad | Completado (Sprint 6) | `TASK-607` |

---

## Detalle Exhaustivo de Requerimientos P1

### PB-004 [INVENTORY] [NEGOCIO]: Transferencias de Insumos entre Sucursales (`StockTransfer`)
- **Categoría:** Inventario Multi-Sucursal
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 5)`
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
- **Estado:** `Sprint 2 (Completado en TASK-228)`
- **Alcance:** Sustitución de inyecciones directas de repositorios ajenos por contratos `.api` (`BranchPublicApi`, `UserPublicApi`), garantizando cumplimiento con `ApplicationModules.verify()`.

---

### PB-012 [GLOBAL] [ARQUITECTURA]: Estandarización de Excepciones de Dominio y Formato ProblemDetail (RFC 7807)
- **Categoría:** Arquitectura REST & Resiliencia
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (Completado en TASK-229)`
- **Alcance:** Jerarquía de excepciones de negocio (`DomainException`), `@RestControllerAdvice` centralizado y manejo estandarizado `application/problem+json` con interceptor HTTP en frontend.

---

### PB-016 [POS] [ARQUITECTURA]: Capa de Repositorio Local en Frontend (Data Access Layer para Dexie.js aislando `appDb`)
- **Categoría:** Frontend / Arquitectura y Dexie.js
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 2 (Completado en TASK-230)`
- **Alcance:** Aislar el acceso a IndexedDB mediante repositorios tipados en `src/app/core/repositories/`, desacoplando `PosService` y `FinanceService` de la instancia global `appDb`.

---

### PB-020 [POS] [NEGOCIO]: Motor de Promociones, Combos, Descuentos Directos, Modificadores y Extras en POS
- **Categoría:** Motor de Precios & POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Progreso (Combos, Promociones y Modificadores base en Sprint 2; Cupones en TASK-207; Suite completa de Extras y Complementos ampliada en Retrospectiva Sprint 3)`
- **Alcance:** Packs gastronómicos, Happy Hour según huso de sucursal, cupones de descuento, y gestión integral de extras/complementos:
  - Modificadores con costo adicional (ej. "Doble carne", "Papas extras") y sin costo (ej. "Término medio").
  - Parametrización por empresa/sucursal: si el extra deduce insumos en Kardex o se trata como un concepto puramente comercial.
  - Modificaciones por exclusión (ej. "Sin cebolla"): configuración sobre si reingresa el insumo al stock o si se emite únicamente como nota visual en comanda para cocina.

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
- **Estado:** `En Progreso (Contenerización, Backups y CI/CD Completados en TASK-601/606/608; Provisión OCI y Smoke HTTPS movidos a TASK-609)`
- **Alcance:** Multi-Stage Dockerfiles, Twelve-Factor App, `docker-compose.staging.yml`, almacenamiento S3, aprovisionamiento VPS/OCI, SSL Let's Encrypt, CI/CD GitHub Actions y backups automatizados.

---

### PB-027 [AUTH] [SEGURIDAD]: Gestión del Ciclo de Vida de Sesiones de Usuario (JWT Expiration, Silent Refresh y Timeout Parametrizable)
- **Categoría:** Seguridad & Autenticación / Módulo Auth
- **Prioridad:** `P1 - Alta (Sprint 3)`
- **Estado:** `Completado (Fases 1 y 2 de Diseño en TASK-314 / Implementación en TASK-315)`
- **Especificación Formal:** Formalizado en [ADR-0011](../../adr/0011_session_lifecycle_and_silent_refresh.md), [use_cases_sessions.md](../architecture/03-use-cases/use_cases_sessions.md), [sequence_session_lifecycle_and_refresh.md](../architecture/04-behavior-and-flows/sequence_session_lifecycle_and_refresh.md), [database_erd.md](../architecture/02-domain-and-data/database_erd.md) y manual en `docs/business-processes/04-administracion-y-personal/04_flujo_ciclo_vida_sesiones_y_seguridad.md`.
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

### PB-028 [UI/UX] [ARQUITECTURA]: Diseño Responsivo Integral, Navegación Adaptativa Móvil y Ergonomía de Espacio
- **Categoría:** Frontend UI/UX / Shell y Vistas Operativas
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Breakpoints base completados en TASK-212, TASK-316 y TASK-317; Optimización ergonómica, menú contextual POS y colapso de sidebar completados en TASK-411)`
- **Contexto:** Si bien se implementaron los breakpoints móviles y el bottom-sheet en POS, la retrospectiva del Sprint 3 identificó que en pantallas medianas/grandes el menú no se puede colapsar (falta botón hamburguesa desktop), y en pantallas móviles hay sobrecarga de elementos (subtítulos innecesarios que roban espacio vertical útil y botones infrecuentes como abrir turno o registrar gasto ocupando espacio de venta).
- **Alcance Funcional y Técnico:**
  - Menú lateral colapsable con botón hamburguesa en pantallas grandes y medianas para ganar ancho útil de pantalla.
  - Reducción de sobrecarga visual en móviles: supresión de subtítulos estáticos en vistas de alta rotación (POS, Finanzas).
  - Menú contextual ("···" o acción flotante) para acciones de baja frecuencia operativa (abrir turno, registrar egreso) dejando el área principal libre para la comanda y el catálogo.
  - Tipografía monospace legible (`text-sm font-mono`) para montos numéricos en Inventario y Kardex.
- **Criterios de Aceptación:**
  - El sidebar se colapsa a modo compacto en escritorio para maximizar el área de trabajo.
  - La pantalla de POS en smartphone aprovecha al 100% el alto de la pantalla sin textos descriptivos superfluos.

---

### PB-029 [FINANCE] [NEGOCIO]: Visor y Control de Ventas del Turno Activo para Cajero en Módulo de Finanzas
- **Categoría:** Finanzas & POS / Control de Caja
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 4)`
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
- **Estado:** `En Backlog (Sprint 4)`
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
- **Estado:** `En Backlog (Sprint 4)`
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
- **Estado:** `En Backlog (Sprint 4)`
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
- **Estado:** `En Backlog (Sprint 5)`
- **Contexto:** Los modelos de cadena gastronómica varían: algunos restaurantes compran y reciben directamente en cada sucursal (modelo descentralizado actual), mientras que otros disponen de un centro de distribución o almacén central que recibe compras al por mayor y distribuye a las sucursales mediante transferencias internas.
- **Alcance Funcional y Técnico:**
  - Parametrizar a nivel de empresa (`PB-026`) el parámetro `INVENTORY_DISTRIBUTION_MODEL`:
    - `DIRECT_PER_BRANCH`: Cada sucursal gestiona sus propias compras y recepciones directamente.
    - `CENTRAL_WAREHOUSE_HUB`: Se designa una sucursal o sede física como "Almacén Central", canalizando las órdenes de compra maestras y restringiendo las recepciones de sucursales operativas a transferencias de stock internas (`PB-004`).
  - Adaptar permisos RBAC y validaciones en el módulo de compras para respetar el modelo configurado.
- **Criterios de Aceptación:**
  - Si una empresa activa el modelo centralizado, las sucursales satélite reciben insumos exclusivamente vía transferencia desde el almacén central.

---

### PB-034 [CATALOG/POS] [NEGOCIO]: Gestión de Menús Diarios, Plantillas Reutilizables y Selección en Turno con Cupos
- **Categoría:** Catálogo & Operación POS / Reglas de Negocio
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Reestructurado y Ampliado en Retrospectiva Sprint 3)`
- **Contexto:** En la retrospectiva del Sprint 3 se determinó la necesidad de separar conceptualmente el *Catálogo Maestro de Platos* de la *Gestión de Menús*. Un restaurante ofrece menús diarios diferenciados (ej. "Menú Ejecutivo Lunes", "Menú Fin de Semana", "Carta Nocturna") que agrupan un subconjunto de N platos del catálogo. El administrador necesita crear plantillas de menús con cantidades estimadas por defecto y, al momento de abrir el turno de caja, seleccionar qué menú operará la jornada, pudiendo modificar las cantidades sobre la marcha y decidir si guarda los cambios en la plantilla base o genera un nuevo menú derivado.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Diseño (Spec-First):**
    - Modelo de datos relacional: `menus` (nombre, descripción, tenant/branch), `menu_items` (`dish_id`, cantidad/cupo sugerido por defecto).
    - Plantillas de menú reutilizables y mutables.
    - Flujo de apertura de turno: selector de plantilla de menú, ajuste dinámico de cupos de platos y opción de guardar como nueva plantilla.
    - Algoritmo de verificación preventiva contra `branch_stock` (aviso informativo de insumos faltantes según receta BOM).
  - **Fase 2: Implementación Backend y Frontend:**
    - Módulo independiente de gestión de menús en `/catalog/menus` con creación rápida a partir de platos existentes.
    - Integración con el modal de apertura de turno (`cash_shifts`): asignación del menú activo a la sesión de caja.
    - En POS, filtrado automático de platos según el menú activo del turno y decremento reactivo de cupos con bloqueo de "Agotado" al llegar a cero.
- **Criterios de Aceptación:**
  - El usuario puede crear plantillas de menú con cantidades estimadas y seleccionarlas al abrir el turno.
  - Al abrir turno, el cajero puede ajustar las cantidades estimadas de platos sin afectar permanentemente la plantilla base, o bien optar por guardarlas como un nuevo menú.
  - En el POS solo se comercializan los platos activos en el menú de la sesión, bloqueando pedidos cuando se agote el cupo pactado.

---

### PB-035 [STORAGE] [ARQUITECTURA]: Motor Desacoplado de Almacenamiento Agnóstico (Local y S3/R2/OCI Object Storage)
- **Categoría:** Arquitectura Cloud & Media / Storage
- **Prioridad:** `P1 - Alta`
- **Estado:** `Sprint 6 (Planificado en TASK-603)`
- **Contexto:** El almacenamiento de fotos de platos, logos corporativos, comprobantes de gastos (`evidence_url`) y respaldo operativo no debe depender del sistema de archivos efímero ni vivir acoplado a `modules.catalog` (`LocalFileStorageService`). Dado que el frontend Angular ya realiza recorte y compresión WebP 4:3 en navegador (`TASK-506`), el backend debe enfocarse en desacoplar el motor bajo `core.storage` con validación estricta de seguridad y proveedores intercambiables (*stateless*).
- **Alcance Funcional y Técnico:**
  - Contrato de servicio agnóstico `MediaStorageService` en `core.storage` usando patrón **Strategy / Adapter** (`LocalStorageProvider`, `S3CompatibleStorageProvider` compatible con OCI Object Storage, Cloudflare R2, MinIO y AWS S3).
  - Validación estricta en backend (extensión, tipo MIME / *magic bytes*) sin añadir librerías Java redundantes de procesamiento de imágenes.
  - Parametrización dinámica `GLOBAL` gestionable desde Backoffice (`storage.active_provider`, `storage.max_file_size_bytes` con default `1 MB`, `storage.public_url_prefix`), manteniendo las credenciales sensibles (`AccessKey`, `SecretKey`) estrictamente en variables de entorno Twelve-Factor.
- **Criterios de Aceptación:**
  - El cambio de almacenamiento de disco local a S3/OCI/R2 se realiza limpiamente mediante configuración global + variables de entorno sin tocar lógica de `catalog` o `finance`.
  - El límite máximo de subida (ej. `1 MB`) es configurable en caliente desde el Backoffice.

---

### PB-037 [CATALOG] [NEGOCIO]: Modificaciones de Estructura de Plato en Pedido: Exclusión de Insumos BOM y Notas de Cocina
- **Categoría:** Catálogo, POS & Kardex / Recetas y Variaciones
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Backlog (Sprint 5)`
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

### PB-039 [AUTH] [SEGURIDAD]: Gestión Dinámica de Roles Personalizados, Privilegios Agrupados y Asignación de Usuarios
- **Categoría:** Seguridad & RBAC / Gestión Dinámica de Accesos
- **Prioridad:** `P1 - Alta (Sprint 3)`
- **Estado:** `Completado (Fases 1 y 2 de Diseño en TASK-336 / Implementación en TASK-337 y TASK-338)`
- **Especificación Formal:** Formalizado en [ADR-0010](../../adr/0010_granular_rbac_roles_and_privileges.md), [use_cases_rbac.md](../architecture/03-use-cases/use_cases_rbac.md), [sequence_rbac_resolution_and_enforcement.md](../architecture/04-behavior-and-flows/sequence_rbac_resolution_and_enforcement.md), [database_erd.md](../architecture/02-domain-and-data/database_erd.md) y manual en `docs/business-processes/04-administracion-y-personal/03_flujo_gestion_roles_y_privilegios.md`.
- **Contexto:** Actualmente los roles del sistema son estáticos (`ADMIN_TENANT`, `BRANCH_MANAGER`, `CASHIER`, `KITCHEN`). Cada restaurante u operador gastronómico necesita flexibilidad para crear roles a medida (ej. "Encargado de Compras", "Auditor Nocturno", "Jefe de Barra", "Subgerente"), asignando permisos atómicos agrupados modularmente, gestionando empleados rotativos multi-sucursal (`user_branches`), expiración temporal (`role_valid_until`) y reseteo jerárquico delegado de credenciales (`must_change_password`).
- **Alcance Funcional y Técnico:**
  - **Catálogo de Recursos y Privilegios:** Definición jerárquica de recursos (`pos`, `inventory`, `finance`, `catalog`, `users`, `settings`) con permisos atómicos agrupados en privilegios comprensibles para el usuario de negocio (ej. *"Operar Punto de Venta"*, *"Autorizar Gastos Menores"*, *"Ajustar Kardex y Mermas"*, *"Ver Reportes Financieros"*).
  - **Presets Semilla por Entorno:** Al darse de alta la empresa en Onboarding (`PB-001`), se inicializan roles predeterminados sugeridos según el tipo de negocio (Pizzería/Comida Rápida, Restaurante a la Carta, Bar/Cafetería).
  - **Gestión de Roles Personalizados por Tenant:** CRUD de roles propios de la empresa (`custom_roles`, `role_permissions` en PostgreSQL), permitiendo al Administrador marcar privilegios mediante switches o checkboxes en la UI.
  - **Resolución Dinámica en Seguridad:** Integración en `UserPublicApi` para mapear los roles y permisos calculados al token JWT y a los Route Guards / Directivas de Angular.
  - **Vistas en Frontend:** Centro de Gestión de Roles y Permisos en `/users/roles` con asignación interactiva a colaboradores.
- **Criterios de Aceptación:**
  - El administrador puede crear un rol con privilegios específicos, asignarlo a un usuario y verificar que los permisos se reflejen de inmediato en su sesión sin reinicio de la plataforma.
  - No se permite eliminar ni despojar de permisos al rol nativo `ADMIN_TENANT` para evitar bloqueos del sistema.

---

### PB-042 [UI/UX] [ARQUITECTURA]: Design System de Restaurante, Suite de Componentes Standalone y Estandarización Visual de Layouts
- **Categoría:** Frontend UI/UX / Design System & Arquitectura de Componentes
- **Prioridad:** `P1 - Alta (Requisito Estructural para Escalar el Frontend)`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-UI-01..05)`
- **Contexto:** En la retrospectiva del Sprint 3 se constató una grave falta de estandarización visual: botones de aceptar y cancelar con el mismo color verde, badges poco contrastados que causan cansancio visual, márgenes pegados al límite de la pantalla en POS e Inventario, y duplicación masiva de código HTML en lugar de reutilizar componentes web de Angular. Se requiere construir un Design System con componentes base reutilizables.
- **Alcance Funcional y Técnico:**
  - **Fase 1: Especificación y Tokens del Design System:**
    - Formalización de tokens de color semánticos en Tailwind: `primary` (acción principal), `secondary` (acciones neutras), `destructive/danger` (cancelaciones, eliminaciones), `warning`, `success`.
    - Contraste estricto para Modo Claro y Modo Oscuro cumpliendo normas WCAG AA de legibilidad.
    - Componente Layout Wrapper (`<app-page-wrapper>`): estandarización de paddings, títulos de página, breadcrumbs y márgenes consistentes en todas las vistas.
  - **Fase 2: Suite de Componentes Standalone Reutilizables:**
    - `<app-button>`: soporte para variantes (`primary`, `secondary`, `outline`, `danger`, `ghost`), tamaños y estado de carga (`loading`).
    - `<app-badge>`: variantes semánticas con contraste garantizado y tamaño tipográfico armónico.
    - `<app-card>` y `<app-dish-card>`: tarjeta estandarizada para platos y elementos de catálogo.
    - `<app-modal>`: contenedor accesible con backdrop, bloqueo de scroll y z-index controlado.
    - Corrección del conflicto de `z-index` en el selector de sucursal del header.
- **Criterios de Aceptación:**
  - Todas las vistas del sistema utilizan el contenedor `<app-page-wrapper>` con márgenes homogéneos.
  - No existen botones de cancelación del mismo color que botones de confirmación.
  - El selector de sucursal se despliega siempre por encima de cualquier elemento de contenido.

---

### PB-043 [UI/UX] [ARQUITECTURA]: Motor Unificado de Notificaciones (Toasts con Z-Index Seguro y Alertas en Línea)
- **Categoría:** Frontend UI/UX / Feedback al Usuario & Infraestructura
- **Prioridad:** `P1 - Alta`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-UI-08)`
- **Contexto:** Actualmente los mensajes de toast aparecen por detrás del backdrop de los modales debido a problemas de apilamiento CSS (*Stacking Contexts*). Además, los formularios no señalan claramente los errores en los campos de entrada, dificultando al usuario comprender por qué una acción fue rechazada.
- **Alcance Funcional y Técnico:**
  - **Servicio Global `NotificationService`:**
    - Arquitectura basada en Angular CDK Overlay o Portales con renderizado en `document.body` y `z-[9999]`, garantizando visibilidad absoluta sobre modales y drawers.
    - Niveles de notificación tipados: `SUCCESS`, `ERROR`, `WARNING`, `INFO`.
    - Configuración de posición (ej. superior derecha), duración y botón de cierre manual.
  - **Validación en Línea para Signal Forms:**
    - Directiva o componente de campo de formulario que resalta bordes en rojo y muestra texto de ayuda accesible debajo del input.
- **Criterios de Aceptación:**
  - Al fallar una validación dentro de un modal, la notificación es 100% visible por encima del modal.
  - Los campos de texto requeridos se marcan visualmente en rojo al interactuar sin llenarlos.

---

### PB-044 [CATALOG] [NEGOCIO]: Modelo Flexible de Recetas e Inventario de Platos (BOM Estricto, Plato como Insumo Unitario o Venta Libre)
- **Categoría:** Catálogo & Kardex / Reglas de Negocio Configurables
- **Prioridad:** `P1 - Alta`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-CAT-02)`
- **Contexto:** Actualmente el sistema asume que todo plato requiere una receta de insumos en Kardex. Sin embargo, no todos los restaurantes operan con control estricto de ingredientes gramo a gramo: algunos compran o preparan platos terminados (ej. 20 empanadas) y quieren controlar stock por unidad de plato; otros simplemente venden bebidas o platos comerciales sin deducir inventario alguno.
- **Alcance Funcional y Técnico:**
  - **Estrategias de Inventario por Plato (`dish_inventory_mode`):**
    1. `RECIPE_BOM_STRICT`: Deducción atómica de insumos según ingredientes (modelo actual).
    2. `DISH_AS_INVENTORY_UNIT`: El plato se auto-representa como su propio insumo en almacén (control de stock por unidades de plato terminado).
    3. `COMMERCIAL_NO_INVENTORY`: Venta libre en POS sin deducción ni verificación de Kardex.
  - **Parametrización Multinivel (`PB-026`):**
    - Parámetro a nivel empresa/sucursal `DEFAULT_DISH_INVENTORY_STRATEGY` con opción de sobreescritura individual por plato.
- **Criterios de Aceptación:**
  - Un restaurante puede operar platos en modo libre sin que el POS exija stock ni falle por falta de insumos.
  - Los platos en modo unidad descuentan existencias de sí mismos directamente en el stock de la sucursal.

---

### PB-046 [QA] [CALIDAD]: Batería E2E Crítica de Negocio (Ciclo Integral POS + Turno + Gastos + Arqueo) y Gobernanza de Testing
- **Categoría:** Calidad & Testing / Regresión Transaccional
- **Prioridad:** `P1 - Alta`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 3 - OBS-TST-01)`
- **Contexto:** Las pruebas E2E actuales se dividieron en scripts por componentes aislados, permitiendo que anomalías críticas como la venta sin turno pasaran desapercibidas. Se requiere una batería de prueba de ciclo cerrado continuo que simule la jornada real de un restaurante.
- **Alcance Funcional y Técnico:**
  - **Suite E2E de Jornada Completa (Clean-to-Tail):**
    1. Intento de venta con turno cerrado $\rightarrow$ Verificación de bloqueo estricto en UI y backend (HTTP 409).
    2. Apertura formal de turno con fondo inicial ($Bs.\ 200.00$).
    3. Selección de menú diario y verificación de cupos pactados.
    4. Cobro de 3 ventas con distintas combinaciones (efectivo con cambio, tarjeta, extras con costo).
    5. Solicitud de gasto menor de emergencia por cajero ($Bs.\ 35.00$) $\rightarrow$ Estado `PENDING_APPROVAL`.
    6. Cambio de sesión a Administrador $\rightarrow$ Aprobación del gasto menor.
    7. Retorno a Cajero $\rightarrow$ Conteo físico de billetes en modal de arqueo $\rightarrow$ Cuadre matemático exacto ($Diferencia = 0.00$).
    8. Cierre de turno y auditoría final.
  - **Gobernanza:** Integración en el checklist de pre-release (`release/sprint-X`) antes de mergear a `main`.
- **Criterios de Aceptación:**
  - La suite se ejecuta de forma 100% determinista vía script persistido sin fallos aleatorios.

---

### PB-047 [DEVOPS/DB] [CALIDAD]: Scripts de Reset, Estandarización de Semillas y Aprovisionamiento Limpio de Base de Datos Dev
- **Categoría:** Infraestructura & Base de Datos / Calidad Operativa
- **Prioridad:** `P1 - Alta`
- **Estado:** `En Progreso (Sprint 4)`
- **Contexto:** Durante el testing exploratorio y retrospectiva del Sprint 4, remanentes de bases de datos sucias y seeds incompletas causaban fallas operativas recurrentes (caja cerrada, turnos de menús diarios inexistentes, stock en 0 e insumos insuficientes, usuarios cajeros sin asignación explícita de sucursales).
- **Alcance Funcional y Técnico:**
  - **Scripts de Limpieza y Recreación:**
    - `scripts/db/reset_database.sql`: Truncamiento en cascada de transacciones operativas (`orders`, `order_items`, `shift_menu_*`, `expenses`, `cash_shifts`, `kardex_movements`, `branch_stock`, `event_publication*`, etc.) preservando parametría y catálogos.
    - `scripts/db/reset_database.sh`: Script ejecutable con soporte `--purge` (purga transaccional) y `--full-recreate` (recreación DDL desde cero con Flyway).
  - **Estandarización de Semillas (Seeds V1-V10):**
    - `DataInitializer`: Asignación explícita de sucursales en `user_branches` para administradores y cajeros en los 3 tenants demo.
    - `CatalogDataInitializer`: Generación de plantillas de `DailyMenu` (modo `FIXED_PACKAGE`) con tiempos `STARTER`, `MAIN`, `BEVERAGE` y `DESSERT`.
    - `InventoryDataInitializer`: Aprovisionamiento de stock amplio ($\ge 50$ unidades) para todas las sucursales creadas.
    - `FinanceDataInitializer`: Apertura automática de turnos de caja iniciales (`OPEN`) con fondo operativo ($Bs.\ 200.00-300.00$) para todas las sucursales.
  - **Aprovisionamiento de Instancia Limpia `restaurant_dev`:**
    - Base de datos independiente en contenedor PostgreSQL para aislar pruebas manuales y desarrollo activo.
- **Criterios de Aceptación:**
  - Ejecución idempotente de los scripts sin violaciones de clave foránea.
  - Arranque limpio del backend con Flyway V1-V10 y seeds completos con 0 advertencias o inconsistencias.

---

### PB-049 [ONBOARDING] [UX]: Rediseño Arquitectónico y UX de Cero del Tutorial y Tour Guiado Interactivo
- **Categoría:** Experiencia de Usuario & Onboarding / Gamificación Operativa
- **Prioridad:** `P1 - Alta`
- **Estado:** `Pendiente (Identificado en Retrospectiva Sprint 4)`
- **Contexto:** El tour guiado interactivo actual presenta pasos rígidos y elementos superpuestos que no se adaptan óptimamente a dispositivos móviles o flujos no lineales. Se requiere rediseñar la experiencia desde cero, orientándola a micro-metas de negocio del restaurante.
- **Alcance Funcional y Técnico:**
  - Rediseño conceptual de la narrativa del tour: enfoque práctico ("Tu primera venta en 3 pasos", "Configuración express de tu menú").
  - Arquitectura desacoplada en `onboarding-tour.service.ts` con soporte para pausas, reanudaciones y disparadores contextuales.
  - Componente modal/tooltip emergente adaptativo con alto contraste y soporte móvil táctil.
- **Criterios de Aceptación:**
  - El usuario puede iniciar, pausar y retomar el tour en cualquier momento desde el topbar.
  - El tour funciona con el 100% de fluidez tanto en desktop como en pantallas móviles sin tapar botones clave.

---

### PB-057 [DEVOPS/STARTUP] [ARQUITECTURA]: Arranque Limpio Go-Live (Baseline System Startup sin Tenancies Demo)
- **Categoría:** Inicialización de Sistema & Seguridad Supra-Tenant
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 / TASK-602)`
- **Contexto:** Actualmente los 4 `CommandLineRunner` (`DataInitializer`, `CatalogDataInitializer`, `FinanceDataInitializer`, `InventoryDataInitializer`) siembran 3 restaurantes de demostración incondicionalmente al arrancar. Además, `app_users.tenant_id` exige `NOT NULL`, impidiendo contar con un `SUPERADMIN` supra-tenant desacoplado de restaurantes clientes en producción.
- **Alcance Funcional y Técnico:**
  - Aislar los seeders demo bajo `@Profile({"dev", "demo"})` para garantizar cero contaminación en Staging/Producción.
  - Desacoplar el modelo de identidad de plataforma vs tenant (resolver `app_users.tenant_id` o tabla/esquema de administradores globales) y crear un bootstrap limpio para Go-Live (roles base, catálogo de parámetros, cuenta inicial `SUPERADMIN` por variables de entorno).
- **Criterios de Aceptación:**
  - Al levantar el sistema en perfil `staging` o `prod`, nacen 0 restaurantes demo y solo existe el contexto inicial `SUPERADMIN` listo para operar desde el Backoffice o Onboarding público.

---

### PB-058 [DEVSECOPS] [SEGURIDAD]: Auditoría Local DevSecOps (Secretos, Vulnerabilidades SAST/SCA y Escaneo de Contenedores)
- **Categoría:** DevSecOps / Seguridad Continua & Higiene de Código
- **Prioridad:** `P1 - Alta`
- **Estado:** `Completado (Sprint 6 / TASK-607)`
- **Contexto:** Antes de exponer contenedores y código hacia entornos cloud (Staging/Producción), es obligatorio validar localmente que no existan secretos filtrados en el repositorio, dependencias vulnerables en Maven/npm, fallas de seguridad SAST (inyecciones, configuraciones inseguras) ni vulnerabilidades críticas en las imágenes Docker construidas.
- **Alcance Funcional y Técnico:**
  - Evaluación e implementación de suite de herramientas locales gratuitas y ligeras (ej. `gitleaks` para detección de secretos, `trivy` para SCA de dependencias + Dockerfiles + imágenes Docker, `npm audit` y SAST como `Semgrep` / `SpotBugs`).
  - Script unificado de auditoría local (`scripts/security/run-security-scan.sh`) y recomendación de hooks pre-commit para prevenir filtración accidental de credenciales antes del `git push`.
- **Criterios de Aceptación:**
  - Ejecución local reproducible con reporte limpio: 0 secretos expuestos en historial/working tree y 0 vulnerabilidades `CRITICAL`/`HIGH` sin mitigar antes de publicar imágenes a la nube.
