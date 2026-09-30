# 01. Vista General de Arquitectura

## 1. Modelo de Dominio y Módulos de Negocio

El sistema adopta el patrón de **Monolito Modular** estructurado mediante **Spring Modulith**. Cada módulo representa un límite de contexto claro (Bounded Context) con encapsulación estricta, comunicándose externamente únicamente a través de su paquete público `.api` o eventos asíncronos desacoplados.

### Módulos Principales:
- `core.security`: Gestión de identidades adaptativa (soporte de login por `username` obligatorio o `email` opcional), emisión y validación de tokens JWT, integración condicional OIDC/Keycloak y propagación de contexto (`TenantContextHolder`).
- `core.config`: Servicio de parametrización dinámica (`tenant_settings` y `branch_settings`) con fallback a valores por defecto tipados (políticas de inventario, flujos de aprobación y validaciones).
- `core.audit`: Trazabilidad inmutable de operaciones sensibles (usuario, sucursal, entidad, acción y diff JSONB de valores anteriores y nuevos).
- `core.common`: Clases base del dominio (`BaseTenantEntity`, `BaseBranchEntity`), manejo estandarizado de errores (`ProblemDetail` RFC 7807) y utilidades transversales.
- `modules.catalog`: Catálogo maestro de platos y categorías, recetas con gramajes (Bill of Materials - BOM), soporte para clonación de platos con receta diferenciada (`cloned_from_id`) y reglas de disponibilidad/precios por sucursal (`branch_dishes`).
- `modules.inventory`: Existencias por sucursal física, registro de compras a proveedores (`purchases` y `purchase_items`), deducciones automáticas por venta basadas en recetas, ajustes de mermas/conteos físicos y cálculo de costo promedio ponderado en Kardex.
- `modules.pos`: Ciclo de vida completo del ticket de comanda (`DRAFT` -> `WAITING` -> `IN_PREPARATION` -> `READY` -> `DELIVERED` -> `PAID` / `CANCELLED`), emisión de `OrderPaidEvent`, cobros multiformato e integración offline-first con clave de idempotencia (`client_transaction_id`).
- `modules.finance`: Control de turnos de caja (`cash_shifts`), conciliación de arqueo físico vs. esperado, workflow de aprobación de gastos operativos en efectivo con evidencia adjunta (`expenses`) y prorrateo de costos fijos mensuales.
- `modules.analytics`: Dashboard de rentabilidad real diaria, margen de contribución por plato y cálculo del punto de equilibrio operativo.

---

## 2. Aislamiento Multi-Tenant y Multi-Sucursal

El aislamiento lógico de datos se implementa en dos niveles para equilibrar integridad y rendimiento:

### A. Jerarquía de Entidades Base (`core.common`):
1. **`BaseTenantEntity` (`tenantId`, `createdAt`, `updatedAt`, `version`):**
   Para entidades de catálogo y configuración global de la empresa (`categories`, `ingredients`, platos maestros en `dishes`, `tenant_settings`).
2. **`BaseBranchEntity extends BaseTenantEntity` (`branchId`):**
   Para entidades que pertenecen a la operación diaria de una sucursal específica (`branch_stock`, `purchases`, `purchase_items`, `orders`, `order_items`, `cash_shifts`, `expenses`, `branch_dishes`, `branch_settings`).

### B. Propagación de Contexto:
- Cada petición autenticada es interceptada por un filtro de seguridad que extrae `tenant_id` y `branch_id` del token JWT verificado y lo deposita en un `ThreadLocal` contextual (`TenantContextHolder`).
- Los repositorios y servicios aplican automáticamente los filtros de tenant/branch para garantizar aislamiento estricto.
- En base de datos, las tablas transaccionales denormalizan `tenant_id` y `branch_id` para posibilitar índices compuestos de búsqueda inmediata y facilitar el particionado físico futuro.

---

## 3. Principios de Diseño
El diseño técnico se rige por:
- **SOLID:** Responsabilidad única por módulo, inversión de dependencias mediante interfaces públicas en `.api`.
- **KISS y YAGNI ("Lo necesario, suficiente, justo"):** Soluciones directas sin sobreingeniería innecesaria, configurables para atender tanto a PyMEs gastronómicas como a cadenas en expansión.