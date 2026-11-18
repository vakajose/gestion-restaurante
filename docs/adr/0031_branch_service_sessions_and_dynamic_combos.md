---
title: "ADR-0031: Sesiones de Servicio por Sucursal, Menús del Día Desacoplados y Combos Dinámicos"
category: "adr"
tags:
  - adr
  - catalog
  - daily-menu
  - combos
  - service-session
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0031: Sesiones de Servicio por Sucursal, Menús del Día Desacoplados y Combos Dinámicos

## Contexto y Problema
El diseño inicial de Menús del Día ([ADR-0015](./0015_daily_menus_and_flexible_dish_inventory.md)) acoplaba la asignación de menú y sus cupos diarios directamente a un `cash_shift` individual. Al introducir la concurrencia multi-cajero por sucursal ([ADR-0016](./0016_cash_control_separation_and_multi_cashier_shifts.md)), donde múltiples cajeros operan en paralelo dentro del mismo restaurante con `INDIVIDUAL_PER_CASHIER`, ese acoplamiento generaba tres fallos críticos:
1. Cada cajero debía re-configurar el menú del día o duplicar cupos aislados por caja en lugar de compartir el inventario diario de cocina de la sucursal.
2. Si un turno de caja cerraba a media tarde y abría el turno siguiente, los cupos compartidos de almuerzo se fragmentaban o perdían continuidad operativa.
3. No existía soporte estructural para combos armables por pasos (ej. Almuerzo Ejecutivo: Sopa a elegir + Segundo a elegir + Postre) cuyos platos seleccionados pudieran venderse solo dentro del combo (`allowIndividualSale = false`) o con cupos ilimitados (`isUnlimited = true`), ni soporte de recurrencia semanal multi-día en `daily_menus`.

## Decisión
Se introduce la entidad **`BranchServiceSession`** y el modelo unificado de **Combos con Selección por Slots y Menú del Día**:

1. **Desacoplamiento mediante `BranchServiceSession` (`V23`):**
   - La tabla `branch_service_sessions` representa la jornada operativa de servicio compartida de una sucursal, garantizando unicidad activa por sucursal (`status IN ('CONFIGURED', 'ACTIVE')`).
   - Opera en dos modos (`menuMode`): `FULL_CATALOG` (carta completa libre) o `DAILY_MENU` (catálogo gobernado por menú del día y cupos compartidos).
   - Endpoints en `CatalogController`: `GET /api/v1/catalog/service-sessions/current`, `POST /api/v1/catalog/service-sessions/configure` y `POST /api/v1/catalog/service-sessions/{sessionId}/close`.
   - Tanto `cash_shifts.service_session_id` como `shift_menu_assignments.service_session_id` referencian a esta sesión de servicio compartida en lugar de aislar asignaciones por cajero.
   - El parámetro `finance.auto_open_full_catalog_session` (booleano, default `true`, alcance `BRANCH`, criticidad `HIGH`) controla si el POS abre automáticamente una sesión `FULL_CATALOG` al abrir el primer turno de caja cuando no existe jornada configurada.
2. **Menús del Día enriquecidos (`V24`):**
   - `daily_menus.day_of_week` se amplía a `VARCHAR(120)` para admitir listas de días separadas por comas (ej. `LUNES,MARTES,MIERCOLES`).
   - `DailyMenuItem` y `ShiftMenuItem` incorporan `allowIndividualSale` (permite ocultar un plato como venta suelta pero mantenerlo disponible dentro de combos) e `isUnlimited` (elimina el tope numérico de cupo diario).
   - La tabla puente `daily_menu_combos` vincula combos configurados a un menú del día validando en `DailyMenuServiceImpl` que las selecciones requeridas y slots `MENU_COURSE` sean consistentes.
3. **Combos Dinámicos y Selecciones Explícitas (`V23`, `TASK-634`):**
   - `CreateComboRequest` / `CreateComboItemRequest` soportan tres fuentes de selección (`selectionSource`):
     - `FIXED`: ítem fijo del combo.
     - `SPECIFIC_OPTIONS`: lista explícita de platos permitidos en el slot con ajuste opcional de precio.
     - `MENU_COURSE`: slot dinámico vinculado a un tiempo de menú (`courseType`: `STARTER | MAIN | DESSERT | BEVERAGE | GENERAL`) resuelto contra el menú activo de la `BranchServiceSession`.
   - En `OrderItem` (`modules.pos`), `dish_id` pasa a ser **nullable** (`V23`) para representar líneas de tipo combo (`comboId` + `dailyMenuId`), mientras el detalle exacto escogido por el cliente se persiste en `order_item_selections` (`slotLabel`, `selectedDishId`, `quantity`, `priceAdjustment`) y alimenta tanto la deducción de cupos del menú como el descuento de Kardex en inventario.
