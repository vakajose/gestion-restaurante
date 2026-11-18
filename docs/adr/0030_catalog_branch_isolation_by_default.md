---
title: "ADR-0030: Aislamiento del Catálogo por Sucursal por Defecto y Clonación Inter-Sucursal"
category: "adr"
tags:
  - adr
  - catalog
  - multi-branch
  - architecture
last_updated: "2026-11-17"
status: "accepted"
---

# ADR-0030: Aislamiento del Catálogo por Sucursal por Defecto y Clonación Inter-Sucursal

## Contexto y Problema
El modelo inicial descrito en [ADR-0004](./0004_catalog_cloning_and_overrides.md) asumía un catálogo base predominantemente global a nivel de empresa (`tenant_id`) sobre el cual cada sucursal aplicaba overrides puntuales de precio y disponibilidad (`branch_dishes`). Sin embargo, en la operación real multi-sucursal (restaurantes con formatos diferenciados entre sedes), compartir por defecto todos los platos nuevos entre sucursales generó acoplamiento no deseado: crear un plato experimental o estacional en una sucursal lo exponía automáticamente en el catálogo de todas las demás sucursales del mismo tenant.

Era necesario resolver dos necesidades simultáneas:
1. Permitir que cada empresa elija entre operar con catálogo aislado por sucursal (`BRANCH_ISOLATED`) o compartido a nivel empresa (`GLOBAL_SHARED`).
2. Establecer como comportamiento seguro por defecto el aislamiento por sucursal sin romper la visibilidad de platos globales preexistentes ni la reutilización controlada entre sucursales mediante clonación explícita (`TASK-632`, `TASK-633`).

## Decisión
Se implementa la estrategia configurable de alcance de catálogo gobernada por el parámetro de sistema `catalog.scope_mode` (definido en `parameters-catalog.json` con alcance `TENANT`, criticidad `HIGH`):

1. **Valor por defecto `BRANCH_ISOLATED` (`ENUM(BRANCH_ISOLATED, GLOBAL_SHARED)`):**
   - Al crear un plato o combo sin `branchId` explícito cuando `catalog.scope_mode = BRANCH_ISOLATED`, `CatalogServiceImpl` (`createDishForScopeDefault`) inyecta automáticamente el `branchId` de la sucursal activa en `TenantContextHolder`.
   - Cuando `catalog.scope_mode = GLOBAL_SHARED`, los platos creados sin `branchId` permanecen con `branch_id = NULL` (alcance global del tenant), manteniendo compatibilidad con cadenas de menú único.
2. **Resolución de visibilidad por sucursal (`getCatalogDishesForBranch`):**
   - La consulta del catálogo efectivo de una sucursal combina los platos globales del tenant (`branch_id IS NULL`) más los platos propios de la sucursal activa (`branch_id = :branchId`), aplicando sobre ambos la capa de overrides locales de la tabla `branch_dishes` (`POST /api/v1/catalog/dishes/{dishId}/branch-override` con `BranchOverrideRequest(branchId, isAvailable, priceOverride)`).
   - Esto garantiza compatibilidad hacia atrás para catálogos semilla y platos globales creados antes del cambio de parámetro.
3. **Clonación explícita e independencia de recetas (`TASK-632`, `TASK-633`):**
   - Para replicar un plato hacia otra sucursal o duplicarlo dentro de la misma sucursal sin acoplamiento futuro, se expone `POST /api/v1/catalog/dishes/{dishId}/clone` con `CloneDishRequest(targetBranchId, newCode, newName, newSalePrice, customRecipe)`.
   - La clonación copia metadatos, estrategia de inventario y receta (o aplica `customRecipe` si se envía en la petición) generando un registro independiente en la sucursal destino.
4. **Combos acotados por sucursal:**
   - Los combos reciben soporte explícito de `branchId` en `CreateComboRequest(branchId, categoryId, code, name, description, basePrice, items)` para respetar la operación aislada por sucursal.

## Consecuencias

### Positivas
- **Cero contaminación accidental entre sucursales:** Las sucursales nuevas o piloto no alteran los menús POS de las demás sedes al registrar platos propios.
- **Flexibilidad para franquicias o cadenas:** Empresas con carta unificada pueden cambiar `catalog.scope_mode` a `GLOBAL_SHARED` en caliente desde la consola de parámetros ([ADR-0008](./0008_unified_multilevel_parameter_system.md), [ADR-0025](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md)) con confirmación de contraseña y auditoría.
- **Reutilización sin acoplamiento:** Clonar platos permite partir de una ficha base e independizar costos, código SKU o receta por sucursal.

### Negativas y Mitigaciones
- **Duplicación controlada de registros cuando `BRANCH_ISOLATED`:** Una empresa con varias sucursales que desee el mismo plato aislado deberá clonarlo si no usa `GLOBAL_SHARED`. *Mitigación:* El flujo de clonación (`POST /dishes/{dishId}/clone`) y el asistente de código secuencial ([ADR-0022](./0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md)) agilizan el alta de variantes.

## Referencias de Implementación
- Parámetro en catálogo: `backend/src/main/resources/config/parameters-catalog.json` (`catalog.scope_mode`, default `BRANCH_ISOLATED`).
- Backend: `CatalogController.java`, `CatalogServiceImpl.java`, `BranchOverrideRequest.java`, `CloneDishRequest.java`, `CreateComboRequest.java`.
- Frontend Angular: `frontend/src/app/features/catalog/`, `frontend/src/app/core/services/catalog.service.ts`.

## Documentos Relacionados
- [ADR-0002: Modelo Multi-Tenant Híbrido y Configuración Dinámica](./0002_hybrid_tenancy_and_dynamic_settings.md)
- [ADR-0004: Clonación y Overrides de Catálogo por Sucursal](./0004_catalog_cloning_and_overrides.md)
- [ADR-0008: Sistema Unificado de Parámetros Multinivel](./0008_unified_multilevel_parameter_system.md)
- [ADR-0022: Asistente Secuencial SKU, Selector Emoji y Ordenamiento](./0022_sequential_sku_assistant_category_emoji_picker_and_ordering.md)
- [ADR-0025: Humanización de Parámetros, Catálogo JSON y Ayuda Contextual](./0025_parameter_console_humanization_json_catalog_and_contextual_help.md)
- [ADR-0031: Sesiones de Servicio por Sucursal y Combos Dinámicos](./0031_branch_service_sessions_and_dynamic_combos.md)
- [Casos de Uso de Catálogo](../project/architecture/03-use-cases/use_cases_catalog.md)
- [Casos de Uso de Configuración](../project/architecture/03-use-cases/use_cases_configuration.md)
- [Secuencia de Resolución de Parámetros y Hot-Reload](../project/architecture/04-behavior-and-flows/sequence_parameter_resolution_and_hot_reload.md)
