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

- [x] **TASK-503 [POS] [DISEÑO]:** Máquina de Estados para Cancelaciones, Autorización con PIN y Reversión a Kardex vs Merma (PB-031 / ADR-0019).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar y formalizar la máquina de estados de la orden con estado `CANCELLED`:
    1. Motivos tipados de cancelación (desistimiento de cliente, error de digitación, plato en mal estado).
    2. Política de inventario: si no se elaboró $\rightarrow$ reintegro a stock (`CANCELLATION_REVERSAL`); si ya se elaboró $\rightarrow$ terna parametrizada en `pos.cancellation_prepared_waste_policy` (`ALLOW_AUTO_BUFFER_REUSE`, `ALLOW_BUFFER_REUSE`, `STRICT_WASTE`).
    3. Motor de autorizaciones generalizado de turno con canal dual (PIN in-situ de 4-6 dígitos y cola remota en `/finance`).
    4. Impacto financiero en el turno de caja: egreso por devolución (`REFUND`) para mantener cuadre exacto de efectivo en gaveta física vs devolución digital.
    5. Componente presentacional puro en frontend `<app-authorization-queue>` para prevenir God Components.
  - *Criterio de Entrega:* Documentos certificados: [ADR-0019](./adr/0019_order_cancellation_workflow_authorizations_and_waste_treatment.md), diagramas [sequence_pos_order_cancellation_and_authorizations.md](./project/architecture/04-behavior-and-flows/sequence_pos_order_cancellation_and_authorizations.md) y [state_order_cancellation_and_waste.md](./project/architecture/04-behavior-and-flows/state_order_cancellation_and_waste.md), y actualización SDD backend/frontend. Finalizado el 07/11/2026.

- [x] **TASK-504 [POS] [NEGOCIO]:** Implementación de Reintegro de Efectivo en Gaveta y Registro de Merma por Cancelación (PB-031).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints en backend y diálogo en frontend para ejecutar cancelaciones con autorización por PIN:
    1. Verificación de credencial/PIN de supervisor vía `SecurityPublicApi` / `UserPublicApi.validateSupervisorPin`.
    2. Publicación de `OrderCancelledEvent` para actualización atómica en Kardex (`CANCELLATION_REVERSAL`, `ALLOW_AUTO_BUFFER_REUSE`, `WASTE_ADJUSTMENT`) y Finanzas.
    3. Registro de movimiento de egreso en `cash_shifts` si la orden fue cobrada en efectivo (`total_cash_refunds`).
    4. Componente presentacional puro `<app-authorization-queue>` y modales interactivos en `/cash-control` y `/finance`.
  - *Criterio de Entrega:* Pruebas de integración en Spring Modulith y tests unitarios en Vitest al 100% pasando. Finalizado el 08/11/2026.

---

### 3. Modalidades Para Llevar (Takeout) y Delivery (PB-032)