4. **Hoja de ruta evolutiva (`TASK-637` y `TASK-638`):**
   - Queda planificado para evolución futura (`TASK-637`) migrar los filtros y slots basados en `CourseType` hacia `category_id` (`0..*`) del catálogo, y (`TASK-638`) unificar los conceptos de Plato/Combo/Complemento soportando grupos de modificadores en combos, costos extras por slot y materialización de `FULL_CATALOG`.

## Consecuencias

### Positivas
- **Multi-cajero sin fricción:** Todos los cajeros de una sucursal comparten en tiempo real la misma sesión de servicio, menú activo y descuento atómico de cupos.
- **Modelado fiel del menú ejecutivo y combos:** El cajero arma el combo en un solo flujo en POS y el backend deduce simultáneamente cupos del menú del día e ingredientes de cada plato elegido.
- **Flexibilidad operativa:** Restricciones de venta individual (`allowIndividualSale = false`) permiten controlar márgenes en menús completos.

### Negativas y Mitigaciones
- **Complejidad adicional en sincronización offline POS:** Los combos complejos y cupos de `BranchServiceSession` requieren validación de servidor (`ITEM_SOLD_OUT`) y aún no cuentan con tabla dedicada en Dexie.js (`appDb`). *Mitigación:* Documentado explícitamente en [ADR-0003](./0003_offline_first_dexie_sync.md) y planificado bajo `TASK-640`.

## Referencias de Implementación
- Migraciones Flyway: `V23__service_sessions_combo_menus_and_order_selections.sql`, `V24__widen_daily_menus_day_of_week.sql`.
- Backend Catálogo: `CatalogController.java`, `DailyMenuController.java`, `CatalogServiceImpl.java`, `DailyMenuServiceImpl.java`, `CreateComboRequest.java`, `CreateComboItemRequest.java`, `ConfigureServiceSessionRequest.java`, `CreateDailyMenuRequest.java`.
- Backend POS / Inventario: `OrderItem.java`, `OrderItemSelection.java`, `CreateOrderItemRequest.java`, `OrderServiceImpl.java`, `KardexService.java`.
- Frontend Angular: `frontend/src/app/features/catalog/menus/`, `frontend/src/app/features/pos/`.

## Documentos Relacionados
- [ADR-0003: Estrategia Offline-First con Dexie.js e Idempotencia](./0003_offline_first_dexie_sync.md)
- [ADR-0004: Catálogo Híbrido: Sobreescrituras y Clonación Estructural](./0004_catalog_cloning_and_overrides.md)
- [ADR-0013: Invariante Transaccional de Turno en POS y Políticas de Caja](./0013_pos_shift_invariant_and_expense_authorizations.md)
- [ADR-0015: Menús del Día y Deducción Flexible de Inventario](./0015_daily_menus_and_flexible_dish_inventory.md)
- [ADR-0016: Separación de Control de Caja y Turnos Multi-Cajero](./0016_cash_control_separation_and_multi_cashier_shifts.md)
- [ADR-0018: Suite de Modificadores, Extras y Notas de Cocina](./0018_pos_modifiers_extras_and_kitchen_notes_suite.md)
- [ADR-0030: Aislamiento del Catálogo por Sucursal por Defecto](./0030_catalog_branch_isolation_by_default.md)
- [Casos de Uso de Catálogo](../project/architecture/03-use-cases/use_cases_catalog.md)
- [Casos de Uso de POS](../project/architecture/03-use-cases/use_cases_pos.md)
- [Casos de Uso de Finanzas](../project/architecture/03-use-cases/use_cases_finance.md)
- [Casos de Uso de Configuración](../project/architecture/03-use-cases/use_cases_configuration.md)
- [Secuencia de Asignación de Menú del Día y Cumplimiento de Cupos](../project/architecture/04-behavior-and-flows/sequence_daily_menu_shift_assignment_and_quota_enforcement.md)
- [Secuencia de Venta en POS y Deducción de Inventario](../project/architecture/04-behavior-and-flows/sequence_pos_order_deduction.md)
