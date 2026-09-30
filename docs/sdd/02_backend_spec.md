# 02. Especificación Técnica de Backend

## 1. Framework y Configuración Base
- **Lenguaje:** Java 25 LTS (Amazon Corretto).
- **Framework:** Spring Boot 4.1.x / 3.4.x (según compatibilidad de dependencias en `pom.xml`).
- **Virtual Threads:** Habilitado mediante `spring.threads.virtual.enabled=true`.
- **Manejo de Errores:** `ProblemDetail` según RFC 7807 para todas las respuestas de excepción REST.
- **Validación Arquitectónica:** Spring Modulith con verificación estricta de límites de módulo.

---

## 2. IAM Híbrido y Adaptativo (Perfiles de Configuración)
- `@Profile("auth-local")`: Emite tokens JWT firmados (HMAC-SHA256 / RSA). Almacena credenciales con algoritmo de hashing Argon2id en `app_users`.
  - **Identidad Adaptativa:** Permite autenticación por `username` (identificador primario) o `email`. Soporta usuarios sin correo electrónico para operadores de cocina o cajeros cuando `tenant_settings.REQUIRE_USER_EMAIL = 'false'`.
- `@Profile("auth-oidc")`: Configura Spring Security como Resource Server (`oauth2ResourceServer().jwt()`) para validar tokens emitidos por Keycloak o proveedores OIDC externos.

---

## 3. Estructura de Paquetes (Spring Modulith)

```text
com.restaurant.app/
├── core/
│   ├── security/           # JWT, UserDetails, TenantContextHolder, OIDC
│   ├── config/             # TenantConfigService, BranchConfigService, Dynamic Settings
│   ├── audit/              # AuditLogService, interceptor de cambios JSONB
│   └── common/             # BaseTenantEntity, BaseBranchEntity, ProblemDetail
└── modules/
    ├── catalog/
    │   ├── api/            # CatalogPublicApi, DTOs públicos de recetas y platos
    │   └── internal/       # Entidades, repositorios y servicios internos de catálogo
    ├── inventory/
    │   ├── api/            # InventoryPublicApi, DTOs de existencias
    │   └── internal/       # KardexService, PurchaseService, listeners de deducción
    ├── pos/
    │   ├── api/            # OrderPaidEvent, DTOs de órdenes
    │   └── internal/       # OrderService, KDS, endpoints REST del POS
    ├── finance/
    │   ├── api/            # FinancePublicApi
    │   └── internal/       # CashShiftService, ExpenseApprovalWorkflow, Arqueo
    └── analytics/
        └── internal/       # Cálculo de márgenes, rentabilidad y punto de equilibrio
```

---

## 4. Jerarquía Dual de Entidades Base (`core.common`)

Para respetar el principio de segregación y el modelo de datos físico, se definen dos clases base abstractas:

```java
package com.restaurant.app.core.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseTenantEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

```java
package com.restaurant.app.core.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseBranchEntity extends BaseTenantEntity {

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
}
```

### Distribución de Entidades de Dominio:
- **Extienden de `BaseTenantEntity`:** `Category`, `Ingredient`, platos maestros en `Dishes`, `TenantSetting`.
- **Extienden de `BaseBranchEntity`:** `BranchStock`, `Purchase`, `PurchaseItem`, `Order`, `OrderItem`, `CashShift`, `Expense`, `BranchDish`, `BranchSetting`.
- **Auditoría (`AuditLog`):** Extiende de `BaseTenantEntity` e incorpora un campo opcional `branchId` (nullable).

---

## 5. Servicio de Configuración Dinámica (`core.config`)

Para desacoplar el código de cambios de esquema ante nuevos requerimientos de negocio, el servicio consulta `tenant_settings` y `branch_settings` con fallbacks a valores por defecto tipados:

```java
package com.restaurant.app.core.config.api;

import java.util.UUID;

public interface TenantConfigService {
    String getString(UUID tenantId, String key, String defaultValue);
    boolean getBoolean(UUID tenantId, String key, boolean defaultValue);
    int getInt(UUID tenantId, String key, int defaultValue);
}
```

---

## 6. Contratos de Comunicación Inter-Módulo (Spring Modulith)

Queda estrictamente prohibido inyectar repositorios o servicios internos de un módulo en otro. La comunicación se realiza exclusivamente mediante interfaces públicas en el paquete `.api` o mediante eventos asíncronos.

### A. API Pública de Catálogo (`modules.catalog.api`):

```java
package com.restaurant.app.modules.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogPublicApi {

    record RecipeItemDto(UUID ingredientId, String ingredientName, BigDecimal quantity, String unitOfMeasure) {}
    record DishRecipeDto(UUID dishId, String dishName, List<RecipeItemDto> items) {}

    Optional<DishRecipeDto> getRecipeForDish(UUID tenantId, UUID dishId);
}
```

### B. Evento Emitido por POS al Cobrar un Ticket:

```java
package com.restaurant.app.modules.pos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPaidEvent(
    UUID orderId,
    UUID tenantId,
    UUID branchId,
    String ticketNumber,
    List<OrderItemDto> items,
    BigDecimal totalAmount,
    String paymentMethod,
    Instant timestamp
) {
    public record OrderItemDto(UUID dishId, String dishName, int quantity, BigDecimal unitPrice) {}
}
```

### C. Consumidor en el Módulo Inventory:

```java
package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class InventoryEventListener {

    private final KardexService kardexService;

    InventoryEventListener(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        kardexService.processOrderDeduction(event);
    }
}
```

---

## 7. Patrón Strategy: Política de Stock Flexible

El servicio `KardexService` evalúa la clave `INVENTORY_DEDUCTION_MODE` configurada para el tenant:

1. **`MANUAL_ISOLATED`:** El listener omite el descuento automático de ingredientes. Ideal para pequeñas empresas que solo usan el POS como registradora y controlan inventario por conteos periódicos.
2. **`AUTOMATIC_PERMISSIVE`:** Descuenta ingredientes según receta. Si no hay stock suficiente, permite saldo negativo en `branch_stock` y genera un registro de alerta en `audit_logs`.
3. **`AUTOMATIC_STRICT`:**
   - **En operación online:** Si no hay stock suficiente, rechaza la confirmación del ticket antes del cobro.
   - **En sincronización offline:** Dado que el cliente físico ya pagó y se retiró, el ticket financiero SIEMPRE se registra, pero el stock registra un estado `STOCK_DEFICIT_WARNING` en auditoría para revisión administrativa.

---

## 8. Workflow de Aprobación de Gastos Menores de Caja (`modules.finance`)

- Los gastos pagados con efectivo de la gaveta (`paid_from_cash_drawer = true`) registrados por el cajero se crean en estado `PENDING_APPROVAL`, requiriendo `evidence_url` (foto del recibo).
- El encargado de sucursal (`BRANCH_MANAGER`) revisa la solicitud y aprueba o rechaza el gasto.
- Únicamente los gastos con `approval_status = 'APPROVED'` y pagados en efectivo se descuentan de la fórmula de `expected_cash` al momento del cierre de caja:
  $$\text{expected\_cash} = \text{initial\_cash} + \text{total\_cash\_sales} - \text{total\_cash\_expenses}_{\text{approved}}$$

---

## 9. Reglas de Validación Arquitectónica (JUnit)

La integridad de los módulos se valida automáticamente en la suite de pruebas unitarias:

```java
package com.restaurant.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verifyModularity() {
        ApplicationModules.of(RestaurantApplication.class).verify();
    }
}
```