- [x] **TASK-505 [POS] [NEGOCIO]:** Modalidades de Servicio (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`), Selector Dual y Distintivos en Tickets (PB-032 / ADR-0020).
  - *Prioridad:* `P1 - Alta`
  - *Estado:* `Completada e Integrada (Fase 3 Construcción & Fase 4 Sincronización)`
  - *Objetivo:* Incorporar modalidades operativas de servicio con selector dual:
    1. Selector dual de modalidad: barra superior de POS (`PosHeader`) para preselección rápida y selector contextual en modal de Checkout (`PosCheckoutModal`).
    2. Modalidades soportadas: En Salón (`DINE_IN`), Para Llevar (`TAKEOUT`), Encargo para Recoger (`SCHEDULED_PICKUP`) y A Domicilio (`DELIVERY`).
    3. Campos contextuales y costo de envío: teléfono, dirección (obligatoria en delivery), costo de envío (`delivery_fee` parametrizable con `pos.delivery_fee_enabled`) y notas de entrega/recogida.
    4. Rótulo y banner destacado en tickets impresos y modal de comanda (`*** PARA LLEVAR ***`, `*** ENCARGO PARA RECOGER ***`, `*** DELIVERY / A DOMICILIO ***` y `CONSUMO EN SALÓN`).
    5. Filtros por modalidad y badges distintivos en el visor de ventas del turno (`/cash-control`).
  - *Criterio de Entrega:* Migración Flyway V13, persistencia en backend/frontend, cálculo de total con deliveryFee, 100% de tests passing en Maven (156/156) y Vitest (319/319). Certificado el diseño en [ADR-0020](./adr/0020_service_modalities_takeout_delivery_and_ticket_badges.md) y diagrama [sequence_pos_service_modalities_and_delivery.md](./project/architecture/04-behavior-and-flows/sequence_pos_service_modalities_and_delivery.md). Finalizado el 08/11/2026.


---

### 4. Catálogo Visual con Imágenes y Codificación Secuencial (PB-036 / PB-048)

- [x] **TASK-506 [CATALOG] [UI/UX]:** Visualización de Imágenes de Platos en Cuadrícula POS con Toggle Parametrizable (PB-036 / ADR-0021).
  - *Prioridad:* `P2 - Media`
  - *Estado:* `Completado (100% Tests Backend + Frontend Verificados)`
  - *Objetivo:* Carga, encuadre nativo y despliegue opcional de imágenes en la cuadrícula de productos del POS:
    1. Modelo de datos y almacenamiento desacoplado: columna `image_url` en `dishes` (Flyway `V14`), servicio `LocalFileStorageService` con configuración global en `application.yml` y endpoint `POST /api/v1/catalog/dishes/{id}/image`.
    2. Componente nativo de encuadre y compresión: `app-image-cropper-modal` en Angular 22 Zoneless con máscara fija 4:3 (400x300 px), pan/zoom y exportación nativa en HTML5 Canvas a WebP al 80% (~25-35 KB, sin librerías npm externas).
    3. Fallback temático: contenedor de altura fija con degradado suave y emoji de categoría si el plato no tiene foto.
    4. Selector triple de vistas en POS: `COMPLETO` (Galería visual), `COMPACTO` (Botones de alta densidad) y `LISTA` (Renglones), inicializado por parámetro `pos.display_dish_images` y persistido localmente en `localStorage`.
    5. Ergonomía de herramientas y diseño móvil: barra desktop unificada `[Buscador | Selector Modalidad | Selector Triple]`, ocultamiento de título en móvil, icono de Mesa (`🪑`), y 4 botones de pago en una sola fila en checkout.
    6. Persistencia offline: `imageUrl` en Dexie.js (`LocalProduct`) para sinergia con el Disk Cache del navegador.
  - *Criterio de Entrega:* Migración Flyway V14, endpoint y servicio de almacenamiento probados, componente de recorte y selector triple testeados al 100% en Vitest y Maven. Certificado en [ADR-0021](./adr/0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md) y diagrama [sequence_pos_dish_image_upload_and_display.md](./project/architecture/04-behavior-and-flows/sequence_pos_dish_image_upload_and_display.md).


- [ ] **TASK-507 [CATALOG] [UI/UX]:** Asistente de SKU Secuencial (`BEB-1`), Selector de Emojis Gastronómicos y Ordenamiento en Catálogo (PB-048 / ADR-0022).
  - *Prioridad:* `P2 - Media`
  - *Estado:* `Especificación SDD Completada (ADR-0022) - Listo para Implementación`
  - *Objetivo:* Facilitar el alta asistida de platos y categorías en el catálogo:
    1. Modelo relacional y prefijos: columna `code_prefix VARCHAR(10) NOT NULL` con restricción de unicidad por tenant en `categories` (Flyway `V15`).
    2. Endpoint backend de correlativos: `GET /api/v1/catalog/dishes/next-code?categoryId={id}` con política estricta `MAX + 1` en formato simple `${prefix}-${n}` (ej. `BEB-1`, `HAM-2`), sin rellenar huecos.
    3. UX de alta de plato: autocompletado reactivo de SKU al elegir categoría, respeto estricto a códigos manuales o EAN-13 ingresados por el usuario y botón `[🪄 Sugerir Código]`.
    4. Selector de emojis nativo: componente popover `app-emoji-picker` ($< 2\text{ KB}$, 0 dependencias npm) con ~40 emojis gastronómicos curados y búsqueda libre.
    5. Precálculo de orden visual: `sortOrder = max(sortOrders) + 1` al crear categoría.
    6. Renderizado en POS: emoji en pestañas de categoría y como fallback central en tarjetas sin foto (`TASK-506`).
  - *Criterio de Entrega:* Migración Flyway V15, endpoint de correlativos con tests unitarios y de integración, selector de emojis y asistente testeados al 100% en Vitest y Maven. Certificado en [ADR-0022](./adr/0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md) y diagrama [sequence_catalog_sku_assistant_and_emoji_picker.md](./project/architecture/04-behavior-and-flows/sequence_catalog_sku_assistant_and_emoji_picker.md).


---

### 5. Auditoría Gerencial y Autoservicio de Perfil (PB-030 / PB-045 / PB-047)

- [ ] **TASK-508 [FINANCE] [BI]:** Histórico Gerencial de Turnos con Filtros por Fechas, Sucursal y Exportación (PB-030 / ADR-0023).
  - *Prioridad:* `P1 - Alta`
  - *Estado:* `Especificación SDD Completada (ADR-0023) - Listo para Implementación`
  - *Objetivo:* Vista de auditoría histórica y BI en `/finance`:
    1. Endpoint multicriterio en backend: `GET /api/v1/finance/shifts/history` con filtros por rango de fechas (convertido al huso de sucursal ADR-0006), sucursal, cajero, estado y tipo de descuadre (`CUADRADO`, `SOBRANTE`, `FALTANTE`).
    2. Endpoint drill-down: `GET /api/v1/finance/shifts/{id}/audit-detail` con desglose completo de órdenes por medio de pago, gastos y cancelaciones con reintegro.
    3. Modal de inspección en profundidad: `<app-shift-detail-modal>` con pestañas de balance, órdenes y egresos.
    4. Exportación nativa CSV compatible con Excel: utilidad `exportToCsv` ($< 3\text{ KB}$, con BOM UTF-8) para lista consolidada y detalle individual.
  - *Criterio de Entrega:* Consultas indexadas en Spring Modulith, visor de turnos y exportador testeados al 100% en Vitest y Maven. Certificado en [ADR-0023](./adr/0023_shift_history_multicriteria_audit_and_csv_export.md) y diagrama [sequence_finance_shift_history_audit_and_export.md](./project/architecture/04-behavior-and-flows/sequence_finance_shift_history_audit_and_export.md).


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
