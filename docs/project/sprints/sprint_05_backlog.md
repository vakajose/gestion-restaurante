---
title: "Sprint 5 Backlog — Servicio Avanzado y Candidato a Producción (Cerrado)"
category: "sprints"
sprint: 5
status: "closed"
tags:
  - tasks
  - sprint-backlog
  - sprint-5
  - execution-plan
  - agents
last_updated: "2026-11-11"
---

# 🚀 Sprint 5 Backlog — Servicio Avanzado y Candidato a Producción (Cerrado v0.5.0 / v1.0.0-RC)

> **Estado del Sprint:** `CERRADO Y CERTIFICADO (100% COMPLETADO)`  
> **Reporte E2E de Certificación:** [`docs/testing/reporte_ejecucion_sprint5.md`](../../testing/reporte_ejecucion_sprint5.md)  
> ⬅️ **Navegación:** [Bóveda de Sprints (Histórico y Planificación)](./README.md) | [Product Backlog (Por Prioridades)](../product_backlog/README.md)  
> **Sprints Históricos:** [Sprint 1 (Cerrado)](./sprint_01_backlog.md) | [Sprint 2 (Cerrado)](./sprint_02_backlog.md) | [Sprint 3 (Cerrado v0.3.0)](./sprint_03_backlog.md) | [Sprint 4 (Cerrado)](./sprint_04_backlog.md)  
> **Próximo Ciclo:** [Sprint 6 (Infraestructura Cloud y SaaS Backoffice)](./sprint_06_planning.md)

---

## 📋 Resumen de Ejecución y Protocolo de Calidad (DoD)

El desarrollo se ejecutó siguiendo el ciclo **SDD-SDLC de 4 fases** (ADR-0007 y Protocolo Multi-Agente) con co-diseño interactivo en Fases 1 y 2 y sincronización documental autónoma:
- **Backend:** `./mvnw test` (**170/170** pruebas unitarias, integración y Spring Modulith ArchUnit pasando sin fallos).
- **Frontend:** `ng lint && ng test --watch=false` (**42 archivos, 373/373** pruebas unitarias pasando en Vitest/JSDOM con **0 advertencias** de linting).
- **E2E Playwright:** Suite de Jornada Maestra (`frontend/e2e/sprint5-journey.spec.ts`) y batería de regresión formalizadas en [`bateria_pruebas_sprint5.md`](../../testing/bateria_pruebas_sprint5.md).
- **Nota de Alcance (Reasignación):** La tarea originalmente numerada como `TASK-512` (Scripts de Inicialización Limpia para Go-Live / PB-050) fue reasignada oficialmente al Sprint 6 como `TASK-607`.

---

## 🎯 Tareas Completadas del Sprint 5

### 1. Modificadores, Complementos y Personalización de Comandas (PB-020)

- [x] **TASK-501 [POS] [NEGOCIO]:** Suite de Modificadores, Complementos y Extras con Configuración de Costo y Kardex (PB-020).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Extender la API y lógica de modificadores para permitir extras con costo adicional/sin costo, exclusiones ("Sin cebolla"), parametrización de deducción de Kardex y persistencia en `order_item_modifiers`.
  - *Criterio de Entrega:* 100% de tests en Spring Modulith pasando; certificado en [ADR-0018](../../adr/0018_pos_modifiers_extras_and_kitchen_notes_suite.md).

- [x] **TASK-502 [POS] [UI/UX]:** Selector Interactivo de Modificadores en POS y Notas de Cocina en Comanda (PB-020).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Modal interactivo con validaciones mínimo/máximo, campo de notas de cocina por ítem, resumen visual en carrito/ticket y persistencia offline en Dexie.js.
  - *Criterio de Entrega:* Selector reactivo sin bloqueos en UI; 100% de tests unitarios en Vitest pasando.

---

### 2. Cancelaciones, Devoluciones y Tratamiento de Mermas (PB-031)

- [x] **TASK-503 [POS] [DISEÑO]:** Máquina de Estados para Cancelaciones, Autorización con PIN y Reversión a Kardex vs Merma (PB-031 / ADR-0019).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Diseñar máquina de estados `CANCELLED`, motivos tipados, política parametrizable `pos.cancellation_prepared_waste_policy`, motor dual de autorización (PIN in-situ y cola remota) y componente `<app-authorization-queue>`.
  - *Criterio de Entrega:* [ADR-0019](../../adr/0019_order_cancellation_workflow_authorizations_and_waste_treatment.md) y diagramas de secuencia/estado certificados. Finalizado el 07/11/2026.

- [x] **TASK-504 [POS] [NEGOCIO]:** Implementación de Reintegro de Efectivo en Gaveta y Registro de Merma por Cancelación (PB-031).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Verificación de PIN de supervisor (`UserPublicApi.validateSupervisorPin`), publicación de `OrderCancelledEvent` para Kardex/Finanzas, registro de `total_cash_refunds` en `cash_shifts` y UI interactiva en `/cash-control` y `/finance`.
  - *Criterio de Entrega:* 100% tests en Spring Modulith y Vitest pasando. Finalizado el 08/11/2026.

---

### 3. Modalidades Para Llevar (Takeout) y Delivery (PB-032)

