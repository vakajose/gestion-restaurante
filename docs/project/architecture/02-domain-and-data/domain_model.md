---
title: "Modelo de Dominio Java y Agregados Spring Modulith"
category: "architecture"
tags: ["domain-model", "ddd", "jpa", "java-25", "spring-modulith", "aggregates", "enums"]
last_updated: "2026-11-17"
status: "verified"
---

# Modelo de Dominio Java y Agregados Spring Modulith

Representación del modelo de objetos JPA y enums/contratos de dominio verificados en `backend/src/main/java/com/restaurant/app/` hasta `V24` (**40 entidades JPA ERP/Config** + **8 entidades Backoffice**). Por brevedad arquitectónica, los diagramas omiten getters/setters y campos técnicos repetitivos (`id`, `createdAt`, `version`), destacando atributos de negocio, invariantes y relaciones entre agregados.

> [!NOTE]
> **Precisiones estructurales verificadas en código:**
> - Cada entidad transaccional declara explícitamente `tenantId` (y `branchId` cuando aplica) sin heredar de `BaseTenantEntity` ni `BaseBranchEntity` (únicamente `BaseBranchEntity extends BaseTenantEntity` sin subclases `@Entity`).
> - Las tablas DDL `purchases`, `purchase_items` y `audit_logs` (`V1`) aún no tienen entidades `@Entity` en el dominio Java.
> - Los agregados del plano Backoffice (`PlatformAdmin`, `PlatformRefreshToken`, `SubscriptionPlan`, `TenantSubscription`, `SubscriptionCharge`, `SubscriptionPayment`, `TenantDailyTelemetry`, `PlatformAuditLog`) están modelados en [Dominio y ERD del Backoffice](../../../backoffice/architecture/domain_and_erd_subscriptions.md).

---

## 1. Núcleo Transversal: Tenancy, Configuración, Entitlements y RBAC

```mermaid
classDiagram
    class Tenant {
        +UUID id
        +String legalName
        +String slug
        +String nitOrTaxId
        +String logoUrl
        +Boolean isActive
    }

    class Branch {
        +UUID id
        +UUID tenantId
        +String name
        +String timezone
        +String currency
        +BigDecimal latitude
        +BigDecimal longitude
        +Boolean isActive
    }

    class TenantSetting {
        +UUID tenantId
        +String settingKey
        +String settingValue
        +String valueType
    }

    class BranchSetting {
        +UUID tenantId
        +UUID branchId
        +String settingKey
        +String settingValue
    }

    class PlatformSetting {
        +String settingKey
        +String settingValue
        +String valueType
    }

    class SystemParameterValue {
        +String parameterKey
        +ParameterScope scope
        +UUID tenantId
        +UUID branchId
        +String parameterValue
        +String auditReason
    }

    class SystemParameter {
        <<enumeration>>
        20 claves tipadas
        +ParameterCategory category
        +ParameterCriticality criticality
        +ParameterScope minScope
    }

    class AppUser {
        +UUID tenantId
        +UUID branchId
        +String username
        +String fullName
        +String phoneNumber
        +String role
        +UUID roleId
        +Instant roleValidUntil
        +Boolean mustChangePassword
        +String pinHash
        +Instant lastLoginAt
    }

    class Role {
        +UUID tenantId
        +String code
        +String name
        +Boolean isSystem
    }

    class RolePermission {
        +UUID tenantId
        +UUID roleId
        +String permission
    }

    class UserBranch {
        +UUID tenantId
        +UUID userId
        +UUID branchId
    }

    class RefreshToken {
        +UUID tenantId
        +UUID userId
        +String tokenHash
        +Instant expiresAt
        +Instant revokedAt
        +UUID replacedById
    }

    class TenantEntitlementSnapshot {
        <<record>>
        +SubscriptionStatus status
        +SuspensionMode suspensionMode
        +int maxBranches
        +int maxUsers
        +int maxMonthlyOrders
    }

    Tenant "1" --> "*" Branch : posee
    Tenant "1" --> "*" TenantSetting : configura
    Branch "1" --> "*" BranchSetting : sobreescribe
    Tenant "1" --> "*" Role : define
    Role "1" --> "*" RolePermission : concede
    Tenant "1" --> "*" AppUser : pertenece
    AppUser "*" --> "0..1" Role : roleId
    AppUser "1" --> "*" UserBranch : habilitadoEn
    AppUser "1" --> "*" RefreshToken : sesiones
    SystemParameterValue ..> SystemParameter : instancia clave
```

---

## 2. Catálogo, Recetas, Modificadores, Combos, Menús y Jornadas (`modules.catalog`)

