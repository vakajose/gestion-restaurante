---
title: "ADR-0032: Motor de Promociones en POS con Evaluación por Zona Horaria de Sucursal"
category: "adr"
tags:
  - adr
  - pos
  - promotions
  - timezone
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0032: Motor de Promociones en POS con Evaluación por Zona Horaria de Sucursal

## Contexto y Problema
Los restaurantes requieren aplicar descuentos comerciales programados por campaña, franja horaria (ej. *Happy Hour*), día de la semana, categoría o plato específico, así como habilitar futuras promociones manuales o cupones por monto mínimo de pedido. Calcular estas reglas en el cliente sin evaluación consistente en servidor expone inconsistencias horarias (navegador con hora local alterada vs `branches.timezone`) y debilita el control financiero en caja.

Era necesario establecer un motor determinista de promociones en el módulo `pos` que respete estrictamente las reglas de zona horaria del proyecto ([ADR-0006](./0006_datetime_and_timezone_handling.md)) y permita consultar promociones vigentes por sucursal sin acoplar reglas comerciales dentro de los componentes visuales.

## Decisión
Se implementa el subsistema de promociones en `modules.pos` compuesto por `Promotion`, `PromotionBranch`, `PromotionEngine`, `PromotionService` y `PromotionController`:

1. **Modelo de reglas promocionales (`CreatePromotionRequest` / `Promotion`):**
   - **Tipos de descuento (`discountType`):** `PERCENTAGE` o `FIXED_AMOUNT`.
   - **Alcance (`scope`):** `GLOBAL` (toda la orden), `CATEGORY` (categorías elegibles) o `DISH` (platos elegibles).
   - **Modo de aplicación (`applicationMode`):** `AUTOMATIC` (aplicación automática cuando cumple condiciones) o `MANUAL` (activación explícita sobre la orden).
   - **Ventana temporal y condiciones:** `daysOfWeek`, franja horaria diaria (`startTime`, `endTime`), vigencia de fechas (`startDate`, `endDate`), monto mínimo de pedido (`minOrderAmount`) y lista de sucursales habilitadas (`branchIds` vía `PromotionBranch`).
2. **Evaluación estricta en `branches.timezone` (`PromotionEngine`):**
   - El instante de evaluación (`evalTime`, o `Instant.now()` si se omite) se convierte explícitamente a `ZoneId` de la sucursal (`branches.timezone`, default `America/La_Paz`), evaluando allí `LocalDate`, `DayOfWeek` y `LocalTime` conforme a [ADR-0006](./0006_datetime_and_timezone_handling.md).
   - La lógica pura de elegibilidad y cálculo reside en `PromotionEngine` (sin efectos secundarios), facilitando pruebas unitarias de dominio (Nivel 1 de [ADR-0017](./0017_multilevel_testing_governance_and_automation_standard.md)).
3. **API REST dedicada (`/api/v1/pos/promotions`):**
   - `GET /api/v1/pos/promotions`: listado de promociones del tenant.
   - `GET /api/v1/pos/promotions/active?branchId=&evalTime=`: consulta promociones activas vigentes para la sucursal en el instante indicado.
   - `GET /api/v1/pos/promotions/{id}`: detalle de promoción.
   - `POST /api/v1/pos/promotions`: creación de campaña promocional con asignación por sucursal (`branchIds`).
4. **Estado actual de interfaz POS y alcance pendiente:**
   - La UI de POS en Angular consulta `GET /api/v1/pos/promotions/active` y evalúa/aplica automáticamente las promociones en modo `AUTOMATIC` (vía señal reactiva `effectivePromotion()`).
   - La pantalla administrativa CRUD de promociones, la selección manual en POS de promociones `MANUAL` y la persistencia offline en Dexie.js de promociones activas quedan fuera del corte actual y forman parte de la hoja de ruta pendiente (`TASK-640`).

## Consecuencias

### Positivas
- **Consistencia horaria multi-sucursal:** Campañas de almuerzo o *Happy Hour* se activan y desactivan exactamente según el reloj local de cada sucursal.
- **Desacoplamiento del motor de reglas:** `PromotionEngine` permite probar combinaciones de fechas, horas, días y montos mínimos sin levantar base de datos ni navegador.
- **Base preparada para modo manual:** El contrato de dominio y API ya soporta `applicationMode = MANUAL` para habilitar selección explícita o cupones en iteraciones futuras.

### Negativas y Mitigaciones
- **Dependencia de conectividad en promociones vigentes:** Mientras Dexie.js (`appDb`) no almacene promociones cacheadas (`TASK-640`), las ventas creadas completamente sin red no descargarán nuevas campañas iniciadas durante la desconexión. *Mitigación:* El POS registra la orden offline de manera idempotente y mantiene claro el alcance online vigente en la documentación de arquitectura.

## Referencias de Implementación
- Backend: `Promotion.java`, `PromotionBranch.java`, `PromotionEngine.java`, `PromotionService.java`, `PromotionController.java`, `CreatePromotionRequest.java` en `backend/src/main/java/com/restaurant/app/modules/pos/`.
- Frontend Angular: `frontend/src/app/features/pos/` (`effectivePromotion()`), `frontend/src/app/core/services/pos.service.ts`.

## Documentos Relacionados
- [ADR-0003: Estrategia Offline-First con Dexie.js e Idempotencia](./0003_offline_first_dexie_sync.md)
- [ADR-0006: Manejo Estricto de Fechas y Zonas Horarias](./0006_datetime_and_timezone_handling.md)
- [ADR-0017: Gobernanza de Testing Automatizado Multinivel](./0017_multilevel_testing_governance_and_automation_standard.md)
- [Casos de Uso de POS](../project/architecture/03-use-cases/use_cases_pos.md)
- [Secuencia de Evaluación de Promociones en POS](../project/architecture/04-behavior-and-flows/sequence_pos_promotions_evaluation.md)
- [Secuencia de Venta en POS y Deducción de Inventario](../project/architecture/04-behavior-and-flows/sequence_pos_order_deduction.md)