- [x] **TASK-505 [POS] [NEGOCIO]:** Modalidades de Servicio (`DINE_IN`, `TAKEOUT`, `DELIVERY`, `SCHEDULED_PICKUP`), Selector Dual y Distintivos en Tickets (PB-032 / ADR-0020).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Selector dual de modalidad, campos contextuales de envío, tarifa `delivery_fee` parametrizable (`pos.delivery_fee_enabled`), rótulos destacados en tickets y filtros en visor de ventas.
  - *Criterio de Entrega:* Migración Flyway V13, 100% de tests pasando en Maven (156/156) y Vitest (319/319), [ADR-0020](../../adr/0020_service_modalities_takeout_delivery_and_ticket_badges.md) y diagrama [sequence_pos_service_modalities_and_delivery.md](../architecture/04-behavior-and-flows/sequence_pos_service_modalities_and_delivery.md). Finalizado el 08/11/2026.

---

### 4. Catálogo Visual con Imágenes y Codificación Secuencial (PB-036 / PB-048)

- [x] **TASK-506 [CATALOG] [UI/UX]:** Visualización de Imágenes de Platos en Cuadrícula POS con Toggle Parametrizable (PB-036 / ADR-0021).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Columna `image_url` (Flyway V14), `LocalFileStorageService`, componente nativo `app-image-cropper-modal` (4:3 400x300 px WebP sin dependencias npm), fallback temático con emoji y selector triple de vistas en POS (`COMPLETO`, `COMPACTO`, `LISTA`).
  - *Criterio de Entrega:* 100% tests backend/frontend verificados, [ADR-0021](../../adr/0021_visual_dish_catalog_image_cropping_and_pos_view_modes.md) y diagrama [sequence_pos_dish_image_upload_and_display.md](../architecture/04-behavior-and-flows/sequence_pos_dish_image_upload_and_display.md).

- [x] **TASK-507 [CATALOG] [UI/UX]:** Asistente de SKU Secuencial (`BEB-1`), Selector de Emojis Gastronómicos y Ordenamiento en Catálogo (PB-048 / ADR-0022).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Columna `code_prefix` única por tenant (Flyway V15), endpoint `GET /api/v1/catalog/dishes/next-code`, autocompletado SKU `MAX+1`, selector nativo `app-emoji-picker` ($< 2\text{ KB}$) y precálculo de `sortOrder`.
  - *Criterio de Entrega:* 100% tests backend/frontend verificados, [ADR-0022](../../adr/0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md) y diagrama [sequence_catalog_sku_assistant_and_emoji_picker.md](../architecture/04-behavior-and-flows/sequence_catalog_sku_assistant_and_emoji_picker.md).

---

### 5. Auditoría Gerencial y Autoservicio de Perfil (PB-030 / PB-045 / PB-047)

- [x] **TASK-508 [FINANCE] [BI]:** Histórico Gerencial de Turnos con Filtros por Fechas, Sucursal y Exportación (PB-030 / ADR-0023).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Endpoints `/shifts/history` (con conversión horaria de sucursal ADR-0006) y `/shifts/{id}/audit-detail`, modal drill-down `<app-shift-detail-modal>` y exportador CSV nativo `exportToCsv` con BOM UTF-8.
  - *Criterio de Entrega:* 100% tests backend/frontend verificados, [ADR-0023](../../adr/0023_shift_history_multicriteria_audit_and_csv_export.md) y diagrama [sequence_finance_shift_history_audit_and_export.md](../architecture/04-behavior-and-flows/sequence_finance_shift_history_audit_and_export.md).

- [x] **TASK-509 [AUTH] [UI/UX]:** Perfil de Usuario Autoservicio, Despliegue de Nombres Reales y Limpieza de Privilegios (PB-045 / ADR-0024).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Columna `phone_number` (Flyway V16), rediseño limpio de `/profile` (sin lista cruda de permisos), gestión autoservicio de PIN de 4-6 dígitos, nombres reales en negrita en sidebar/header y búsqueda predictiva en `/users`.
  - *Criterio de Entrega:* 100% tests backend/frontend verificados, [ADR-0024](../../adr/0024_self_service_user_profile_pin_and_humanization.md) y diagrama [sequence_profile_self_service_and_user_humanization.md](../architecture/04-behavior-and-flows/sequence_profile_self_service_and_user_humanization.md).

- [x] **TASK-510 [CONFIG] [UI/UX]:** Humanización de Tarjetas de Parámetros y Opciones en Español (PB-047 / ADR-0025).
  - *Prioridad:* `P2 - Media`
  - *Objetivo:* Catálogo desacoplado JSON `parameters-catalog.json`, carga en arranque en `SystemParameterCatalog`, extensión de `SystemParameterDto` (`help`, `options` humanizadas), componente nativo `<app-context-help>` y buscador predictivo multicriterio.
  - *Criterio de Entrega:* 100% tests backend (170/170) y frontend (373/373) verificados, [ADR-0025](../../adr/0025_parameter_console_humanization_json_catalog_and_contextual_help.md) y diagrama [sequence_parameters_humanization_and_catalog_metadata.md](../architecture/04-behavior-and-flows/sequence_parameters_humanization_and_catalog_metadata.md).

---

### 6. Certificación Final Pre-Producción (DoD)

- [x] **TASK-511 [QA] [CALIDAD]:** Certificación E2E Integral de Todos los Flujos de Negocio de Restaurante (DoD Producción).
  - *Prioridad:* `P1 - Alta`
  - *Objetivo:* Suite determinista `frontend/e2e/sprint5-journey.spec.ts`, actualización de inventario de pruebas y verificación de la matriz de los 4 niveles.
  - *Criterio de Entrega:* 170/170 tests Maven, 373/373 tests Vitest, 0 lint warnings; formalizado en [`bateria_pruebas_sprint5.md`](../../testing/bateria_pruebas_sprint5.md) y certificado en [`reporte_ejecucion_sprint5.md`](../../testing/reporte_ejecucion_sprint5.md).
