---
title: "Sprint 5 Backlog — Servicio Avanzado y Candidato a Producción (Sprint Activo)"
category: "tasks"
sprint: 5
status: "active"
tags:
  - tasks
  - sprint-backlog
  - sprint-5
  - execution-plan
  - agents
last_updated: "2026-11-07"
---

# 🚀 Sprint 5 Backlog — Servicio Avanzado y Candidato a Producción (Sprint Activo v0.5.0 / v1.0.0-RC)

> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./project/sprints/README.md) | [Product Backlog (Por Prioridades)](./project/product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1 (Cerrado)](./project/sprints/sprint_01_backlog.md) | [Sprint 2 (Cerrado)](./project/sprints/sprint_02_backlog.md) | [Sprint 3 (Cerrado v0.3.0)](./project/sprints/sprint_03_backlog.md) | [Sprint 4 (Cerrado)](./project/sprints/sprint_04_backlog.md)  
> **Sprints Futuros:** [Sprint 6 (Infraestructura Cloud y SaaS Backoffice)](./project/sprints/sprint_06_planning.md)

---

## 📋 Reglas de Ejecución y Protocolo de Calidad (DoD)

El desarrollo se ejecuta de manera incremental y atómica siguiendo el ciclo **SDD-SDLC de 4 fases** (ADR-0007 y Protocolo Multi-Agente).
Antes de dar por concluida cualquier tarea:
- Backend: `./mvnw clean test` (100% pruebas unitarias e integración pasando sin fallos).
- Frontend: `npm run lint && npm test` (100% pruebas unitarias pasando en Vitest/JSDOM con 0 advertencias).
- SDD: Sincronización obligatoria de diagramas Mermaid si se alteran APIs, base de datos o flujos.
- Multi-Agente: Sincronización autónoma inmediata a cargo de `doc-architect` sin recordatorios manuales.

> [!IMPORTANT]
> **Secuencia de Arranque del Sprint 5:**
> 1. **Fase Personalización y Modificadores:** Consolidar `PB-020` (TASK-501 & TASK-502) en backend y frontend para permitir comandas flexibles, extras con/sin costo y deducción selectiva de inventario en Kardex.
> 2. **Fase Cancelaciones y Mermas:** Formalizar e implementar `PB-031` (TASK-503 & TASK-504) con máquina de estados de orden, autorización con PIN y tratamiento de mermas vs reintegro a stock.
> 3. **Fase Modalidades de Despacho:** Implementar `PB-032` (TASK-505) con modalidades `DINE_IN`, `TAKEOUT`, `DELIVERY` y rótulos en tickets.
> 4. **Fase Catálogo Visual y Ergonomía:** Implementar `PB-036` (TASK-506) para imágenes en POS con toggle y `PB-048` (TASK-507) para SKU secuencial y emojis en categorías.
> 5. **Fase Auditoría Gerencial y Usabilidad:** Implementar `PB-030` (TASK-508) histórico de turnos con filtros, `PB-045` (TASK-509) perfil autoservicio y `PB-047` (TASK-510) humanización de parametría.
> 6. **Fase Certificación Final:** Ejecución de batería E2E completa de jornada y regresión con `TASK-511` (DoD Producción).

---

## 🎯 Tareas del Sprint 5 (En Curso)

### 1. Modificadores, Complementos y Personalización de Comandas (PB-020)

- [x] **TASK-501 [POS] [NEGOCIO]:** Suite de Modificadores, Complementos y Extras con Configuración de Costo y Kardex (PB-020).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Extender la API y lógica de modificadores para permitir:
    1. Modificadores con costo adicional y sin costo (término de cocción, tipo de salsa).
    2. Modificadores por exclusión ("Sin cebolla") con indicación explícita para comanda.
    3. Parametrización por tenant/sucursal: si el extra deduce insumos en Kardex o se trata como concepto comercial.
    4. Trazabilidad exacta en el detalle de la comanda y persistencia en `order_item_modifiers`.
  - *Criterio de Entrega:* 100% de tests en Spring Modulith pasando; eventos de venta reflejan deducción condicional de insumos.

