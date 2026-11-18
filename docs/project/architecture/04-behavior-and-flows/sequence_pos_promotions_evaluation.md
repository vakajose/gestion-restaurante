---
title: "Secuencia de Evaluación y Aplicación de Promociones en POS"
category: "behavior-flow"
module: "pos"
tags: ["sequence", "pos", "promotions", "timezone", "adr-0006", "adr-0032", "discounts"]
last_updated: "2026-11-17"
status: "verified"
---

# Secuencia de Evaluación y Aplicación de Promociones en POS

Flujo técnico completo para la consulta de promociones vigentes según la hora local de cada sucursal (`ADR-0006`), su previsualización reactiva en el carrito Angular (`PosService`) y la validación autoritativa e imputación contable por ítem en el backend (`PromotionEngine` + `OrderServiceImpl`).

## Diagrama de Secuencia (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    actor Cajero as Cajero (POS)
    participant PosUI as PosComponent
    participant PosSvc as PosService (Angular Signals)
    participant PromoCtrl as PromotionController (/api/v1/pos/promotions)
    participant PromoSvc as PromotionService
    participant BranchApi as BranchPublicApi
    participant Engine as PromotionEngine
    participant OrderSvc as OrderServiceImpl

    %% 1. Carga de promociones activas
    Cajero->>PosUI: Abrir POS en sucursal activa
    PosUI->>PosSvc: loadActivePromotions(branchId)
    PosSvc->>PromoCtrl: GET /api/v1/pos/promotions/active?branchId={id}
    PromoCtrl->>PromoSvc: getActivePromotionsForBranch(tenantId, branchId, evalTime)
    PromoSvc->>BranchApi: getBranchTimezone(branchId)
    BranchApi-->>PromoSvc: timezone (ej. America/La_Paz)
    PromoSvc->>PromoSvc: findActiveWithBranchesByTenantId(tenantId)

    loop Por cada promoción activa
        PromoSvc->>Engine: isPromotionActiveNow(promo, evalTime, timezone, branchId)
        Note right of Engine: Valida promotion_branches, hora local sucursal, fechas, daysOfWeek y ventana horaria (incl. cruce medianoche)
    end
    PromoSvc-->>PromoCtrl: List PromotionDto vigentes ahora
    PromoCtrl-->>PosSvc: HTTP 200 OK
    PosSvc->>PosSvc: activePromotions.set(promos)

    %% 2. Cálculo reactivo en carrito POS
    Cajero->>PosUI: Agregar ítems al carrito / elegir promo manual opcional
    PosSvc->>PosSvc: effectivePromotion = selectedPromotion ?? primera AUTOMATIC
    PosSvc->>PosSvc: computed discountAmount (valida minOrderAmount; calcula GLOBAL o DISH)
    PosSvc-->>PosUI: Mostrar rawSubtotal, discountAmount y totalAmount neto

    %% 3. Validación autoritativa al cobrar
    Cajero->>PosSvc: checkout(...) -> POST /api/v1/pos/orders (appliedPromotionId)
    OrderSvc->>OrderSvc: Resolver zona horaria sucursal (getBranchZoneId)
    alt appliedPromotionId enviado (manual o preseleccionada)
        OrderSvc->>Engine: isPromotionActiveNow(promo, Instant.now(), branchTimezone, branchId)
        alt Expiró o no aplica a la sucursal
            Engine-->>OrderSvc: false
            OrderSvc-->>PosSvc: HTTP 422 BusinessRuleException (promoción no vigente)
        end
    else Sin appliedPromotionId
        OrderSvc->>Engine: Seleccionar primera promoción AUTOMATIC vigente en este instante
    end

    opt Promoción aplicable y totalAmount >= minOrderAmount
        alt scope == GLOBAL
            OrderSvc->>OrderSvc: Distribuir PERCENTAGE o FIXED_AMOUNT entre orderItems[].discountAmount
        else scope == CATEGORY o DISH
            loop Por cada OrderItem (plato o categoría de combo/plato)
                OrderSvc->>Engine: calculateItemDiscount(promo, dishId, categoryId, itemSubtotal)
                OrderSvc->>OrderSvc: Asignar item.discountAmount (topado a itemSubtotal)
            end
        end
        OrderSvc->>OrderSvc: Restar descuento total antes de sumar deliveryFee
    end
```

## Reglas de Evaluación en `PromotionEngine`

1. **Vigencia Estricta en Hora Local (`ADR-0006`):**
   - Toda marca `Instant` se convierte con `ZoneId` de la sucursal (`branches.timezone`, fallback `America/La_Paz`).
   - Se verifican en orden: estado `isActive`, pertenencia de `branchId` a `promotion_branches` (una lista vacía aplica a todas las sucursales del tenant), rango calendario `startDate`..`endDate`, coincidencia con `daysOfWeek` (soporta nombres ISO como `MONDAY` o números `1..7`) y franja horaria `startTime`..`endTime`.
   - **Cruce de Medianoche:** Cuando `startTime.isAfter(endTime)` (por ejemplo Happy Hour nocturno `22:00:00` a `02:00:00`), la ventana acepta horas `>= startTime` o `<= endTime`.
2. **Modos y Alcances:**
   - **`applicationMode`:** `AUTOMATIC` (se selecciona automáticamente en backend si no se envió `appliedPromotionId`) o `MANUAL` (requiere selección explícita del cajero en POS).
   - **`scope`:** `GLOBAL`, `CATEGORY` (`targetCategoryId`) o `DISH` (`targetDishId`).
   - **`discountType`:** `PERCENTAGE` o `FIXED_AMOUNT` (cada descuento por ítem nunca excede su propio `subtotal`, y el `totalAmount` nunca desciende de `0.00`).
3. **Paridad Cliente vs Servidor:**
   - El señalizado en Angular (`PosService.discountAmount`) precalcula en tiempo real descuentos `GLOBAL` y `DISH` para retroalimentación inmediata al cajero, mientras que `OrderServiceImpl` recalcula autoritativamente en el servidor todos los alcances (`GLOBAL`, `CATEGORY` y `DISH`) incluyendo la categoría de combos al persistir `order_items.discount_amount`.

## Enlaces Relacionados

- [Flujo de Venta POS y Deducción](./sequence_pos_order_deduction.md)
- [Modelo Entidad-Relación (`promotions`, `promotion_branches`)](../02-domain-and-data/database_erd.md)
- [Casos de Uso POS](../03-use-cases/use_cases_pos.md)
- [ADR-0006: Estándar de Fechas y Zonas Horarias](../../../adr/0006_datetime_and_timezone_handling.md)
- [ADR-0032: Motor de Promociones POS](../../../adr/0032_pos_promotions_engine.md)