```mermaid
classDiagram
    class Category {
        +UUID tenantId
        +String name
        +String codePrefix
        +String icon
        +Integer sortOrder
    }

    class Ingredient {
        +UUID tenantId
        +String name
        +String unitOfMeasure
        +BigDecimal minStockAlert
    }

    class Dish {
        +UUID tenantId
        +UUID branchId
        +UUID clonedFromId
        +UUID categoryId
        +String code
        +String name
        +BigDecimal salePrice
        +String imageUrl
        +InventoryStrategy inventoryStrategy
        +Boolean allowNegativeStockOverride
    }

    class DishRecipe {
        +UUID dishId
        +UUID ingredientId
        +BigDecimal quantity
    }

    class BranchDish {
        +UUID tenantId
        +UUID branchId
        +UUID dishId
        +Boolean isAvailable
        +BigDecimal priceOverride
    }

    class ModifierGroup {
        +UUID tenantId
        +String name
        +Integer minSelectable
        +Integer maxSelectable
        +Boolean isRequired
    }

    class Modifier {
        +UUID tenantId
        +UUID groupId
        +String name
        +BigDecimal additionalPrice
        +UUID ingredientId
        +BigDecimal quantityConsumed
    }

    class Combo {
        +UUID tenantId
        +UUID branchId
        +UUID categoryId
        +String code
        +String name
        +BigDecimal basePrice
    }

    class ComboItem {
        +UUID comboId
        +String name
        +String selectionSource
        +CourseType courseType
        +Boolean isRequired
        +UUID defaultDishId
        +Integer quantity
    }

    class ComboItemOption {
        +UUID comboItemId
        +UUID dishId
        +BigDecimal priceAdjustment
        +Boolean isDefault
    }

    class DailyMenu {
        +UUID tenantId
        +UUID branchId
        +String name
        +String dayOfWeek
        +PriceMode priceMode
        +BigDecimal packagePrice
        +Boolean isTemplate
    }

    class DailyMenuItem {
        +UUID menuId
        +UUID dishId
        +CourseType courseType
        +Integer suggestedQuota
        +BigDecimal overridePrice
        +Boolean allowIndividualSale
        +Boolean isUnlimited
    }

    class DailyMenuCombo {
        +UUID menuId
        +UUID comboId
        +Integer sortOrder
    }

    class BranchServiceSession {
        +UUID tenantId
        +UUID branchId
        +LocalDate serviceDate
        +String menuMode
        +UUID dailyMenuId
        +String status
        +Instant activatedAt
        +Instant closedAt
    }

    class ShiftMenuAssignment {
        +UUID tenantId
        +UUID branchId
        +UUID serviceSessionId
        +UUID cashShiftId
        +UUID menuId
    }

    class ShiftMenuItem {
        +UUID shiftMenuAssignmentId
        +UUID dishId
        +CourseType courseType
        +Integer initialQuota
        +Integer currentQuota
        +Integer soldCount
        +BigDecimal effectivePrice
        +Boolean isSoldOut
        +Boolean allowIndividualSale
        +Boolean isUnlimited
        +Long version
    }

    class ShiftMenuQuotaAdjustment {
        +UUID shiftMenuItemId
        +UUID adjustedBy
        +Integer quantityDelta
        +String reason
    }

    Category "1" --> "*" Dish : clasifica
    Category "1" --> "*" Combo : clasifica
    Dish "1" *-- "*" DishRecipe : receta
    DishRecipe "*" --> "1" Ingredient : insumo
    Dish "1" --> "*" BranchDish : override
    Dish "*" o-- "*" ModifierGroup : dish_modifier_groups
    ModifierGroup "1" *-- "*" Modifier : opciones
    Combo "1" *-- "*" ComboItem : bloques
    ComboItem "1" *-- "*" ComboItemOption : elecciones
    DailyMenu "1" *-- "*" DailyMenuItem : tiempos
    DailyMenu "1" *-- "*" DailyMenuCombo : combos
    BranchServiceSession "0..1" --> "0..1" DailyMenu : aplica
    BranchServiceSession "1" --> "0..1" ShiftMenuAssignment : activa
    ShiftMenuAssignment "1" *-- "*" ShiftMenuItem : cupos
    ShiftMenuItem "1" --> "*" ShiftMenuQuotaAdjustment : historial
```

---

## 3. Ventas POS, Promociones, Inventario y Finanzas (`modules.pos`, `inventory`, `finance`)