- [x] **TASK-502 [POS] [UI/UX]:** Selector Interactivo de Modificadores en POS y Notas de Cocina en Comanda (PB-020).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Integrar en la cuadrícula de POS la experiencia fluida para:
    1. Al seleccionar un plato con grupos de modificadores, desplegar el modal interactivo de opciones con validaciones de mínimo/máximo (`minSelectable` / `maxSelectable`).
    2. Campo de notas especiales de cocina ("bien cocido", "alérgico al maní") por ítem de la comanda.
    3. Resumen visual claro en el carrito lateral y en el visor de pedidos (`/cash-control` y comanda impresa).
    4. Soporte offline-first en Dexie.js para almacenar y sincronizar modificadores seleccionados.
  - *Criterio de Entrega:* Selector reactivo sin bloqueos en UI; 100% de tests unitarios en Vitest pasando.

---

### 2. Cancelaciones, Devoluciones y Tratamiento de Mermas (PB-031)

- [ ] **TASK-503 [POS] [DISEÑO]:** Máquina de Estados para Cancelaciones, Autorización con PIN y Reversión a Kardex vs Merma (PB-031 / ADR).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar y formalizar la máquina de estados de la orden con estado `CANCELLED`:
    1. Motivos tipados de cancelación (desistimiento de cliente, error de digitación, plato en mal estado).
    2. Política de inventario: si no se elaboró $\rightarrow$ reintegro a stock (`CANCELLATION_REVERSAL`); si ya se elaboró $\rightarrow$ merma/desperdicio (`WASTE_ADJUSTMENT`).
    3. Autorización requerida (PIN de supervisor/administrador).
    4. Impacto financiero en el turno de caja: egreso por devolución (`REFUND`) para mantener cuadre exacto de efectivo.
  - *Criterio de Entrega:* ADR formal aceptado, diagramas de máquina de estados, secuencia y especificaciones SDD sincronizadas.

- [ ] **TASK-504 [POS] [NEGOCIO]:** Implementación de Reintegro de Efectivo en Gaveta y Registro de Merma por Cancelación (PB-031).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints en backend y diálogo en frontend para ejecutar cancelaciones con autorización por PIN:
    1. Verificación de credencial/PIN de supervisor vía `SecurityPublicApi`.
    2. Publicación de `OrderCancelledEvent` para actualización atómica en Kardex y Finanzas.
    3. Registro de movimiento de egreso en `cash_shifts` si la orden fue cobrada en efectivo.
    4. Modal en POS / Control de Caja con selector de motivo y confirmación visual.
  - *Criterio de Entrega:* Pruebas de integración en Spring Modulith y tests unitarios en Vitest al 100%.

---

### 3. Modalidades Para Llevar (Takeout) y Delivery (PB-032)

- [ ] **TASK-505 [POS] [NEGOCIO]:** Modalidades de Servicio (`DINE_IN`, `TAKEOUT`, `DELIVERY`) y Distintivos en Tickets (PB-032).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Incorporar modalidades operativas de servicio:
    1. Selector de modalidad en cabecera de orden en POS: Local (`DINE_IN`), Para Llevar (`TAKEOUT`), Delivery (`DELIVERY`).
    2. Campos opcionales contextuales: nombre del cliente, teléfono y notas de despacho/empaque.
    3. Rótulo destacado en comanda impresa y visor en vivo de cocina.
    4. Filtros por modalidad en `/cash-control` y reportería.
  - *Criterio de Entrega:* Transmisión y persistencia de modalidad en backend y frontend con 100% de tests passing.

---

### 4. Catálogo Visual con Imágenes y Codificación Secuencial (PB-036 / PB-048)

- [ ] **TASK-506 [CATALOG] [UI/UX]:** Visualización de Imágenes de Platos en Cuadrícula POS con Toggle Parametrizable (PB-036).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Carga y despliegue opcional de imágenes en la cuadrícula de productos del POS:
    1. Renderizado de tarjeta con foto optimizada y lazy loading; fallback a icono/emoji si no hay imagen.
    2. Toggle rápido en barra de herramientas del POS: modo tarjetas visuales vs modo compacto de botones.
    3. Persistencia de preferencia de visualización local por cajero/dispositivo.
  - *Criterio de Entrega:* 100% fluidez de scroll en desktop y móviles; tests unitarios en Vitest.

- [ ] **TASK-507 [CATALOG] [UI/UX]:** Asistente de SKU Secuencial (`BEB-001`) y Selector de Emojis para Categorías (PB-048).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Facilitar el alta de platos y categorías en el catálogo:
    1. Sugerencia automática del siguiente correlativo disponible al elegir categoría (ej. `HAM-001`, `HAM-002`).
    2. Selector interactivo de emojis y símbolos gastronómicos para categorías (🍕, 🍔, 🥗, ☕, 🍺, 🍰, 🥩).
    3. Cálculo automático del orden visual (`displayOrder`) para evitar colisiones.
  - *Criterio de Entrega:* Modales de creación de categorías y platos enriquecidos; 100% tests en Vitest.

---

### 5. Auditoría Gerencial y Autoservicio de Perfil (PB-030 / PB-045 / PB-047)

- [ ] **TASK-508 [FINANCE] [BI]:** Histórico Gerencial de Turnos con Filtros por Fechas, Sucursal y Exportación (PB-030).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Vista de auditoría histórica en `/finance`:
    1. Tabla de turnos cerrados con filtros por rango de fechas (según huso local de sucursal ADR-0006), cajero, sucursal y resultado de arqueo (cuadrado, sobrante, faltante).
    2. Modal de inspección detallada de turno con desglose de ventas, egresos y movimientos de gaveta.
    3. Exportación de resumen para archivo contable.
  - *Criterio de Entrega:* Consultas optimizadas con paginación en backend; vista interactiva con UI Kit en frontend.

- [ ] **TASK-509 [AUTH] [UI/UX]:** Perfil de Usuario Autoservicio, Despliegue de Nombres Reales y Limpieza de Privilegios (PB-045).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Humanizar y dar autoservicio al perfil de usuario:
    1. Despliegue de `user.fullName` y nombre legible del rol en header y topbar en vez de roles técnicos crudos.
    2. Vista "Mi Perfil" para actualizar nombre, teléfono y cambio seguro de contraseña propia.
    3. Reemplazo de lista cruda de permisos técnicos por tarjeta ejecutiva de roles y sucursales autorizadas.
  - *Criterio de Entrega:* Formularios reactivos con validación inline; 100% tests en Vitest.

- [ ] **TASK-510 [CONFIG] [UI/UX]:** Humanización de Tarjetas de Parámetros y Opciones en Español (PB-047).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Rediseño visual de la consola de parámetros (`/settings/parameters`):
    1. Encabezados claros en lenguaje natural en español con la clave técnica secundaria en badge discreto.
    2. Opciones de listas desplegables con etiquetas descriptivas en lugar de enums técnicos crudos en inglés.
    3. Tooltips de ayuda y ejemplos prácticos para parámetros complejos.
  - *Criterio de Entrega:* Consola comprensible para cualquier administrador de restaurante; 0 warnings de linting.

---

### 6. Certificación Final Pre-Producción (DoD)

- [ ] **TASK-511 [QA] [CALIDAD]:** Certificación E2E Integral de Todos los Flujos de Negocio de Restaurante (DoD Producción).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Batería completa de regresión y certificación Playwright:
    1. Flujo completo: Login $\rightarrow$ Turno $\rightarrow$ Venta con Modificadores $\rightarrow$ Modalidad Takeout $\rightarrow$ Cancelación con PIN $\rightarrow$ Gasto Menor $\rightarrow$ Arqueo Físico $\rightarrow$ Auditoría Gerencial.
    2. Validación offline en Dexie y re-sincronización idempotente.
    3. Reporte final formal en `docs/testing/reporte_ejecucion_sprint5.md`.
  - *Criterio de Entrega:* 100% pruebas E2E pasando de forma determinista sin fallos ni timeouts rígidos.