```mermaid
classDiagram
    class Order {
        +UUID tenantId
        +UUID branchId
        +UUID cashShiftId
        +String ticketNumber
        +String orderStatus
        +String paymentMethod
        +String orderType
        +BigDecimal deliveryFee
        +BigDecimal totalAmount
        +UUID clientTransactionId
        +String cancellationReason
        +String refundMethod
        +String wastePolicyApplied
    }

    class OrderItem {
        +UUID orderId
        +UUID dishId
        +UUID comboId
        +UUID dailyMenuId
        +Integer quantity
        +BigDecimal unitPrice
        +BigDecimal discountAmount
        +BigDecimal subtotal
    }

    class OrderItemModifier {
        +UUID orderItemId
        +UUID modifierId
        +String modifierName
        +BigDecimal unitPrice
        +UUID ingredientId
        +BigDecimal quantityConsumed
    }

    class OrderItemSelection {
        +UUID orderItemId
        +String slotLabel
        +UUID selectedDishId
        +Integer quantity
        +BigDecimal priceAdjustment
    }

    class Promotion {
        +UUID tenantId
        +String name
        +String discountType
        +BigDecimal discountValue
        +String scope
        +String applicationMode
        +String daysOfWeek
        +LocalTime startTime
        +LocalTime endTime
        +LocalDate startDate
        +LocalDate endDate
        +BigDecimal minOrderAmount
    }

    class PromotionBranch {
        +UUID promotionId
        +UUID branchId
    }

    class CashShift {
        +UUID tenantId
        +UUID branchId
        +UUID serviceSessionId
        +UUID openedBy
        +BigDecimal initialCash
        +BigDecimal totalCashSales
        +BigDecimal totalCashExpenses
        +BigDecimal totalCashRefunds
        +BigDecimal expectedCash
        +BigDecimal actualCash
        +String status
    }

    class Expense {
        +UUID tenantId
        +UUID branchId
        +UUID cashShiftId
        +String expenseType
        +BigDecimal amount
        +Boolean paidFromCashDrawer
        +String approvalStatus
        +LocalDate expenseDate
    }

    class ShiftAuthorization {
        +UUID tenantId
        +UUID branchId
        +UUID cashShiftId
        +String requestType
        +UUID referenceId
        +BigDecimal amount
        +String status
        +String decisionChannel
    }

    class BranchStock {
        +UUID tenantId
        +UUID branchId
        +UUID ingredientId
        +UUID dishId
        +BigDecimal currentQuantity
        +BigDecimal averageUnitCost
    }

    class KardexMovement {
        +UUID tenantId
        +UUID branchId
        +UUID ingredientId
        +UUID dishId
        +String movementType
        +BigDecimal quantity
        +BigDecimal unitCost
        +BigDecimal balanceQuantity
        +UUID referenceId
        +Instant movementDate
    }

    class PreparedDishBuffer {
        +UUID tenantId
        +UUID branchId
        +UUID orderId
        +UUID dishId
        +Integer quantity
        +Instant expiresAt
        +Boolean isConsumed
    }

    class InventoryStrategyFactory {
        <<domain service>>
        +getStrategy(InventoryStrategy) InventoryDeductionStrategy
    }

    Order "1" *-- "*" OrderItem : lineas
    OrderItem "1" *-- "*" OrderItemModifier : extras
    OrderItem "1" *-- "*" OrderItemSelection : seleccionesCombo
    Promotion "1" *-- "*" PromotionBranch : sucursales
    CashShift "1" --> "*" Order : agrupa
    CashShift "1" --> "*" Expense : egresos
    CashShift "1" --> "*" ShiftAuthorization : aprobaciones
    Order ..> KardexMovement : OrderPaidEvent / OrderCancelledEvent
    Order ..> PreparedDishBuffer : rescate cancelacion
    InventoryStrategyFactory ..> BranchStock : actualiza stock
```

## Eventos de Dominio entre Módulos (Spring Modulith)

| Evento (`.api`) | Publicador | Consumidor(es) (`@ApplicationModuleListener`) | Propósito |
| :--- | :--- | :--- | :--- |
| `TenantOnboardedEvent` | `modules.company` | Extensible para post-aprovisionamiento | Notifica creación inicial de tenant, admin y sucursal matriz. |
| `ParameterUpdatedEvent` | `modules.configuration` | `core.security` / listeners de caché | Propaga recarga caliente de parámetros críticos (ej. expiración JWT, políticas de contraseña). |
| `OrderPaidEvent` | `modules.pos` | `modules.inventory.InventoryEventListener`, `modules.backoffice.BackofficeTelemetryListener` | Deduce inventario Kardex de forma asíncrona tras la venta y acumula telemetría diaria de consumo. |
| `OrderCancelledEvent` | `modules.pos` | `modules.inventory.InventoryEventListener` | Revierte stock, inserta platos rescatables en `PreparedDishBuffer` o registra `WASTE_ADJUSTMENT`. |
| `TenantEntitlementChangedEvent` | `modules.backoffice` | `core.entitlements.TenantEntitlementServiceImpl` | Invalida caché en memoria del snapshot de suscripción/cuotas del tenant afectado. |

## Enlaces Relacionados

- [Modelo Entidad-Relación PostgreSQL (47 tablas ERP)](./database_erd.md)
- [ERD y Dominio Backoffice (9 tablas V17–V22)](../../../backoffice/architecture/domain_and_erd_subscriptions.md)
- [Esquema Local Dexie.js](./dexie_local_schema.md)
- [C4 Nivel 3: Backend Spring Modulith](../01-c4-system/c4_level3_backend_modulith.md)
- [Estrategia de Deducción Kardex](../04-behavior-and-flows/activity_inventory_deduction_strategy.md)
